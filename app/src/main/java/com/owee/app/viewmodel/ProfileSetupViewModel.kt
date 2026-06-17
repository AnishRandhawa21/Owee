package com.owee.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.owee.app.data.remote.SupabaseProvider
import com.owee.app.data.remote.model.User
import com.owee.app.data.repository.UserRepository
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class ProfileSetupState {
    object Idle : ProfileSetupState()
    object Loading : ProfileSetupState()
    object Success : ProfileSetupState()
    object UsernameAlreadyExists : ProfileSetupState()
    data class Error(val message: String) : ProfileSetupState()
}

class ProfileSetupViewModel(
    private val userRepository: UserRepository = UserRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProfileSetupState>(ProfileSetupState.Idle)
    val uiState = _uiState.asStateFlow()

    fun completeProfile(name: String, username: String, photoUrl: String?) {
        if (name.isBlank() || username.isBlank()) {
            _uiState.value = ProfileSetupState.Error("Name and Username cannot be empty")
            return
        }

        viewModelScope.launch {
            _uiState.value = ProfileSetupState.Loading

            try {
                // 1. Check if username is taken
                val isTaken = userRepository.isUsernameTaken(username)
                if (isTaken) {
                    _uiState.value = ProfileSetupState.UsernameAlreadyExists
                    return@launch
                }

                // 2. Get current Auth ID from Supabase
                val authId = SupabaseProvider.client.auth.currentUserOrNull()?.id
                if (authId == null) {
                    _uiState.value = ProfileSetupState.Error("User not authenticated")
                    return@launch
                }

                // 3. Insert into public.users table
                val newUser = User(
                    auth_id = authId,
                    name = name,
                    username = username,
                    photo_url = if (photoUrl.isNullOrEmpty()) null else photoUrl
                )

                userRepository.insertUser(newUser)
                
                // Cache user data in the shared AuthViewModel locally after successful insert
                // This prevents re-fetching from DB
                _uiState.value = ProfileSetupState.Success

            } catch (e: Exception) {
                _uiState.value = ProfileSetupState.Error(e.message ?: "An unexpected error occurred")
            }
        }
    }

    fun resetState() {
        _uiState.value = ProfileSetupState.Idle
    }
}
