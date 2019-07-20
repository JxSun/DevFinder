package com.jxsun.devfinder.data.repository

import com.jxsun.devfinder.RxImmediateSchedulerRule
import com.jxsun.devfinder.data.source.local.LocalDataSource
import com.jxsun.devfinder.data.source.remote.RemoteDataSource
import com.jxsun.devfinder.model.GitHubUser
import com.nhaarman.mockitokotlin2.doNothing
import com.nhaarman.mockitokotlin2.doReturn
import io.reactivex.Single
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations

class GitHubUserRepositoryTest {

    @Rule
    @JvmField
    val schedulers = RxImmediateSchedulerRule()

    @Mock
    private lateinit var remoteDataSource: RemoteDataSource

    @Mock
    private lateinit var localDataSource: LocalDataSource

    private lateinit var sut: GitHubUserRepository

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)

        sut = GitHubUserRepository(
                remoteDataSource = remoteDataSource,
                localDataSource = localDataSource
        )
    }

    @Test
    fun `load cached data and nothing found`() {
        doReturn("").`when`(localDataSource).getKeyword()
        doReturn(1).`when`(localDataSource).getNextPage()
        doReturn(1).`when`(localDataSource).getLastPage()

        val testObservable = sut.loadCached().test()

        testObservable.assertValue { it.keyword == "" }
        testObservable.assertValue { it.nextPage == -1 }
        testObservable.assertValue { it.lastPage == -1 }
        testObservable.assertValue { it.users.isEmpty() }
    }

    @Test
    fun `load cached data and have existed data`() {
        val users = listOf(GitHubUser(
                id = 777,
                loginName = "Joshua Bloch",
                avatarUrl = "https://joshua.bloch"
        ))

        doReturn("Josh").`when`(localDataSource).getKeyword()
        doReturn(5).`when`(localDataSource).getNextPage()
        doReturn(50).`when`(localDataSource).getLastPage()
        doReturn(Single.just(users)).`when`(localDataSource).loadUsers()

        val testObservable = sut.loadCached().test()

        testObservable.assertValue { it.keyword == "Josh" }
        testObservable.assertValue { it.nextPage == 5 }
        testObservable.assertValue { it.lastPage == 50 }
        testObservable.assertValue {
            it.users.size == 1 && it.users[0] == users[0]
        }
    }

    @Test
    fun `query with different keyword`() {
        val users = listOf(GitHubUser(
                id = 777,
                loginName = "Joshua Bloch",
                avatarUrl = "https://joshua.bloch"
        ))

        doReturn("Trump").`when`(localDataSource).getKeyword()
        doNothing().`when`(localDataSource).resetByKeyword("Josh")
        doReturn(Single.just(RemoteDataSource.UserData(
                nextPage = 2,
                lastPage = 100,
                users = users
        ))).`when`(remoteDataSource).getUsers("Josh", 1)

        val testObserver = sut.query("Josh", 1).test()

        testObserver.assertValue {
            it.users.size == 1 && it.users[0].loginName == "Joshua Bloch"
        }
        testObserver.assertValue { it.nextPage == 2 }
        testObserver.assertValue { it.lastPage == 100 }
    }
}
