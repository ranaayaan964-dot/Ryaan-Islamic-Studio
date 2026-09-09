package com.example.data.api

import com.example.data.model.AladhanApiResponse
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface AladhanApiService {

    /**
     * Exact GPS Coordinates endpoint for 100% accurate Sahiwal, Pakistan prayer timings.
     * GET https://aladhan.api.islamic.network/v1/timings/{date}
     * Parameters: latitude=30.6682, longitude=73.1114, method=1 (Karachi Univ), school=1 (Hanafi), timezonestring=Asia/Karachi
     */
    @GET("v1/timings/{date}")
    suspend fun getTimingsByCoordinates(
        @Path("date") date: String,
        @Query("latitude") latitude: Double = 30.6682,
        @Query("longitude") longitude: Double = 73.1114,
        @Query("method") method: Int = 1,
        @Query("school") school: Int = 1,
        @Query("timezonestring") timezoneString: String = "Asia/Karachi"
    ): AladhanApiResponse

    /**
     * Official fallback server endpoint for city-based prayer timings.
     * GET https://aladhan.api.islamic.network/v1/timingsByCity/{date}
     * Parameters: city=Sahiwal, country=Pakistan, method=1, school=1
     */
    @GET("v1/timingsByCity/{date}")
    suspend fun getTimingsByCity(
        @Path("date") date: String,
        @Query("city") city: String = "Sahiwal",
        @Query("country") country: String = "Pakistan",
        @Query("method") method: Int = 1,
        @Query("school") school: Int = 1
    ): AladhanApiResponse

    companion object {
        // Official primary fallback server from Aladhan OpenAPI spec
        const val PRIMARY_ISLAMIC_NETWORK_URL = "https://aladhan.api.islamic.network/"
        const val SECONDARY_ALADHAN_URL = "https://api.aladhan.com/"
        const val TERTIARY_ALISLAM_URL = "https://aladhan.api.alislam.ru/"

        fun create(baseUrl: String = PRIMARY_ISLAMIC_NETWORK_URL): AladhanApiService {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }

            val client = OkHttpClient.Builder()
                .connectTimeout(12, TimeUnit.SECONDS)
                .readTimeout(12, TimeUnit.SECONDS)
                .addInterceptor(logging)
                .build()

            val moshi = Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()

            return Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(client)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
                .create(AladhanApiService::class.java)
        }
    }
}
