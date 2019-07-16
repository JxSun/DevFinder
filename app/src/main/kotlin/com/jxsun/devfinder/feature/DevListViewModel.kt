package com.jxsun.devfinder.feature

import androidx.lifecycle.ViewModel
import com.jxsun.devfinder.mvi.MviViewModel
import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import io.reactivex.functions.BiFunction
import io.reactivex.subjects.PublishSubject

class DevListViewModel(
        private val actionProcessor: DevListActionProcessor
) : ViewModel(), MviViewModel<DevListIntent, DevListViewState> {

    private val intentSubject = PublishSubject.create<DevListIntent>()

    private val intentFilter = ObservableTransformer<DevListIntent, DevListIntent> {
        it.publish { shared ->
            Observable.merge(
                    shared.ofType(DevListIntent.InitialIntent::class.java).take(1),
                    shared.ofType(DevListIntent.SearchIntent::class.java)
            )
        }
    }

    private val viewStateReducer = BiFunction { prevState: DevListViewState, result: DevListResult ->
        when (result) {
            is DevListResult.InProgress -> prevState.copy(
                    isLoading = true
            )
            is DevListResult.Success -> prevState.copy(
                    userList = result.userList,
                    isLoading = false,
                    error = null
            )
            is DevListResult.Failure -> prevState.copy(
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
                .scan(DevListViewState.IDLE, viewStateReducer)
                .distinctUntilChanged()
                .replay()
                .autoConnect(0)
    }

    private fun convertToAction(intent: DevListIntent): DevListAction {
        return when (intent) {
            is DevListIntent.InitialIntent -> DevListAction.InitialAction
            is DevListIntent.SearchIntent -> DevListAction.SearchAction(
                    keyword = intent.keyword
            )
        }
    }
}