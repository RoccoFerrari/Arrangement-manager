package com.example.arrangement_manager.retrofit

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

object RetrofitClient {

    // IP del server Flask
    // Non si usa 127.0.0.1 perché questo indirizzo si riferisce alll'emulatore stesso
    // si deve usare http://10.0.2.2:5000/
    // private const val BASE_URL = "http://192.168.1.34:5000/" // DT
    // private const val BASE_URL = "http://10.0.2.2:5000/" // LH
     private const val BASE_URL = "http://192.168.1.17:5000/" // LT

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    // Interceptor per il logging (DEBUG)
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    // OkHttpClient per aggiungere l'interceptor
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    val apiService: ArrangementManagerApiService by lazy {
        retrofit.create(ArrangementManagerApiService::class.java)
    }
}