package com.jxsun.devfinder.feature

import android.animation.Animator
import android.graphics.Rect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.bartoszlipinski.viewpropertyobjectanimator.ViewPropertyObjectAnimator
import com.jakewharton.rxbinding3.view.clicks
import com.jxsun.devfinder.R
import com.jxsun.devfinder.model.GitHubUser
import com.jxsun.devfinder.model.exception.ClientException
import com.jxsun.devfinder.model.exception.NoConnectionException
import com.jxsun.devfinder.model.exception.ServerException
import com.jxsun.devfinder.util.extention.hideSoftInput
import com.jxsun.devfinder.util.extention.plusAssign
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.fragment_devlist.*
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber
import java.util.concurrent.TimeUnit

class DevListFragment : Fragment() {

    companion object {
        const val TAG = "DevListFragment"

        fun newInstance() = DevListFragment()
    }

    private val devListViewModel: DevListViewModel by viewModel()
    private val disposables = CompositeDisposable()
    private val recyclerViewAdapter = DevListRecyclerViewAdapter()
    private val onScrollListener = OnRecyclerViewScrollListener()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_devlist, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        with(recyclerView) {
            adapter = recyclerViewAdapter
            layoutManager = LinearLayoutManager(this.context)
            addItemDecoration(DividerItemDecoration(this.context, DividerItemDecoration.VERTICAL))
            addOnScrollListener(onScrollListener)
            setHasFixedSize(true)
        }

        disposables += devListViewModel.states().observeOn(AndroidSchedulers.mainThread()).subscribe(this::render)
        devListViewModel.processIntent(intents())
    }

    private fun intents(): Observable<DevListIntent> {
        return Observable.merge(
                Observable.just(DevListIntent.InitialIntent),
                searchIntent(),
                loadMoreIntent()
        )
    }

    private fun searchIntent(): Observable<DevListIntent> {
        return searchBtn.clicks()
                .throttleFirst(300, TimeUnit.MILLISECONDS)
                .filter { searchInput.text.toString().isNotBlank() }
                .map {
                    DevListIntent.SearchIntent(
                            keyword = searchInput.text.toString()
                    )
                }
                .cast(DevListIntent::class.java)
                .doOnNext { hideSoftInput() }
    }

    private fun loadMoreIntent(): Observable<DevListIntent> {
        return onScrollListener.reloadSubject
                .map {
                    DevListIntent.LoadMoreIntent(
                            keyword = searchInput.text.toString()
                    )
                }
    }

    fun render(state: DevListViewState) {
        Timber.d("state: $state")
        if (state.isLoading) {
            progressBar.visibility = View.VISIBLE
        } else {
            progressBar.visibility = View.GONE
        }

        if (state.error != null) {
            showError(state.error)
        } else if (state.userList.isNotEmpty() && !state.isLoading) {
            if (state.firstShow) {
                val offsetViewBounds = Rect()
                searchBar.getDrawingRect(offsetViewBounds)
                container.offsetDescendantRectToMyCoords(searchBar, offsetViewBounds)

                ViewPropertyObjectAnimator
                        .animate(searchBar)
                        .setDuration(500)
                        .setInterpolator(DecelerateInterpolator())
                        .margin(0)
                        .translationY(-offsetViewBounds.top.toFloat())
                        .addListener(object : Animator.AnimatorListener {
                            override fun onAnimationRepeat(animation: Animator?) {
                            }

                            override fun onAnimationEnd(animation: Animator?) {
                                searchBar.clearAnimation()
                                recyclerView.visibility = View.VISIBLE
                                showDevelopers(state.keyword, state.userList)
                                searchBar.visibility = View.VISIBLE
                                searchBar.isClickable = true
                            }

                            override fun onAnimationCancel(animation: Animator?) {
                            }

                            override fun onAnimationStart(animation: Animator?) {
                            }
                        })
                        .start()

                searchBar.isClickable = false
                searchInput.setText(state.keyword)
            } else {
                showDevelopers(state.keyword, state.userList)
            }
        }
    }

    private fun showError(error: Throwable) {
        context?.let {
            when (error) {
                is ServerException -> R.string.server_error
                is ClientException -> R.string.client_error
                is NoConnectionException -> R.string.connectivity_error
                else -> R.string.unknown_error
            }.let { resId ->
                Toast.makeText(it, getString(resId), Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showDevelopers(keyword: String, devList: List<GitHubUser>) {
        recyclerViewAdapter.addDevList(keyword, devList)
    }

    override fun onDestroyView() {
        disposables.clear()
        recyclerView?.adapter = null
        super.onDestroyView()
    }
}