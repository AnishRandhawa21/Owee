package com.owee.app.data.remote

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.owee.app.BuildConfig
import com.owee.app.data.remote.model.GoogleUser
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.IDToken
import kotlinx.serialization.json.jsonPrimitive

class AuthManager(
    private val context: Context
) {

    suspend fun signIn(): GoogleUser? {

        val googleIdOption = GetGoogleIdOption.Builder()
            .setServerClientId(BuildConfig.GOOGLE_WEB_CLIENT_ID)
            .setFilterByAuthorizedAccounts(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        val credentialManager = CredentialManager.create(context)

        val result = try {
            credentialManager.getCredential(
                context = context,
                request = request
            )
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }

        val credential = result.credential

        val googleCredential = GoogleIdTokenCredential
            .createFrom(credential.data)

        // 3. Sign in to Supabase using the Google ID Token
        SupabaseProvider.client.auth.signInWith(IDToken) {
            idToken = googleCredential.idToken
            provider = Google
        }

        // 5. Get the authenticated Supabase user and metadata
        val supabaseUser = SupabaseProvider.client.auth.currentUserOrNull()
            ?: throw IllegalStateException("Supabase sign in failed")

        // Pull the photo URL from Supabase metadata if credential manager missed it
        val metadataPhoto = supabaseUser.userMetadata?.get("avatar_url")?.jsonPrimitive?.content
            ?: supabaseUser.userMetadata?.get("picture")?.jsonPrimitive?.content

        return GoogleUser(
            id = supabaseUser.id,
            token = googleCredential.idToken,
            email = supabaseUser.email ?: "",
            photoUrl = metadataPhoto ?: googleCredential.profilePictureUri?.toString()
        )
    }

    fun getSupabaseUserId(): String? {
        return SupabaseProvider.client.auth.currentUserOrNull()?.id
    }

    suspend fun signOut() {
        SupabaseProvider.client.auth.signOut()
    }
}
