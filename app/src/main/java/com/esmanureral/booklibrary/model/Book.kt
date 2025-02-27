package com.esmanureral.booklibrary.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey


//Room için
@Entity
class Book(
    @ColumnInfo(name="isim")
    var isim:String,
    @ColumnInfo(name = "yazar")
    var yazar:String,
    @ColumnInfo(name="gorsel")
    var gorsel:ByteArray
)
{
    @PrimaryKey(autoGenerate = true)
    var id=0
}