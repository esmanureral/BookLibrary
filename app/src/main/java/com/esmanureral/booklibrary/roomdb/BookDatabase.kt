package com.esmanureral.booklibrary.roomdb

import androidx.room.Database
import androidx.room.RoomDatabase
import com.esmanureral.booklibrary.model.Book


@Database(entities = [Book::class], version = 1)
abstract class BookDatabase:RoomDatabase(){
    abstract fun bookDao():BookDAO
}