package com.esmanureral.booklibrary.roomdb

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.esmanureral.booklibrary.model.Book
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Flowable

//veritabanındaki tablolara erişim..
@Dao
interface BookDAO {
    //Flowable: RxJava'nın bir sınıfıdır ve verinin değiştiği her durumda bildirim yapılmasını sağlar.
    @Query("SELECT * FROM Book")
    fun getAll(): Flowable<List<Book>>

    @Query("SELECT * FROM Book WHERE id=:id")
    fun findById(id:Int):Flowable<Book>

    @Insert
    fun insert(book:Book): Completable

    @Delete
    fun delete(book:Book):Completable


}