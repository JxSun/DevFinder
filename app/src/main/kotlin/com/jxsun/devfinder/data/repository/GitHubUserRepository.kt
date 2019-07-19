package com.jxsun.devfinder.data.repository

import androidx.annotation.VisibleForTesting
import com.jxsun.devfinder.data.local.AppPreferences
import com.jxsun.devfinder.data.local.LocalDataMapper
import com.jxsun.devfinder.data.local.database.GitHubUserDao
import com.jxsun.devfinder.data.remote.GitHubService
import com.jxsun.devfinder.data.remote.RemoteDataMapper
import com.jxsun.devfinder.model.GitHubUser
import com.jxsun.devfinder.model.exception.ClientException
import com.jxsun.devfinder.model.exception.NoConnectionException
import com.jxsun.devfinder.model.exception.ServerException
import com.jxsun.devfinder.model.exception.UnknownAccessException
import com.jxsun.devfinder.util.NetworkChecker
import io.reactivex.Completable
import io.reactivex.Single
import okhttp3.Headers
import timber.log.Timber
import java.util.regex.Pattern

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
    var maxPage = 1

    override fun loadCached(): Single<Repository.CachedGitHubUsers> {
        return Single.defer {
            val keyword = preferences.keyword
            nextPage = preferences.nextPage
            maxPage = preferences.maxPage
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
                nextPage = 1
                maxPage = 1
                userDao.clear()
            }
            keyword
        }.flatMap {
            if (networkChecker.isNetworkConnected()) {
                gitHubService.getUsers(keyword, nextPage)
            } else {
                Single.error(NoConnectionException())
            }
        }.map { result ->
            // Check http response status
            val httpCode = result.response()?.code()
            if (httpCode != null) {
                when {
                    httpCode in 400..499 -> throw ClientException(httpCode)
                    httpCode >= 500 -> throw ServerException(httpCode)
                }
            }
            if (result.isError) {
                result.error()?.let { throw it } ?: throw UnknownAccessException(httpCode)
            }

            parsePagingInfo(result.response()?.headers())
                    .also { linkDataList ->
                        nextPage = linkDataList.find { data -> data.linkIndicator == "next" }?.number
                                ?: 1
                        maxPage = linkDataList.find { data -> data.linkIndicator == "last" }?.number
                                ?: 1
                        Timber.d("link: next=$nextPage, max=$maxPage")
                    }

            result.response()?.body()?.items?.map(remoteDataMapper::toModel) ?: listOf()
        }.doOnSuccess {
            preferences.keyword = keyword
            preferences.nextPage = nextPage
            preferences.maxPage = maxPage

            it.forEach { user ->
                userDao.upsert(localDataMapper.fromModel(user))
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
            userDao.clear()
        }
    }

    data class LinkData(
            val linkIndicator: String,
            val number: Int
    )
}