package com.example.fastroutes.network

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest

object SupabaseClientProvider {

    val client = createSupabaseClient(
        supabaseUrl = "https://uhfdwakbhifzbqbkrsij.supabase.co",
        supabaseKey = " sb_publishable_tEzuQCJ9xx10BIYnhNu4Yg_auqlnfji"
    ) {
        install(Postgrest)
        install(Auth)
    }
}