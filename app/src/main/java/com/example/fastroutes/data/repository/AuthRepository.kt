package com.example.fastroutes.data.repository

import com.example.fastroutes.data.model.AdminUser
import com.example.fastroutes.network.SupabaseClientProvider
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.from

class AuthRepository {

    private val supabase = SupabaseClientProvider.client

    suspend fun login(
        email: String,
        password: String
    ) {
        supabase.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
    }

    suspend fun logout() {
        supabase.auth.signOut()
    }

    suspend fun isAdminLoggedIn(): Boolean {
        val user = supabase.auth.currentUserOrNull() ?: return false

        val adminRows = supabase
            .from("admin_users")
            .select {
                filter {
                    eq("user_id", user.id)
                }
            }
            .decodeList<AdminUser>()

        return adminRows.isNotEmpty()
    }
}