package com.jxsun.devfinder.data.repository

import androidx.annotation.VisibleForTesting
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

    @Volatile
    @VisibleForTesting
    var nextPage = 1

    @Volatile
    @VisibleForTesting
    var lastPage = 1

    private val resultDataParser = ResultDataParser()

    override fun loadCached(): Single<Repository.CachedGitHubUsers> {
        return Single.defer {
            val keyword = preferences.keyword
            nextPage = preferences.nextPage
            lastPage = preferences.lastPage
            if (keyword.isNotBlank()) {
                userDao.getAll()
                        .firstOrError()
                        .map {
                            Repository.CachedGitHubUsers(
                                    keyword = keyword,
                                    users = it.map(localDataMapper::toModel)
                            )
                        }
                        .doOnSuccess { Timber.d("cached loaded") }
            } else {
                Single.just(
                        Repository.CachedGitHubUsers(
                                keyword = "",
                                users = listOf()
                        )
                )
            }
        }
    }

    override fun query(keyword: String, forceFetch: Boolean): Single<List<GitHubUser>> {
        return Single.fromCallable {
            if (keyword != preferences.keyword) {
                preferences.keyword = keyword
                nextPage = 0
                lastPage = 0
                userDao.clear()
            }
            keyword
        }.flatMap {
            if (networkChecker.isNetworkConnected()) {
                if (lastPage == 0 // just being reset
                        || nextPage < lastPage) { // not reach the end yet
                    Timber.d("start fetching page $nextPage")
                    gitHubService.getUsers(keyword, nextPage)
                            .compose(resultDataParser.parse())
                            .map {
                                nextPage = it.link.nextPage
                                lastPage = it.link.lastPage
                                it.userDataList.map(remoteDataMapper::toModel)
                            }
                } else {
                    Timber.d("already reach the end: $lastPage")
                    Single.just(listOf())
                }
            } else {
                Single.error(NoConnectionException())
            }
        }.doOnSuccess {
            preferences.keyword = keyword
            preferences.nextPage = nextPage
            preferences.lastPage = lastPage

            it.forEach { user ->
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