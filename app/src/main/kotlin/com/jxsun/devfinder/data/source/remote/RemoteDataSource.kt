package com.jxsun.devfinder.data.source.remote

import com.jxsun.devfinder.model.GitHubUser
import com.jxsun.devfinder.model.exception.NoConnectionException
import com.jxsun.devfinder.util.NetworkChecker
import io.reactivex.Single

class RemoteDataSource(
        private val gitHubService: GitHubService,
        private val remoteDataMapper: RemoteDataMapper,
        private val networkChecker: NetworkChecker
) {

    private val responseDataParser = ResponseDataParser()

    fun getUsers(
            query: String,
            page: Int
    ): Single<UserData> {
        return if (networkChecker.isNetworkConnected()) {
            gitHubService.getUsers(query, page)
                    .compose(responseDataParser.parse())
                    .map {
                        UserData(
                                nextPage = it.link.nextPage,
                                lastPage = it.link.lastPage,
                                users = it.userDataList.map(remoteDataMapper::toModel)
                        )
                    }
        } else {
            Single.error(NoConnectionException())
        }
    }

    data class UserData(
            val nextPage: Int,
            val lastPage: Int,
            val users: List<GitHubUser>
    )
}