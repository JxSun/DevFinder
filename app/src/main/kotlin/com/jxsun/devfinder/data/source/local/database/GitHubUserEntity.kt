package com.jxsun.devfinder.data.source.local.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = AppDatabase.GITHUB_USER_TABLE_NAME)
data class GitHubUserEntity(
        @PrimaryKey val id: Long,
        val loginName: String,
        val avatarUrl: String
)