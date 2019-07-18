package com.jxsun.devfinder.data.repository

import com.jxsun.devfinder.model.GitHubUser
import io.reactivex.Completable
import io.reactivex.Single

interface Repository<T> {

    fun loadCached(): Single<CachedGitHubUsers>

    fun query(keyword: String, forceFetch: Boolean = false): Single<List<GitHubUser>>

    fun clear(): Completable

    data class CachedGitHubUsers(
            val keyword: String,
            val users: List<GitHubUser>
    )
}