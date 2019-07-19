package com.jxsun.devfinder.feature

import com.jxsun.devfinder.RxImmediateSchedulerRule
import com.jxsun.devfinder.model.GitHubUser
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.verify
import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations

class DevListViewModelTest {

    @Rule
    @JvmField
    val schedulers = RxImmediateSchedulerRule()

    @Mock
    private lateinit var actionProcessor: DevListActionProcessor

    private lateinit var sut: DevListViewModel

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)

        sut = DevListViewModel(actionProcessor)
    }

    @Test
    fun `process initial intent`() {
        val processedResult = ObservableTransformer<DevListAction.InitialAction, DevListResult> {
            it.flatMap {
                Observable.fromArray(
                        DevListResult.InProgress(keyword = "", nextPage = 0, lastPage = 0),
                        DevListResult.Success(keyword = "", nextPage = 5, lastPage = 100, userList = listOf())
                )
            }
        }

        doReturn(processedResult).`when`(actionProcessor).process()

        val testObserver = sut.states().test()
        sut.processIntent(Observable.just(DevListIntent.InitialIntent))

        verify(actionProcessor).process()
        testObserver.assertValueAt(1) {
            it.isLoading && it.keyword.isEmpty() && it.nextPage == 0 && it.lastPage == 0 && it.userList.isEmpty() && it.error == null
        }
        testObserver.assertValueAt(2) {
            !it.isLoading && it.keyword.isEmpty() && it.nextPage == 5 && it.lastPage == 100 && it.userList.isEmpty() && it.error == null
        }
    }

    @Test
    fun `process search intent`() {
        val keyword = "Josh"
        val processedResult = ObservableTransformer<DevListAction.SearchAction, DevListResult> {
            it.flatMap {
                Observable.fromArray(
                        DevListResult.InProgress(keyword = keyword, nextPage = 0, lastPage = 0),
                        DevListResult.Success(
                                keyword = keyword,
                                nextPage = 2,
                                lastPage = 100,
                                userList = listOf(GitHubUser(
                                        id = 9999,
                                        loginName = "Josh",
                                        avatarUrl = ""
                                ))
                        )
                )
            }
        }

        doReturn(processedResult).`when`(actionProcessor).process()

        val testObserver = sut.states().test()
        sut.processIntent(Observable.just(DevListIntent.SearchIntent(keyword = keyword)))

        verify(actionProcessor).process()
        testObserver.assertValueAt(1) {
            it.isLoading && it.keyword == keyword && it.nextPage == 0 && it.lastPage == 0 && it.userList.isEmpty() && it.error == null
        }
        testObserver.assertValueAt(2) {
            !it.isLoading && it.keyword == keyword && it.nextPage == 2 && it.lastPage == 100 && it.userList.size == 1 && it.error == null
        }
    }

    @Test
    fun `process load more intent`() {
        val keyword = "Josh"
        val processedResult = ObservableTransformer<DevListAction.LoadMoreAction, DevListResult> {
            it.flatMap {
                Observable.fromArray(
                        DevListResult.InProgress(keyword = keyword, nextPage = 5, lastPage = 100),
                        DevListResult.Success(
                                keyword = keyword,
                                nextPage = 6,
                                lastPage = 100,
                                userList = listOf(GitHubUser(
                                        id = 9999,
                                        loginName = "Josh",
                                        avatarUrl = ""
                                ))
                        )
                )
            }
        }

        doReturn(processedResult).`when`(actionProcessor).process()

        val testObserver = sut.states().test()
        sut.processIntent(Observable.just(DevListIntent.LoadMoreIntent(keyword = keyword, nextPage = 5, lastPage = 100)))

        verify(actionProcessor).process()
        testObserver.assertValueAt(1) {
            it.isLoading && it.keyword == keyword && it.nextPage == 5 && it.lastPage == 100 && it.userList.isEmpty() && it.error == null
        }
        testObserver.assertValueAt(2) {
            !it.isLoading && it.keyword == keyword && it.nextPage == 6 && it.lastPage == 100 && it.userList.size == 1 && it.error == null
        }
    }
}