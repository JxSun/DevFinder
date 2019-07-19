package com.jxsun.devfinder.feature

import androidx.lifecycle.ViewModel
import com.jxsun.devfinder.mvi.MviViewModel
import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import io.reactivex.functions.BiFunction
import io.reactivex.subjects.PublishSubject
import timber.log.Timber

class DevListViewModel(
        private val actionProcessor: DevListActionProcessor
) : ViewModel(), MviViewModel<DevListIntent, DevListViewState> {

    private val intentSubject = PublishSubject.create<DevListIntent>()

    var lastViewState: DevListViewState = DevListViewState.IDLE

    private val intentFilter = ObservableTransformer<DevListIntent, DevListIntent> {
        it.publish { shared ->
            Observable.merge(
                    shared.ofType(DevListIntent.InitialIntent::class.java).take(1),
                    shared.ofType(DevListIntent.SearchIntent::class.java),
                    shared.ofType(DevListIntent.LoadMoreIntent::class.java)
            )
        }
    }

    private val viewStateReducer = BiFunction { prevState: DevListViewState, result: DevListResult ->
        Timber.d("result: $result")
        when (result) {
            is DevListResult.InProgress -> prevState.copy(
                    keyword = result.keyword,
                    nextPage = result.nextPage,
                    lastPage = result.lastPage,
                    isLoading = true
            )
            is DevListResult.Success -> prevState.copy(
                    keyword = result.keyword,
                    nextPage = result.nextPage,
                    lastPage = result.lastPage,
                    userList = result.userList,
                    isLoading = false,
                    error = null
            )
            is DevListResult.Failure -> prevState.copy(
                    keyword = result.keyword,
                    nextPage = result.nextPage,
                    lastPage = result.lastPage,
                    isLoading = false,
                    error = result.error
            )
        }
    }

    override fun processIntent(intents: Observable<DevListIntent>) {
        intents.subscribe(intentSubject)
    }

    override fun states(): Observable<DevListViewState> {
        return intentSubject
                .compose(intentFilter)
                .map(this::convertToAction)
                .compose(actionProcessor.process())
                .distinctUntilChanged()
                .scan(DevListViewState.IDLE, viewStateReducer)
                .replay()
                .autoConnect(0)
                .doOnNext { lastViewState = it }
    }

    private fun convertToAction(intent: DevListIntent): DevListAction {
        return when (intent) {
            is DevListIntent.InitialIntent -> DevListAction.InitialAction
            is DevListIntent.SearchIntent -> DevListAction.SearchAction(
                    keyword = intent.keyword,
                    nextPage = -1,
                    lastPage = -1
            )
            is DevListIntent.LoadMoreIntent -> DevListAction.LoadMoreAction(
                    keyword = intent.keyword,
                    nextPage = intent.nextPage,
                    lastPage = intent.lastPage
            )
        }
    }
}