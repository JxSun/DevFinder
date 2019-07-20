package com.jxsun.devfinder.data.source.local.database

import androidx.room.*
import io.reactivex.Flowable

@Dao
interface GitHubUserDao {

    @Query("SELECT * FROM ${AppDatabase.GITHUB_USER_TABLE_NAME}")
    fun getAll(): Flowable<List<GitHubUserEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(entity: GitHubUserEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(vararg entity: GitHubUserEntity)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun update(entity: GitHubUserEntity)

    @Transaction
    fun upsert(entity: GitHubUserEntity) {
        if (-1L == insert(entity)) {
            update(entity)
        }
    }

    @Delete
    fun delete(entity: GitHubUserEntity)

    @Query("DELETE FROM ${AppDatabase.GITHUB_USER_TABLE_NAME}")
    fun clear()
}