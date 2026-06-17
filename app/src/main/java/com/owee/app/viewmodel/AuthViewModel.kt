package com.owee.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.owee.app.data.remote.SupabaseProvider
import com.owee.app.data.remote.model.AuthUser
import com.owee.app.data.repository.UserRepository
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.jsonPrimitive

//data class AuthUser(
//    val id: String = "",
//    val token: String = "",
//    val email: String = "",
//    val photoUrl: String = "",
//    val name: String = "",
//    val username: String = "",
//    val joinedDate: String = ""
//)

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
        if (_user.value.authId == userId && !_user.value.name.isNullOrEmpty()) {
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
                dbId = profile.id ?: "",
                authId = userId,
                email = currentUser?.email ?: "",
                photoUrl = profile.photo_url ?: metadataPhoto ?: "",
                name = profile.name,
                username = profile.username,
                joinedDate = profile.created_at ?: ""
            )
            _authState.value = AuthState.Authenticated
        } else {
            _user.value = _user.value.copy(
                authId = userId,
                email = currentUser?.email ?: "",
                photoUrl = metadataPhoto ?: _user.value.photoUrl // Keep existing or use metadata
            )
            _authState.value = AuthState.NeedsProfile
        }
    }

    fun updateUser(
        dbId: String? = null,
        authId: String,
        token: String? = null,
        email: String? = null,
        photoUrl: String? = null,
        name: String? = null,
        username: String? = null
    ) {
        _user.value = _user.value.copy(
            dbId = dbId ?: _user.value.dbId,
            authId = authId,
            token = token ?: _user.value.token,
            email = email ?: _user.value.email,
            photoUrl = photoUrl ?: _user.value.photoUrl,
            name = name ?: _user.value.name,
            username = username ?: _user.value.username
        )

        if (!_user.value.name.isNullOrEmpty() && !_user.value.username.isNullOrEmpty()) {
            _authState.value = AuthState.Authenticated
        } else {
            viewModelScope.launch {
                loadUserProfile(authId)
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
