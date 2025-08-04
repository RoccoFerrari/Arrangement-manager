package com.example.arrangement_manager

import android.os.Parcelable
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.Parcelize

// Il server Flask restituisce un oggetto 'user'
@JsonClass(generateAdapter = true)
data class LoginResponse(
    val message: String,
    val user: User
)

@JsonClass(generateAdapter = true)
data class User(
    val email: String,
    val password: String
)

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

// Classe per le richieste PUT (Flask accetta solo campi da aggiornare)
@JsonClass(generateAdapter = true)
data class TableUpdate(
    @Json(name = "x_coordinate") val xCoordinate: Float? = null,
    @Json(name = "y_coordinate") val yCoordinate: Float? = null,
    val width: Float? = null,
    val height: Float? = null
)

@JsonClass(generateAdapter = true)
data class MenuItem(
    val name: String,
    @Json(name = "id_user") val idUser: String,
    val price: Float,
    val quantity: Int,
    val description: String
)

@JsonClass(generateAdapter = true)
data class MenuItemUpdate(
    val price: Float? = null,
    val quantity: Int? = null,
    val description: String? = null
)

@JsonClass(generateAdapter = true)
data class OrderEntry(
    @Json(name = "table_name") val tableName: String,
    @Json(name = "menu_item_name") val menuItemName: String,
    @Json(name = "id_user") val idUser: String,
    val quantity: Int
)

@JsonClass(generateAdapter = true)
data class NewOrderEntry(
    @Json(name = "table_name") val tableName: String,
    @Json(name = "menu_item_name") val menuItemName: String,
    val quantity: Int
)