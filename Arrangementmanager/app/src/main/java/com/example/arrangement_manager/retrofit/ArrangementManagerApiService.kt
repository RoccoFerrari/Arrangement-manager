package com.example.arrangement_manager.retrofit

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface ArrangementManagerApiService {

    // --- Users ---

    @POST("users/register")
    suspend fun registerUser(@Body user: User): Response<User>

    @POST("users/login")
    suspend fun loginUser(@Body user: User): Response<LoginResponse>

    // --- Tables ---

    @GET("users/{userId}/tables")
    suspend fun getAllTablesByUser(@Path("userId") userId: String): Response<List<Table>>

    @POST("users/{userId}/tables")
    suspend fun insertTable(@Path("userId") userId: String, @Body table: Table): Response<Table>

    @PUT("users/{userId}/tables/{name}")
    suspend fun updateTable(
        @Path("userId") userId: String,
        @Path("name") tableName: String,
        @Body tableUpdate: TableUpdate
    ): Response<Table>

    @DELETE("users/{userId}/tables/{name}")
    suspend fun deleteTable(@Path("userId") userId: String, @Path("name") tableName: String): Response<Void>


    // --- Menu ---

    @GET("users/{userId}/menu")
    suspend fun getAllMenuByUser(@Path("userId") userId: String): Response<List<MenuItem>>

    @POST("users/{userId}/menu")
    suspend fun insertMenuItem(@Path("userId") userId: String, @Body menuItem: MenuItem): Response<MenuItem>

    @PUT("users/{userId}/menu/{name}")
    suspend fun updateMenuItem(
        @Path("userId") userId: String,
        @Path("name") menuName: String,
        @Body menuItemUpdate: MenuItemUpdate
    ): Response<MenuItem>

    @DELETE("users/{userId}/menu/{name}")
    suspend fun deleteMenuItem(@Path("userId") userId: String, @Path("name") menuName: String): Response<Void>


    // --- Orders ---

    @POST("users/{userId}/orders")
    suspend fun insertOrderEntries(
        @Path("userId") userId: String,
        @Body orders: List<NewOrderEntry>
    ): Response<List<OrderEntry>>
}