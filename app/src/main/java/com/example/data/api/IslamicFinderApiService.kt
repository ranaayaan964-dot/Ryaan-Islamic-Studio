package com.example.data.api

import com.example.data.model.IslamicFinderResponse
import com.example.data.model.MuslimSalatResponse
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Url
import java.util.concurrent.TimeUnit

interface IslamicFinderApiService {

    @GET("index.php/api/prayer_times")
    suspend fun getIslamicFinderPrayerTimes(
        @Query("country") country: String = "PK",
        @Query("city") city: String = "Sahiwal"
    ): IslamicFinderResponse

    @GET
    suspend fun getMuslimSalatTimes(
        @Url url: String = "https://muslimsalat.com/sahiwal.json"
    ): MuslimSalatResponse

    companion object {
        const val ISLAMIC_FINDER_URL = "https://www.islamicfinder.us/index.php/api/prayer_times?country=PK&city=Sahiwal"
        const val MUSLIM_SALAT_URL = "https://muslimsalat.com/sahiwal.json"
        private const val BASE_URL = "https://www.islamicfinder.us/"

        fun create(): IslamicFinderApiService {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }

            val client = OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .followRedirects(true)
                .followSslRedirects(true)
                .addInterceptor(logging)
                .build()

            val moshi = Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
                .create(IslamicFinderApiService::class.java)
        }
    }
}
