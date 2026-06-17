package com.owee.app.data.remote

import com.owee.app.BuildConfig
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.ktor.client.engine.okhttp.OkHttp

object SupabaseProvider {

    private val sanitizedUrl =
        BuildConfig.SUPABASE_URL.substringBefore("/rest/v1")

    val client = createSupabaseClient(
        supabaseUrl = sanitizedUrl,
        supabaseKey = BuildConfig.SUPABASE_KEY
    ) {
        httpEngine = OkHttp.create()
        install(Auth)
        install(Postgrest)
        install(Realtime)
    }
}