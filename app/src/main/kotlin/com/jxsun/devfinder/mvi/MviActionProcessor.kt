package com.jxsun.devfinder.mvi

import io.reactivex.ObservableTransformer

interface MviActionProcessor<A, R> where A : MviAction, R : MviResult {
    fun process(): ObservableTransformer<A, R>
}