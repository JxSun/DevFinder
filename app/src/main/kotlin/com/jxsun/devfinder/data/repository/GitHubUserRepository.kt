package com.jxsun.devfinder.data.repository

import com.jxsun.devfinder.data.source.local.LocalDataSource
import com.jxsun.devfinder.data.source.remote.RemoteDataSource
import com.jxsun.devfinder.model.GitHubUser
import io.reactivex.Completable
import io.reactivex.Single
import timber.log.Timber

class GitHubUserRepository(
        private val remoteDataSource: RemoteDataSource,
        private val localDataSource: LocalDataSource
) : Repository<GitHubUser> {

    override fun loadCached(): Single<Repository.GitHubUserResult> {
        return Single.defer {
            val keyword = localDataSource.getKeyword()
            if (keyword.isNotBlank()) {
                localDataSource.loadUsers()
                        .map { users ->
                            Repository.GitHubUserResult(
                                    keyword = keyword,
                                    nextPage = localDataSource.getNextPage(),
                                    lastPage = localDataSource.getLastPage(),
                                    users = users
                            )
                        }
                        .doOnSuccess { Timber.d("cached loaded") }
            } else {
                Single.just(
                        Repository.GitHubUserResult.EMPTY
                )
            }
        }
    }

    override fun query(keyword: String, nextPage: Int): Single<Repository.GitHubUserResult> {
        return Single.fromCallable {
            // query with new keyword or re-query
            if (keyword != localDataSource.getKeyword() || nextPage == 1) {
                localDataSource.resetByKeyword(keyword)
            }
            keyword
        }.flatMap {
            Timber.d("start fetching page $nextPage")
            remoteDataSource.getUsers(keyword, nextPage)
                    .map {
                        Repository.GitHubUserResult(
                                keyword = keyword,
                                nextPage = it.nextPage,
                                lastPage = it.lastPage,
                                users = it.users
                        )
                    }
                    .doOnSuccess {
                        localDataSource.update(it)
                    }
        }
    }

    override fun clear(): Completable {
        return Completable.fromAction {
            localDataSource.clear()
        }
    }
}