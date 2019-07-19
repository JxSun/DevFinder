package com.jxsun.devfinder.data.repository

import com.jxsun.devfinder.data.local.AppPreferences
import com.jxsun.devfinder.data.local.LocalDataMapper
import com.jxsun.devfinder.data.local.database.GitHubUserDao
import com.jxsun.devfinder.data.remote.GitHubService
import com.jxsun.devfinder.data.remote.RemoteDataMapper
import com.jxsun.devfinder.data.remote.ResultDataParser
import com.jxsun.devfinder.model.GitHubUser
import com.jxsun.devfinder.model.exception.NoConnectionException
import com.jxsun.devfinder.util.NetworkChecker
import io.reactivex.Completable
import io.reactivex.Single
import timber.log.Timber

class GitHubUserRepository(
        private val gitHubService: GitHubService,
        private val userDao: GitHubUserDao,
        private val preferences: AppPreferences,
        private val localDataMapper: LocalDataMapper,
        private val remoteDataMapper: RemoteDataMapper,
        private val networkChecker: NetworkChecker
) : Repository<GitHubUser> {

    private val resultDataParser = ResultDataParser()

    override fun loadCached(): Single<Repository.GitHubUserResult> {
        return Single.defer {
            val keyword = preferences.keyword
            if (keyword.isNotBlank()) {
                userDao.getAll()
                        .firstOrError()
                        .map {
                            Repository.GitHubUserResult(
                                    keyword = keyword,
                                    nextPage = preferences.nextPage,
                                    lastPage = preferences.lastPage,
                                    users = it.map(localDataMapper::toModel)
                            )
                        }
                        .doOnSuccess { Timber.d("cached loaded") }
            } else {
                Single.just(
                        Repository.GitHubUserResult(
                                keyword = "",
                                nextPage = 0,
                                lastPage = 0,
                                users = listOf()
                        )
                )
            }
        }
    }

    override fun query(keyword: String, nextPage: Int): Single<Repository.GitHubUserResult> {
        return Single.fromCallable {
            // query with new keyword or re-query
            if (keyword != preferences.keyword || nextPage == 1) {
                preferences.keyword = keyword
                preferences.nextPage = 0
                preferences.lastPage = 0
                userDao.clear()
            }
            keyword
        }.flatMap {
            if (networkChecker.isNetworkConnected()) {
                Timber.d("start fetching page $nextPage")
                gitHubService.getUsers(keyword, nextPage)
                        .compose(resultDataParser.parse())
                        .map {
                            Repository.GitHubUserResult(
                                    keyword = keyword,
                                    nextPage = it.link.nextPage,
                                    lastPage = it.link.lastPage,
                                    users = it.userDataList.map(remoteDataMapper::toModel)
                            )
                        }
            } else {
                Single.error(NoConnectionException())
            }
        }.doOnSuccess {
            preferences.keyword = it.keyword
            preferences.nextPage = it.nextPage
            preferences.lastPage = it.lastPage

            it.users.forEach { user ->
                userDao.upsert(localDataMapper.fromModel(user))
            }
        }
    }

    override fun clear(): Completable {
        return Completable.fromAction {
            preferences.keyword = ""
            preferences.nextPage = 0
            preferences.lastPage = 0
            userDao.clear()
        }
    }
}