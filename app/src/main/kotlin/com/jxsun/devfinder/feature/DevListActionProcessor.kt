package com.jxsun.devfinder.feature

import com.jxsun.devfinder.data.repository.Repository
import com.jxsun.devfinder.model.GitHubUser
import com.jxsun.devfinder.mvi.MviActionProcessor
import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import io.reactivex.schedulers.Schedulers
import timber.log.Timber

class DevListActionProcessor(
        private val userRepository: Repository<GitHubUser>
) : MviActionProcessor<DevListAction, DevListResult> {

    private fun processInitialAction(): ObservableTransformer<DevListAction.InitialAction, DevListResult> {
        return ObservableTransformer { action ->
            action.flatMap<DevListResult> {
                userRepository.loadCached()
                        .subscribeOn(Schedulers.io())
                        .toObservable()
                        .map {
                            DevListResult.Success(
                                    keyword = it.keyword,
                                    userList = it.users
                            )
                        }
                        .cast(DevListResult::class.java)
                        .startWith(DevListResult.InProgress(""))
            }
        }
    }

    private fun processSearchAction(): ObservableTransformer<DevListAction.SearchAction, DevListResult> {
        return ObservableTransformer { action ->
            action.flatMap<DevListResult> {
                Timber.v("search for ${it.keyword}")
                userRepository.query(keyword = it.keyword, forceFetch = true)
                        .subscribeOn(Schedulers.io())
                        .toObservable()
                        .map { result ->
                            DevListResult.Success(
                                    keyword = it.keyword,
                                    userList = result
                            )
                        }
                        .cast(DevListResult::class.java)
                        .startWith(DevListResult.InProgress(it.keyword))
            }
        }
    }

    private fun processLoadMoreAction(): ObservableTransformer<DevListAction.LoadMoreAction, DevListResult> {
        return ObservableTransformer { action ->
            action.flatMap<DevListResult> {
                userRepository.query(
                        keyword = it.keyword,
                        forceFetch = true
                )
                        .subscribeOn(Schedulers.io())
                        .toObservable()
                        .map { result ->
                            DevListResult.Success(
                                    keyword = it.keyword,
                                    userList = result
                            )
                        }
                        .cast(DevListResult::class.java)
                        .startWith(DevListResult.InProgress(it.keyword))
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
                                .compose(processSearchAction()),
                        shared.ofType(DevListAction.LoadMoreAction::class.java)
                                .compose(processLoadMoreAction())
                ).mergeWith(
                        shared.filter {
                            it !is DevListAction.InitialAction
                                    && it !is DevListAction.SearchAction
                                    && it !is DevListAction.LoadMoreAction
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