package com.example.arrangement_manager.retrofit

import android.os.Parcelable
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.Parcelize

/**
 * Data class representing the response from a login request.
 *
 * @property message A message from the server, typically indicating success or failure.
 * @property user The [User] object associated with the successful login.
 */
@JsonClass(generateAdapter = true)
data class LoginResponse(
    val message: String,
    val user: User
)

/**
 * Data class representing a user.
 *
 * @property email The unique email address of the user.
 * @property password The user's password (used for login requests).
 */
@JsonClass(generateAdapter = true)
data class User(
    val email: String,
    val password: String
)

/**
 * Data class representing a table in the arrangement.
 *
 * This class is also Parcelable to be passed between fragments using Navigation Components.
 *
 * @property name The unique name of the table.
 * @property idUser The ID of the user who owns the table.
 * @property xCoordinate The X coordinate of the table on the canvas.
 * @property yCoordinate The Y coordinate of the table on the canvas.
 * @property width The width of the table.
 * @property height The height of the table.
 */
@Parcelize
@JsonClass(generateAdapter = true)
data class Table(
    val name: String,
    @Json(name = "id_user") val idUser: String,
    @Json(name = "x_coordinate") val xCoordinate: Float,
    @Json(name = "y_coordinate") val yCoordinate: Float,
    val width: Float,
    val height: Float
) : Parcelable

/**
 * Data class used for updating a table's properties.
 *
 * The properties are nullable to allow for partial updates (e.g., only updating the coordinates).
 *
 * @property xCoordinate The new X coordinate of the table.
 * @property yCoordinate The new Y coordinate of the table.
 * @property width The new width of the table.
 * @property height The new height of the table.
 */
@JsonClass(generateAdapter = true)
data class TableUpdate(
    @Json(name = "x_coordinate") val xCoordinate: Float? = null,
    @Json(name = "y_coordinate") val yCoordinate: Float? = null,
    val width: Float? = null,
    val height: Float? = null
)

/**
 * Data class representing a menu item.
 *
 * @property name The unique name of the menu item.
 * @property idUser The ID of the user who owns the menu item.
 * @property price The price of the menu item.
 * @property quantity The quantity of the menu item in stock.
 * @property description A brief description of the menu item.
 */
@JsonClass(generateAdapter = true)
data class MenuItem(
    val name: String,
    @Json(name = "id_user") val idUser: String,
    val price: Float,
    val quantity: Int,
    val description: String
)

/**
 * Data class used for updating a menu item's properties.
 *
 * The properties are nullable to allow for partial updates.
 *
 * @property price The new price of the menu item.
 * @property quantity The new quantity of the menu item.
 * @property description The new description of the menu item.
 */
@JsonClass(generateAdapter = true)
data class MenuItemUpdate(
    val price: Float? = null,
    val quantity: Int? = null,
    val description: String? = null
)

/**
 * Data class representing an entry in a table's order.
 *
 * @property tableName The name of the table placing the order.
 * @property menuItemName The name of the menu item ordered.
 * @property idUser The ID of the user who owns the order.
 * @property quantity The quantity of the menu item ordered.
 */
@JsonClass(generateAdapter = true)
data class OrderEntry(
    @Json(name = "table_name") val tableName: String,
    @Json(name = "menu_item_name") val menuItemName: String,
    @Json(name = "id_user") val idUser: String,
    val quantity: Int
)

/**
 * Data class for creating a new order entry.
 *
 * This version omits the user ID as it is typically handled on the server side based on
 * the authenticated user.
 *
 * @property tableName The name of the table placing the order.
 * @property menuItemName The name of the menu item ordered.
 * @property quantity The quantity of the menu item ordered.
 */
@JsonClass(generateAdapter = true)
data class NewOrderEntry(
    @Json(name = "table_name") val tableName: String,
    @Json(name = "menu_item_name") val menuItemName: String,
    val quantity: Int
)