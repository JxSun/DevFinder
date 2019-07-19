package com.jxsun.devfinder.feature

import com.jxsun.devfinder.RxImmediateSchedulerRule
import com.jxsun.devfinder.data.repository.Repository
import com.jxsun.devfinder.model.GitHubUser
import com.nhaarman.mockitokotlin2.doReturn
import io.reactivex.Observable
import io.reactivex.Single
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations

class DevListActionProcessorTest {

    @Rule
    @JvmField
    val schedulers = RxImmediateSchedulerRule()

    @Mock
    private lateinit var userRepository: Repository<GitHubUser>

    private lateinit var sut: DevListActionProcessor

    private val keyword = "Josh"

    private val users = listOf(GitHubUser(
            id = 9999,
            loginName = "Josh",
            avatarUrl = "https://localhost"
    ))

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)

        sut = DevListActionProcessor(userRepository)
    }

    @Test
    fun `perform initial action and load data back successfully`() {
        doReturn(Single.just(Repository.CachedGitHubUsers(keyword = keyword, users = users)))
                .`when`(userRepository)
                .loadCached()

        val testObserver = Observable.just(DevListAction.InitialAction)
                .compose(sut.process())
                .test()

        testObserver.assertValueCount(2)
        testObserver.assertValueAt(0, DevListResult.InProgress(keyword = ""))
        testObserver.assertValueAt(1, DevListResult.Success(keyword = keyword, userList = users))
    }

    @Test
    fun `perform search action successfully`() {
        doReturn(Single.just(users))
                .`when`(userRepository)
                .query(
                        keyword = keyword,
                        forceFetch = true
                )

        val testObserver = Observable.just(DevListAction.SearchAction(keyword = keyword))
                .compose(sut.process())
                .test()

        testObserver.assertValueCount(2)
        testObserver.assertValueAt(0, DevListResult.InProgress(keyword = keyword))
        testObserver.assertValueAt(1, DevListResult.Success(keyword = keyword, userList = users))
    }

    @Test
    fun `perform search action but encounter error`() {
        val exception = Exception("test")
        doReturn(Single.fromCallable { throw exception })
                .`when`(userRepository)
                .query(
                        keyword = keyword,
                        forceFetch = true
                )

        val testObserver = Observable.just(DevListAction.SearchAction(keyword = keyword))
                .compose(sut.process())
                .test()

        testObserver.values().forEach {
            println(it)
        }
        testObserver.assertValueCount(2)
        testObserver.assertValueAt(0, DevListResult.InProgress(keyword = keyword))
        testObserver.assertValueAt(1, DevListResult.Failure(keyword = keyword, error = exception))
    }

    @Test
    fun `perform load more action successfully`() {
        doReturn(Single.just(users))
                .`when`(userRepository)
                .query(
                        keyword = keyword,
                        forceFetch = true
                )

        val testObserver = Observable.just(DevListAction.LoadMoreAction(keyword = keyword))
                .compose(sut.process())
                .test()

        testObserver.assertValueCount(2)
        testObserver.assertValueAt(0, DevListResult.InProgress(keyword = keyword))
        testObserver.assertValueAt(1, DevListResult.Success(keyword = keyword, userList = users))
    }

    @Test
    fun `perform load more action but encounter error`() {
        val exception = Exception("test")
        doReturn(Single.fromCallable { throw exception })
                .`when`(userRepository)
                .query(
                        keyword = keyword,
                        forceFetch = true
                )

        val testObserver = Observable.just(DevListAction.LoadMoreAction(keyword = keyword))
                .compose(sut.process())
                .test()

        testObserver.values().forEach {
            println(it)
        }
        testObserver.assertValueCount(2)
        testObserver.assertValueAt(0, DevListResult.InProgress(keyword = keyword))
        testObserver.assertValueAt(1, DevListResult.Failure(keyword = keyword, error = exception))
    }
}