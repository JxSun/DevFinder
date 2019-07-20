package com.jxsun.devfinder.data.repository

import com.jxsun.devfinder.model.GitHubUser
import io.reactivex.Completable
import io.reactivex.Single

interface Repository<T> {

    fun loadCached(): Single<GitHubUserResult>

    fun query(keyword: String, nextPage: Int): Single<GitHubUserResult>

    fun clear(): Completable

    data class GitHubUserResult(
            val keyword: String = "",
            val nextPage: Int = -1,
            val lastPage: Int = -1,
            val users: List<GitHubUser> = listOf()
    ) {
        companion object {
            val EMPTY = GitHubUserResult()
        }
    }
}