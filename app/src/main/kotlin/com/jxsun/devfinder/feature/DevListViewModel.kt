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
                    isLoading = true
            )
            is DevListResult.Success -> prevState.copy(
                    firstShow = prevState.firstShow && prevState.userList.isEmpty(),
                    keyword = result.keyword,
                    userList = result.userList,
                    isLoading = false,
                    error = null
            )
            is DevListResult.Failure -> prevState.copy(
                    keyword = result.keyword,
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
    }

    private fun convertToAction(intent: DevListIntent): DevListAction {
        return when (intent) {
            is DevListIntent.InitialIntent -> DevListAction.InitialAction
            is DevListIntent.SearchIntent -> DevListAction.SearchAction(
                    keyword = intent.keyword
            )
            is DevListIntent.LoadMoreIntent -> DevListAction.LoadMoreAction(
                    keyword = intent.keyword
            )
        }
    }
}