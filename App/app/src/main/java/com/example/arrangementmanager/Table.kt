package com.example.arrangementmanager

import java.util.UUID

data class Table(
// Class attributes are the same as the database
    val id: String = UUID.randomUUID().toString(),
    var name: String,
    var x: Float,
    var y: Float,
    var width: Float,
    var height: Float,
    val shape: TableShape // shape <==> category (in DB)
)

enum class TableShape {
    RECTANGLE,
    CIRCLE
}
