package com.jxsun.devfinder.data.repository

import com.jxsun.devfinder.model.GitHubUser
import io.reactivex.Completable
import io.reactivex.Single

interface Repository<T> {

    fun loadCached(): Single<GitHubUserResult>

    fun query(keyword: String, nextPage: Int): Single<GitHubUserResult>

    fun clear(): Completable

    data class GitHubUserResult(
            val keyword: String,
            val nextPage: Int,
            val lastPage: Int,
            val users: List<GitHubUser>
    )
}