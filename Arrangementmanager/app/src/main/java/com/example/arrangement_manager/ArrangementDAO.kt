package com.example.arrangement_manager

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.ForeignKey
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "User")
data class User(
    @PrimaryKey @ColumnInfo(name = "email") val email: String,
    @ColumnInfo(name = "password") val password: String
)

@Entity(primaryKeys = ["name", "id_user"],
    tableName = "Table_",
    foreignKeys = [ForeignKey(
        entity = User::class,
        parentColumns = ["email"],
        childColumns = ["id_user"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class Table_(
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "x_coordinate") val x_coordinate: Float,
    @ColumnInfo(name = "y_coordinate") val y_coordinate: Float,
    @ColumnInfo(name = "width") val width: Float,
    @ColumnInfo(name = "height") val height: Float,
    @ColumnInfo(name = "id_user") val id_user: String
)

@Entity(primaryKeys = ["name", "id_user"],
    tableName = "Menu",
    foreignKeys = [ForeignKey(
        entity = User::class,
        parentColumns = ["email"],
        childColumns = ["id_user"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class MenuItem(
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "price") val price: Float,
    @ColumnInfo(name = "description") val description: String,
    @ColumnInfo(name = "id_user") val id_user: String
)

@Dao
interface ArrangementDAO {

    // Queries

    @Query("SELECT * FROM User")
    fun getAllUsers(): Flow<List<User>>

    @Query("SELECT * FROM User WHERE email = :email")
    suspend fun getUserByEmail(email: String): User?

    @Query("SELECT * FROM Table_ WHERE id_user = :userId ORDER BY name ASC")
    fun getAllTablesByUsers(userId: String): Flow<List<Table_>>

    @Query("SELECT * FROM Menu WHERE id_user = :userId ORDER BY name ASC")
    fun getAllMenuByUser(userId: String): Flow<List<MenuItem>>

    @Query("SELECT EXISTS(SELECT 1 FROM User WHERE Email = :email Limit 1)")
    fun emailExist(email: String) : Boolean


    // Inserts

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertUser(user: User)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMenu(menuItem: MenuItem)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTable(table: Table_)


    // Updates

    @Update
    suspend fun updateTable(table: Table_)

    // Deletes

    @Query("DELETE FROM Menu WHERE id_user = :userId AND name = :name")
    suspend fun deleteMenuItemByNameAndUser(name: String, userId: String)

    @Query("DELETE FROM Table_ WHERE id_user = :userId AND name = :name")
    suspend fun deleteTableByNameAndUser(name: String, userId: String)

}