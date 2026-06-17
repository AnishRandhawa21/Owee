package com.owee.app.data.remote

import com.owee.app.BuildConfig
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest

object SupabaseProvider {

    private val sanitizedUrl = BuildConfig.SUPABASE_URL.substringBefore("/rest/v1")

    val client = createSupabaseClient(
        supabaseUrl = sanitizedUrl,
        supabaseKey = BuildConfig.SUPABASE_KEY
    ) {
        install(Auth)
        install(Postgrest)
    }
}