package com.jxsun.devfinder.feature

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.reactivex.subjects.PublishSubject

private const val LOAD_MORE_THRESHOLD = 10

class OnRecyclerViewScrollListener : RecyclerView.OnScrollListener() {

    private var shouldLoadMore = false

    val reloadSubject = PublishSubject.create<Unit>()

    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
        val layoutManager = recyclerView.layoutManager as LinearLayoutManager
        val totalItemCount = layoutManager.itemCount
        val lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition()
        shouldLoadMore = totalItemCount <= lastVisibleItemPosition + LOAD_MORE_THRESHOLD
    }

    override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
        if (newState == RecyclerView.SCROLL_STATE_IDLE && shouldLoadMore) {
            reloadSubject.onNext(Unit)
        }
    }
}