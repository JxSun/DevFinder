package com.jxsun.devfinder.mvi

import io.reactivex.Observable

interface MviViewModel<I, S> where I: MviIntent, S: MviViewState {

    fun processIntent(intents: Observable<I>)

    fun states(): Observable<S>
}