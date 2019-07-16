package com.jxsun.devfinder.feature

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.jakewharton.rxbinding2.view.RxView
import com.jxsun.devfinder.R
import com.jxsun.devfinder.model.GitHubUser
import com.jxsun.devfinder.util.ViewModelFactory
import com.jxsun.devfinder.util.plusAssign
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.fragment_devlist.*
import timber.log.Timber
import java.util.concurrent.TimeUnit

class DevListFragment : Fragment() {

    companion object {
        const val TAG = "DevListFragment"

        fun newInstance() = DevListFragment()
    }

    private lateinit var viewModel: DevListViewModel
    private val disposables = CompositeDisposable()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_devlist, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        viewModel = ViewModelFactory.getInstance(this.context!!).create(DevListViewModel::class.java)

        disposables += viewModel.states().subscribe(this::render)
        viewModel.processIntent(intents())
    }

    private fun intents(): Observable<DevListIntent> {
        return Observable.merge(
                Observable.just(DevListIntent.InitialIntent),
                searchIntent()
        )
    }

    private fun searchIntent(): Observable<DevListIntent> {
        return RxView.clicks(searchBtn)
                .throttleFirst(300, TimeUnit.MILLISECONDS)
                .filter { searchBar.text.toString().isNotBlank() }
                .map {
                    DevListIntent.SearchIntent(
                            keyword = searchBar.text.toString()
                    )
                }
                .cast(DevListIntent::class.java)
    }

    fun render(state: DevListViewState) {
        Timber.d("state: $state")
        if (state.isLoading) {
            progressBar.visibility = View.VISIBLE
        } else {
            progressBar.visibility = View.GONE
        }

        if (state.error != null) {
            showError()
        } else if (state.userList.isNotEmpty()) {
            showDevelopers(state.userList)
        }
    }

    private fun showError() {

    }

    private fun showDevelopers(devList: List<GitHubUser>) {

    }

    override fun onDestroyView() {
        super.onDestroyView()
        disposables.clear()
    }
}