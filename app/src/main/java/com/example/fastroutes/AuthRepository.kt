package com.example.fastroutes

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class AuthRepository {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    suspend fun login(email: String, password: String): Result<AppUser> {
        return try {
            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            val uid = authResult.user?.uid
                ?: return Result.failure(Exception("Usuario inválido"))

            val doc = db.collection("users").document(uid).get().await()

            if (!doc.exists()) {
                auth.signOut()
                return Result.failure(Exception("Usuario sin permisos en FastRoutes"))
            }

            val active = doc.getBoolean("active") ?: false

            if (!active) {
                auth.signOut()
                return Result.failure(
                    Exception("Tu cuenta está desactivada. Contactá al administrador.")
                )
            }

            val user = AppUser(
                uid = uid,
                email = doc.getString("email") ?: email,
                active = active,
                role = doc.getString("role") ?: "client",
                businessName = doc.getString("businessName") ?: ""
            )

            Result.success(user)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun checkCurrentUser(): Result<AppUser> {
        return try {
            val currentUser = auth.currentUser
                ?: return Result.failure(Exception("No logueado"))

            val doc = db.collection("users").document(currentUser.uid).get().await()

            if (!doc.exists()) {
                auth.signOut()
                return Result.failure(Exception("Usuario sin permisos"))
            }

            val active = doc.getBoolean("active") ?: false

            if (!active) {
                auth.signOut()
                return Result.failure(Exception("Cuenta desactivada"))
            }

            Result.success(
                AppUser(
                    uid = currentUser.uid,
                    email = doc.getString("email") ?: currentUser.email.orEmpty(),
                    active = active,
                    role = doc.getString("role") ?: "client",
                    businessName = doc.getString("businessName") ?: ""
                )
            )

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun logout() {
        auth.signOut()
    }
}