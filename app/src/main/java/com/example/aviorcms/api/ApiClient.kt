package com.example.aviorcms.api

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Хранилище токена — синхронное чтение (важно: раньше был баг в другом
 * варианте приложения, когда токен читался асинхронно и первые запросы
 * уходили без него — здесь EncryptedSharedPreferences читается сразу,
 * без корутин/колбэков, поэтому гонки быть не может).
 */
object TokenStore {
    private const val PREFS_NAME = "avior_secure_prefs"
    private const val KEY_TOKEN = "api_token"
    private const val KEY_ROLE = "user_role"
    private const val KEY_FULL_NAME = "user_full_name"

    private fun prefs(context: Context) = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveSession(context: Context, token: String, role: String, fullName: String) {
        prefs(context).edit()
            .putString(KEY_TOKEN, token)
            .putString(KEY_ROLE, role)
            .putString(KEY_FULL_NAME, fullName)
            .apply()
    }

    fun getToken(context: Context): String? = prefs(context).getString(KEY_TOKEN, null)
    fun getRole(context: Context): String? = prefs(context).getString(KEY_ROLE, null)
    fun getFullName(context: Context): String? = prefs(context).getString(KEY_FULL_NAME, null)

    fun clear(context: Context) {
        prefs(context).edit().clear().apply()
    }
}

/** Подставляет Authorization: Bearer <token> в КАЖДЫЙ запрос — единственная точка, где это делается. */
class AuthInterceptor(private val context: Context) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = TokenStore.getToken(context)
        val request = chain.request().newBuilder().apply {
            if (!token.isNullOrBlank()) {
                addHeader("Authorization", "Bearer $token")
            }
        }.build()
        return chain.proceed(request)
    }
}

object ApiClient {
    private const val BASE_URL = "https://cms.avior.moscow/api/mobile/"

    /**
     * Единственный экземпляр на всё приложение — специально object, а не
     * фабрика: если где-то в коде случайно создать ещё один Retrofit без
     * AuthInterceptor, запросы оттуда будут уходить без токена и сервер
     * будет отвечать 401, выглядящим как «просит войти заново». Не плодить
     * второй источник — только через ApiClient.service.
     */
    private var retrofit: Retrofit? = null

    fun service(context: Context): ApiService {
        if (retrofit == null) {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
            val client = OkHttpClient.Builder()
                .addInterceptor(AuthInterceptor(context.applicationContext))
                .addInterceptor(logging)
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .build()

            retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }
        return retrofit!!.create(ApiService::class.java)
    }
}
