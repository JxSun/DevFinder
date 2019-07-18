package com.jxsun.devfinder.data.repository

import com.jxsun.devfinder.data.local.AppPreferences
import com.jxsun.devfinder.data.local.database.AppDatabase
import com.jxsun.devfinder.data.local.database.GitHubUserEntity
import com.jxsun.devfinder.data.remote.GitHubService
import com.jxsun.devfinder.model.GitHubUser
import io.reactivex.Completable
import io.reactivex.Single
import okhttp3.Headers
import timber.log.Timber
import java.util.regex.Pattern

class GitHubUserRepository(
        private val gitHubService: GitHubService,
        private val database: AppDatabase,
        private val preferences: AppPreferences
) : Repository<GitHubUser> {

    @Volatile
    private var nextPage = 1

    @Volatile
    private var maxPage = 1

    override fun loadCached(): Single<Repository.CachedGitHubUsers> {
        return Single.defer {
            val keyword = preferences.keyword
            nextPage = preferences.nextPage
            maxPage = preferences.maxPage
            if (keyword.isNotBlank()) {
                database.userDao().getAll()
                        .firstOrError()
                        .map {
                            Repository.CachedGitHubUsers(
                                    keyword = keyword,
                                    users = it.map { entity ->
                                        GitHubUser(
                                                id = entity.id,
                                                loginName = entity.loginName,
                                                avatarUrl = entity.avatarUrl
                                        )
                                    }
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
                nextPage = 1
                maxPage = 1
                database.userDao().clear()
            }
            keyword
        }.flatMap {
            gitHubService.getUsers(keyword, nextPage)
        }.map {
            parsePagingInfo(it.response()?.headers())
                    .also { linkDataList ->
                        nextPage = linkDataList.find { data -> data.linkIndicator == "next" }?.number
                                ?: 1
                        maxPage = linkDataList.find { data -> data.linkIndicator == "last" }?.number
                                ?: 1
                        Timber.d("link: next=$nextPage, max=$maxPage")
                    }

            it.response()?.body()?.items?.map { response ->
                GitHubUser(
                        id = response.id,
                        loginName = response.name,
                        avatarUrl = response.avatarUrl
                )
            } ?: listOf()
        }.doOnSuccess {
            preferences.keyword = keyword
            preferences.nextPage = nextPage
            preferences.maxPage = maxPage

            it.forEach { user ->
                database.userDao().upsert(
                        GitHubUserEntity(
                                id = user.id,
                                loginName = user.loginName,
                                avatarUrl = user.avatarUrl
                        )
                )
            }
        }
    }

    private fun parsePagingInfo(headers: Headers?): List<LinkData> {
        return headers?.get("Link")?.let { data ->
            val pattern = Pattern.compile("(?<=page=)(\\d+)|(?<=rel=\").+?(?=\")")
            val matcher = pattern.matcher(data)
            val list = mutableListOf<LinkData>()
            var parseNumber = true
            var number = 0
            var indicator: String
            while (matcher.find()) {
                if (parseNumber) {
                    number = matcher.group().toInt()
                    parseNumber = false
                } else {
                    indicator = matcher.group()
                    list.add(
                            LinkData(
                                    linkIndicator = indicator,
                                    number = number
                            )
                    )
                    parseNumber = true
                }
            }
            list
        } ?: listOf()
    }

    override fun clear(): Completable {
        return Completable.fromAction {
            preferences.keyword = ""
            preferences.nextPage = 1
            preferences.maxPage = 1
            database.userDao().clear()
        }
    }

    data class LinkData(
            val linkIndicator: String,
            val number: Int
    )
}