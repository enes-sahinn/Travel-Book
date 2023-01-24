package com.enessahin.travelbook.roomdb

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.enessahin.travelbook.model.Place
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Flowable

@Dao
interface PlaceDao {

    @Insert
    fun insert(place: Place): Completable

    @Delete
    fun delete(place: Place): Completable

    @Query("SELECT * FROM Place")
    fun getAll(): Flowable<List<Place>>
}

