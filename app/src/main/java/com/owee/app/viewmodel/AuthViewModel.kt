package com.owee.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.owee.app.data.remote.SupabaseProvider
import com.owee.app.data.repository.UserRepository
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.jsonPrimitive

data class AuthUser(
    val id: String = "",
    val token: String = "",
    val email: String = "",
    val photoUrl: String = "",
    val name: String = "",
    val username: String = "",
    val joinedDate: String = ""
)

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object Authenticated : AuthState()
    object NeedsProfile : AuthState()
    object Unauthenticated : AuthState()
}

class AuthViewModel(
    private val userRepository: UserRepository = UserRepository()
) : ViewModel() {

    private val _user = MutableStateFlow(AuthUser())
    val user = _user.asStateFlow()

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState = _authState.asStateFlow()

    init {
        observeSession()
    }

    private fun observeSession() {
        viewModelScope.launch {
            SupabaseProvider.client.auth.sessionStatus.collect { status ->
                when (status) {
                    is SessionStatus.Authenticated -> {
                        loadUserProfile(status.session.user?.id ?: "")
                    }
                    is SessionStatus.NotAuthenticated -> {
                        _authState.value = AuthState.Unauthenticated
                    }
                    is SessionStatus.Initializing -> {
                        _authState.value = AuthState.Loading
                    }
                    else -> Unit
                }
            }
        }
    }

    private suspend fun loadUserProfile(userId: String) {
        // Only fetch if we don't already have the profile or it's a different user
        if (_user.value.id == userId && _user.value.name.isNotEmpty()) {
            _authState.value = AuthState.Authenticated
            return
        }

        val profile = userRepository.getUserProfile(userId)
        val currentUser = SupabaseProvider.client.auth.currentUserOrNull()
        
        // Extract photo from Google metadata if available
        val metadataPhoto = currentUser?.userMetadata?.get("avatar_url")?.jsonPrimitive?.content
            ?: currentUser?.userMetadata?.get("picture")?.jsonPrimitive?.content

        if (profile != null) {
            _user.value = AuthUser(
                id = userId,
                email = currentUser?.email ?: "",
                photoUrl = profile.photo_url ?: metadataPhoto ?: "",
                name = profile.name,
                username = profile.username,
                joinedDate = profile.created_at ?: ""
            )
            _authState.value = AuthState.Authenticated
        } else {
            _user.value = _user.value.copy(
                id = userId,
                email = currentUser?.email ?: "",
                photoUrl = metadataPhoto ?: _user.value.photoUrl // Keep existing or use metadata
            )
            _authState.value = AuthState.NeedsProfile
        }
    }

    fun updateUser(
        id: String,
        token: String,
        email: String,
        photoUrl: String,
        name: String = "",
        username: String = ""
    ) {
        _user.value = _user.value.copy(
            id = id,
            token = token,
            email = email,
            photoUrl = if (photoUrl.isNotEmpty()) photoUrl else _user.value.photoUrl,
            name = if (name.isNotEmpty()) name else _user.value.name,
            username = if (username.isNotEmpty()) username else _user.value.username
        )

        if (_user.value.name.isNotEmpty() && _user.value.username.isNotEmpty()) {
            _authState.value = AuthState.Authenticated
        } else {
            viewModelScope.launch {
                loadUserProfile(id)
            }
        }
    }

    fun logout(onLogout: () -> Unit) {
        viewModelScope.launch {
            SupabaseProvider.client.auth.signOut()
            _user.value = AuthUser()
            _authState.value = AuthState.Unauthenticated
            onLogout()
        }
    }
}
