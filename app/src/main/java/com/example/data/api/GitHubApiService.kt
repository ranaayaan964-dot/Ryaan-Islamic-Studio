package com.example.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Headers
import java.util.concurrent.TimeUnit

/**
 * Data class representing the latest release JSON payload from GitHub Releases API.
 * Specifically targets tag_name (e.g., "v1.0.5") and html_url (the direct link to the GitHub release page).
 */
@JsonClass(generateAdapter = true)
data class GitHubReleaseResponse(
    @Json(name = "tag_name") val tag_name: String = "",
    @Json(name = "html_url") val html_url: String = ""
) {
    val tagName: String get() = tag_name
    val htmlUrl: String get() = html_url
}

/**
 * Dedicated Retrofit interface & direct OkHttp client for the GitHub Releases API.
 * Strictly queries: https://api.github.com/repos/ranaayaan964-dot/Ryaan-Islamic-Studio/releases/latest
 * with header: Accept: application/vnd.github.v3+json
 */
interface GitHubApiService {

    @Headers(
        "Accept: application/vnd.github.v3+json",
        "User-Agent: Ryaan-Islamic-Studio-Android"
    )
    @GET("repos/ranaayaan964-dot/Ryaan-Islamic-Studio/releases/latest")
    suspend fun getLatestRelease(): Response<GitHubReleaseResponse>

    companion object {
        const val GITHUB_API_BASE_URL = "https://api.github.com/"
        const val GITHUB_RELEASES_LATEST_URL =
            "https://api.github.com/repos/ranaayaan964-dot/Ryaan-Islamic-Studio/releases/latest"

        val okHttpClient: OkHttpClient by lazy {
            OkHttpClient.Builder()
                .connectTimeout(12, TimeUnit.SECONDS)
                .readTimeout(12, TimeUnit.SECONDS)
                .addInterceptor { chain ->
                    val request = chain.request().newBuilder()
                        .header("Accept", "application/vnd.github.v3+json")
                        .header("User-Agent", "Ryaan-Islamic-Studio-Android")
                        .build()
                    chain.proceed(request)
                }
                .build()
        }

        fun create(): GitHubApiService {
            val moshi = Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()

            return Retrofit.Builder()
                .baseUrl(GITHUB_API_BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
                .create(GitHubApiService::class.java)
        }

        /**
         * Direct OkHttp GET request fallback to query GitHub release endpoint directly:
         * https://api.github.com/repos/ranaayaan964-dot/Ryaan-Islamic-Studio/releases/latest
         * Returns GitHubReleaseResponse or null on 404 / network error.
         */
        fun fetchLatestReleaseDirect(): GitHubReleaseResponse? {
            return try {
                val request = Request.Builder()
                    .url(GITHUB_RELEASES_LATEST_URL)
                    .addHeader("Accept", "application/vnd.github.v3+json")
                    .addHeader("User-Agent", "Ryaan-Islamic-Studio-Android")
                    .get()
                    .build()

                okHttpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string()
                        if (!body.isNullOrBlank()) {
                            val json = JSONObject(body)
                            val tagName = json.optString("tag_name", "")
                            val htmlUrl = json.optString("html_url", "")
                            GitHubReleaseResponse(tag_name = tagName, html_url = htmlUrl)
                        } else null
                    } else null
                }
            } catch (e: Exception) {
                null
            }
        }
    }
}
