package com.jxsun.devfinder.data.source.local

import com.jxsun.devfinder.data.source.local.database.GitHubUserDao
import com.jxsun.devfinder.data.source.local.database.GitHubUserEntity
import com.nhaarman.mockitokotlin2.doNothing
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.verify
import io.reactivex.Flowable
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations

class LocalDataSourceTest {

    @Mock
    private lateinit var preferences: AppPreferences

    @Mock
    private lateinit var userDao: GitHubUserDao

    private lateinit var sut: LocalDataSource

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)

        sut = LocalDataSource(
                preferences = preferences,
                userDao = userDao,
                localDataMapper = LocalDataMapper()
        )
    }

    @Test
    fun `load cached data`() {
        val entity = GitHubUserEntity(
                id = 999,
                loginName = "Josh",
                avatarUrl = ""
        )
        doReturn(Flowable.just(listOf(entity)))
                .`when`(userDao)
                .getAll()

        val testObserver = sut.loadUsers().test()

        testObserver.assertValue {
            it.size == 1 && it[0].run {
                this.id == entity.id && this.loginName == entity.loginName && this.avatarUrl == entity.avatarUrl
            }
        }
    }

    @Test
    fun `reset by new keyword`() {
        doNothing().`when`(userDao).clear()

        sut.resetByKeyword("Josh")

        verify(preferences).keyword = "Josh"
        verify(preferences).nextPage = -1
        verify(preferences).lastPage = -1
        verify(userDao).clear()
    }

    @Test
    fun clear() {
        doNothing().`when`(userDao).clear()

        sut.clear()

        verify(preferences).keyword = ""
        verify(preferences).nextPage = -1
        verify(preferences).lastPage = -1
        verify(userDao).clear()
        verify(userDao).clear()
    }
}