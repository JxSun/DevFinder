package com.jxsun.devfinder.feature

import com.jxsun.devfinder.data.repository.Repository
import com.jxsun.devfinder.model.GitHubUser
import com.jxsun.devfinder.mvi.MviActionProcessor
import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import timber.log.Timber

class DevListActionProcessor(
        private val userRepository: Repository<GitHubUser>
) : MviActionProcessor<DevListAction, DevListResult> {

    private fun processInitialAction(): ObservableTransformer<DevListAction.InitialAction, DevListResult> {
        return ObservableTransformer { upstream ->
            upstream.flatMap<DevListResult> { action ->
                userRepository.loadCached()
                        .subscribeOn(Schedulers.io())
                        .toObservable()
                        .map { result ->
                            DevListResult.Success(
                                    keyword = result.keyword,
                                    nextPage = result.nextPage,
                                    lastPage = result.lastPage,
                                    userList = result.users
                            )
                        }
                        .cast(DevListResult::class.java)
                        .startWith(DevListResult.InProgress(
                                keyword = "",
                                nextPage = 0,
                                lastPage = 0
                        ))
            }
        }
    }

    private fun processSearchAction(): ObservableTransformer<DevListAction.SearchAction, DevListResult> {
        return ObservableTransformer { upstream ->
            upstream.flatMap<DevListResult> { action ->
                Timber.v("search: ${action.keyword}, next: ${action.nextPage}, last: ${action.lastPage}")
                when {
                    action.lastPage == 0 -> userRepository.query(keyword = action.keyword, nextPage = 1)
                    action.nextPage < action.lastPage -> userRepository.query(keyword = action.keyword, nextPage = action.nextPage)
                    else -> // Already reach the end
                        Single.just(Repository.GitHubUserResult(
                                keyword = action.keyword,
                                nextPage = action.nextPage,
                                lastPage = action.lastPage,
                                users = listOf()
                        ))
                }
                        .subscribeOn(Schedulers.io())
                        .toObservable()
                        .map { result ->
                            DevListResult.Success(
                                    keyword = action.keyword,
                                    nextPage = result.nextPage,
                                    lastPage = result.lastPage,
                                    userList = result.users
                            )
                        }
                        .cast(DevListResult::class.java)
                        .onErrorResumeNext { throwable: Throwable ->
                            Observable.just(
                                    DevListResult.Failure(
                                            keyword = action.keyword,
                                            nextPage = action.nextPage,
                                            lastPage = action.lastPage,
                                            error = throwable
                                    )
                            )
                        }
                        .startWith(DevListResult.InProgress(
                                keyword = action.keyword,
                                nextPage = action.nextPage,
                                lastPage = action.lastPage
                        ))
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
                                .map {
                                    DevListAction.SearchAction(
                                            keyword = it.keyword,
                                            nextPage = it.nextPage,
                                            lastPage = it.lastPage
                                    )
                                }
                                .compose(processSearchAction())
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