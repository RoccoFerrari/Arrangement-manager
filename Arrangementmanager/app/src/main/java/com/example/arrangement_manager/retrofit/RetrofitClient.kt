package com.example.arrangement_manager.retrofit

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

/**
 * Singleton object for configuring and providing a Retrofit client.
 *
 * This object sets up the base URL for the API, a logging interceptor for debugging,
 * and Moshi for JSON serialization and deserialization.
 */
object RetrofitClient {

    // Flask server IP
    // Don't use 127.0.0.1 because this address refers to the emulator itself.
    // Use http://10.0.2.2:5000/ to reference the localhost.
    // private const val BASE_URL = "http://192.168.1.34:5000/" // DT
    // private const val BASE_URL = "http://10.0.2.2:5000/" // LH
     private const val BASE_URL = "http://192.168.1.17:5000/" // LT
    // private const val BASE_URL = "http://192.168.1.108:5000/"

    /**
     * Moshi instance for JSON parsing.
     * KotlinJsonAdapterFactory is added to support Kotlin data classes.
     */
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    /**
     * HttpLoggingInterceptor for logging request and response information.
     * The level is set to BODY to log headers and body of the requests and responses.
     */
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    /**
     * OkHttpClient with the logging interceptor added.
     */
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .build()

    /**
     * Retrofit instance configured with the base URL, HTTP client, and Moshi converter.
     */
    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    /**
     * Lazy-initialized API service.
     * This provides an instance of the ArrangementManagerApiService to make API calls.
     */
    val apiService: ArrangementManagerApiService by lazy {
        retrofit.create(ArrangementManagerApiService::class.java)
    }
}