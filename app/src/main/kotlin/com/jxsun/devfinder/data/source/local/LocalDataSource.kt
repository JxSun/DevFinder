package com.jxsun.devfinder.data.source.local

import com.jxsun.devfinder.data.source.local.database.GitHubUserDao
import com.jxsun.devfinder.data.repository.Repository
import com.jxsun.devfinder.model.GitHubUser
import io.reactivex.Single

class LocalDataSource(
        private val preferences: AppPreferences,
        private val userDao: GitHubUserDao,
        private val localDataMapper: LocalDataMapper
) {

    fun getKeyword() = preferences.keyword

    fun getNextPage() = preferences.nextPage

    fun getLastPage() = preferences.lastPage

    fun loadUsers(): Single<List<GitHubUser>> {
        return userDao.getAll()
                .firstOrError()
                .map { it.map(localDataMapper::toModel) }
    }

    fun resetByKeyword(keyword: String) {
        preferences.keyword = keyword
        preferences.nextPage = -1
        preferences.lastPage = -1
        userDao.clear()
    }

    fun update(data: Repository.GitHubUserResult) {
        preferences.keyword = data.keyword
        preferences.nextPage = data.nextPage
        preferences.lastPage = data.lastPage

        data.users.forEach { user ->
            userDao.upsert(localDataMapper.fromModel(user))
        }
    }

    fun clear() {
        preferences.keyword = ""
        preferences.nextPage = -1
        preferences.lastPage = -1
        userDao.clear()
    }
}