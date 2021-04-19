/*
 * Copyright (c) 2018 DuckDuckGo
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.duckduckgo.app.bookmarks.db

import androidx.room.*
import com.duckduckgo.app.bookmarks.model.SavedSite
import io.reactivex.Single
import kotlinx.coroutines.flow.Flow
import org.jetbrains.anko.collections.forEachWithIndex
import java.util.*

@Dao
interface FavoritesDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(favorite: FavoriteEntity): Long

    @Query("select * from favorites order by position")
    fun favorites(): Flow<List<FavoriteEntity>>

    @Query("select count(*) from favorites WHERE url LIKE :url")
    fun favoritesCountByUrl(url: String): Int

    @Delete
    fun delete(favorite: FavoriteEntity)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun update(favoriteEntity: FavoriteEntity)

    @Query("select * from favorites")
    fun favoritesObservable(): Single<List<FavoriteEntity>>

    @Query("select CAST(COUNT(*) AS BIT) from favorites")
    suspend fun hasBookmarks(): Boolean

    @Query("select position from favorites where id = ( select MAX(id) from favorites)")
    suspend fun getLastPosition(): Int?

    @Query("select * from favorites where id = :id")
    fun favorite(id: Long): FavoriteEntity?

    @Transaction
    suspend fun persistChanges(favorites: List<SavedSite.Favorite>) {
        favorites.forEachWithIndex { index, favorite ->
            val favoriteEntity = favorite(favorite.id) ?: return
            favoriteEntity.position = index
            update(favoriteEntity)
        }
    }
}
