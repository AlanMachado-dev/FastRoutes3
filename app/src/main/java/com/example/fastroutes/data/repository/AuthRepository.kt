package com.example.fastroutes.data.repository

import com.example.fastroutes.network.SupabaseClientProvider
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AppUser(
    val id: String = "",

    @SerialName("user_id")
    val userId: String = "",

    val email: String = "",

    val active: Boolean = false,

    val role: String = "client",

    @SerialName("business_name")
    val businessName: String? = null,

    @SerialName("expires_at")
    val expiresAt: String? = null
)

class AuthRepository {

    private val supabase = SupabaseClientProvider.client

    suspend fun login(
        email: String,
        password: String
    ) {
        val cleanEmail = email.trim()

        if (cleanEmail.isBlank() || password.isBlank()) {
            throw Exception("Ingresá email y contraseña.")
        }

        supabase.auth.signInWith(Email) {
            this.email = cleanEmail
            this.password = password
        }

        val result = checkCurrentUser()

        result.onFailure { error ->
            supabase.auth.signOut()
            throw Exception(error.message ?: "Usuario sin acceso.")
        }
    }

    suspend fun checkCurrentUser(): Result<AppUser> {
        return try {
            val authUser = supabase.auth.currentUserOrNull()
                ?: return Result.failure(Exception("No logueado."))

            val appUsers = supabase
                .from("app_users")
                .select {
                    filter {
                        eq("user_id", authUser.id)
                    }
                }
                .decodeList<AppUser>()

            val appUser = appUsers.firstOrNull()
                ?: return Result.failure(Exception("Usuario sin permisos en FastRoutes."))

            if (!appUser.active) {
                return Result.failure(
                    Exception("Tu cuenta está desactivada. Contactá al administrador.")
                )
            }

            Result.success(appUser)

        } catch (e: Exception) {
            Result.failure(
                Exception(e.message ?: "No se pudo verificar el usuario.")
            )
        }
    }

    suspend fun isAdminLoggedIn(): Boolean {
        return try {
            val user = checkCurrentUser().getOrNull()
            user?.role == "admin"
        } catch (e: Exception) {
            false
        }
    }

    suspend fun logout() {
        supabase.auth.signOut()
    }
}