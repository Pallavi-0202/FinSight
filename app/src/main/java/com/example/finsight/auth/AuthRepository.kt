package com.example.finsight.auth

import com.example.finsight.data.User
import com.example.finsight.data.UserDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository for handling user authentication (registration and login) securely.
 */
class AuthRepository(private val userDao: UserDao) {

    suspend fun register(
        name: String,
        email: String,
        password: String
    ): Result<User> = withContext(Dispatchers.IO) {
        try {
            val normalizedEmail = email.trim().lowercase()

            val existingUser = userDao.getByEmail(normalizedEmail)
            if (existingUser != null) {
                return@withContext Result.failure(Exception("This email is already registered"))
            }

            val salt = PasswordHasher.generateSalt()
            val hash = PasswordHasher.hashPassword(password, salt)

            val newUser = User(
                name = name.trim(),
                email = normalizedEmail,
                passwordHash = hash,
                salt = salt
            )

            val id = userDao.insert(newUser)
            Result.success(newUser.copy(id = id))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun login(
        email: String,
        password: String
    ): Result<User> = withContext(Dispatchers.IO) {
        try {
            val normalizedEmail = email.trim().lowercase()
            val user = userDao.getByEmail(normalizedEmail)

            if (user == null || !PasswordHasher.verifyPassword(password, user.salt, user.passwordHash)) {
                return@withContext Result.failure(Exception("Invalid email or password"))
            }

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(Exception("Invalid email or password"))
        }
    }
}
