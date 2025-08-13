package com.example.arrangement_manager.retrofit

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

/**
 * Retrofit API service interface for the Arrangement Manager application.
 *
 * This interface defines the API endpoints for interacting with the backend server,
 * handling user, table, menu, and order-related operations.
 */
interface ArrangementManagerApiService {

    // --- Users ---

    /**
     * Registers a new user.
     * @param user The [User] object containing the user's email and password.
     * @return A Retrofit [Response] with the registered [User] object.
     */
    @POST("users/register")
    suspend fun registerUser(@Body user: User): Response<User>

    /**
     * Logs in an existing user.
     * @param user The [User] object with login credentials.
     * @return A Retrofit [Response] with a [LoginResponse] object.
     */
    @POST("users/login")
    suspend fun loginUser(@Body user: User): Response<LoginResponse>

    // --- Tables ---

    /**
     * Retrieves all tables for a specific user.
     * @param userId The ID of the user.
     * @return A Retrofit [Response] with a list of [Table] objects.
     */
    @GET("users/{userId}/tables")
    suspend fun getAllTablesByUser(@Path("userId") userId: String): Response<List<Table>>

    /**
     * Inserts a new table for a specific user.
     * @param userId The ID of the user.
     * @param table The [Table] object to be inserted.
     * @return A Retrofit [Response] with the inserted [Table] object.
     */
    @POST("users/{userId}/tables")
    suspend fun insertTable(@Path("userId") userId: String, @Body table: Table): Response<Table>

    /**
     * Updates an existing table for a user.
     * @param userId The ID of the user.
     * @param tableName The name of the table to update.
     * @param tableUpdate The [TableUpdate] object with the new data.
     * @return A Retrofit [Response] with the updated [Table] object.
     */
    @PUT("users/{userId}/tables/{name}")
    suspend fun updateTable(
        @Path("userId") userId: String,
        @Path("name") tableName: String,
        @Body tableUpdate: TableUpdate
    ): Response<Table>

    /**
     * Deletes a table for a user.
     * @param userId The ID of the user.
     * @param tableName The name of the table to delete.
     * @return A Retrofit [Response] with a [Void] body on success.
     */
    @DELETE("users/{userId}/tables/{name}")
    suspend fun deleteTable(@Path("userId") userId: String, @Path("name") tableName: String): Response<Void>


    // --- Menu ---

    /**
     * Retrieves all menu items for a specific user.
     * @param userId The ID of the user.
     * @return A Retrofit [Response] with a list of [MenuItem] objects.
     */
    @GET("users/{userId}/menu")
    suspend fun getAllMenuByUser(@Path("userId") userId: String): Response<List<MenuItem>>

    /**
     * Inserts a new menu item for a specific user.
     * @param userId The ID of the user.
     * @param menuItem The [MenuItem] object to be inserted.
     * @return A Retrofit [Response] with the inserted [MenuItem] object.
     */
    @POST("users/{userId}/menu")
    suspend fun insertMenuItem(@Path("userId") userId: String, @Body menuItem: MenuItem): Response<MenuItem>

    /**
     * Updates an existing menu item for a user.
     * @param userId The ID of the user.
     * @param menuName The name of the menu item to update.
     * @param menuItemUpdate The [MenuItemUpdate] object with the new data.
     * @return A Retrofit [Response] with the updated [MenuItem] object.
     */
    @PUT("users/{userId}/menu/{name}")
    suspend fun updateMenuItem(
        @Path("userId") userId: String,
        @Path("name") menuName: String,
        @Body menuItemUpdate: MenuItemUpdate
    ): Response<MenuItem>

    /**
     * Deletes a menu item for a user.
     * @param userId The ID of the user.
     * @param menuName The name of the menu item to delete.
     * @return A Retrofit [Response] with a [Void] body on success.
     */
    @DELETE("users/{userId}/menu/{name}")
    suspend fun deleteMenuItem(@Path("userId") userId: String, @Path("name") menuName: String): Response<Void>


    // --- Orders ---

    /**
     * Inserts a list of new order entries for a user.
     * @param userId The ID of the user.
     * @param orders A list of [NewOrderEntry] objects to be inserted.
     * @return A Retrofit [Response] with a list of the inserted [OrderEntry] objects.
     */
    @POST("users/{userId}/orders")
    suspend fun insertOrderEntries(
        @Path("userId") userId: String,
        @Body orders: List<NewOrderEntry>
    ): Response<List<OrderEntry>>
}