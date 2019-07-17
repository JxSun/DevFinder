package com.jxsun.devfinder.feature

import com.jxsun.devfinder.data.remote.GitHubService
import com.jxsun.devfinder.model.GitHubUser
import com.jxsun.devfinder.mvi.MviActionProcessor
import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import io.reactivex.schedulers.Schedulers
import timber.log.Timber

class DevListActionProcessor(
        private val gitHubService: GitHubService
) : MviActionProcessor<DevListAction, DevListResult> {

    private fun processInitialAction(): ObservableTransformer<DevListAction.InitialAction, DevListResult> {
        return ObservableTransformer { action ->
            action.flatMap<DevListResult> {
                Observable.just(DevListResult.Success(userList = listOf()))
                        .cast(DevListResult::class.java)
                        .startWith(DevListResult.InProgress)
            }
        }
    }

    private fun processSearchAction(): ObservableTransformer<DevListAction.SearchAction, DevListResult> {
        return ObservableTransformer { action ->
            action.flatMap<DevListResult> {
                Timber.v("search for ${it.keyword}")
                gitHubService.getUsers(it.keyword)
                        .subscribeOn(Schedulers.io())
                        .toObservable()
                        .map { response ->
                            DevListResult.Success(
                                    userList = response.items.map { remoteUser ->
                                        GitHubUser(
                                                id = remoteUser.id,
                                                loginName = remoteUser.name,
                                                avatarUrl = remoteUser.avatarUrl
                                        )
                                    }
                            )
                        }
                        .cast(DevListResult::class.java)
                        .startWith(DevListResult.InProgress)
            }
        }
    }

    override fun process(): ObservableTransformer<DevListAction, DevListResult> {
        return ObservableTransformer { actions ->
            actions.publish { shared ->
                Observable.merge(
                        shared.ofType(DevListAction.InitialAction::class.java)
                                .compose(processInitialAction()),
                        shared.ofType(DevListAction.SearchAction::class.java)
                                .compose(processSearchAction())
                ).mergeWith(
                        shared.filter {
                            it !is DevListAction.InitialAction
                                    && it !is DevListAction.SearchAction
                        }.flatMap {
                            Observable.error<DevListResult>(
                                    IllegalStateException("Unknown action type: $it")
                            )
                        }
                )
            }
        }
    }
}