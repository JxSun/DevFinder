package com.jxsun.devfinder.feature

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.jakewharton.rxbinding3.view.clicks
import com.jxsun.devfinder.R
import com.jxsun.devfinder.model.GitHubUser
import com.jxsun.devfinder.util.plusAssign
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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_devlist, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        disposables += devListViewModel.states().observeOn(AndroidSchedulers.mainThread()).subscribe(this::render)
        devListViewModel.processIntent(intents())
    }

    private fun intents(): Observable<DevListIntent> {
        return Observable.merge(
                Observable.just(DevListIntent.InitialIntent),
                searchIntent()
        )
    }

    private fun searchIntent(): Observable<DevListIntent> {
        return searchBtn.clicks()
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
        devList.forEach {
            Timber.v("dev: $it")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        disposables.clear()
    }
}