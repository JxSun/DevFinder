package com.jxsun.devfinder.data.repository

import com.jxsun.devfinder.RxImmediateSchedulerRule
import com.jxsun.devfinder.data.local.AppPreferences
import com.jxsun.devfinder.data.local.LocalDataMapper
import com.jxsun.devfinder.data.local.database.GitHubUserDao
import com.jxsun.devfinder.data.local.database.GitHubUserEntity
import com.jxsun.devfinder.data.remote.GitHubResponse
import com.jxsun.devfinder.data.remote.GitHubService
import com.jxsun.devfinder.data.remote.RemoteDataMapper
import com.jxsun.devfinder.data.remote.UserResponse
import com.jxsun.devfinder.model.exception.NoConnectionException
import com.jxsun.devfinder.model.exception.ServerException
import com.jxsun.devfinder.util.NetworkChecker
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doNothing
import com.nhaarman.mockitokotlin2.doReturn
import io.reactivex.Flowable
import io.reactivex.Single
import okhttp3.HttpUrl
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.Result
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory

class GitHubUserRepositoryTest {

    @Rule
    @JvmField
    val schedulers = RxImmediateSchedulerRule()

    @Mock
    private lateinit var userDao: GitHubUserDao

    @Mock
    private lateinit var preferences: AppPreferences

    @Mock
    private lateinit var networkChecker: NetworkChecker

    private lateinit var mockWebServer: MockWebServer

    private lateinit var gitHubService: GitHubService

    private lateinit var sut: GitHubUserRepository

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)

        mockWebServer = MockWebServer()
        gitHubService = GitHubServiceStub(mockWebServer.url("/"))

        sut = GitHubUserRepository(
                gitHubService = gitHubService,
                userDao = userDao,
                preferences = preferences,
                localDataMapper = LocalDataMapper(),
                remoteDataMapper = RemoteDataMapper(),
                networkChecker = networkChecker
        )
    }

    @Test
    fun `load cached data and nothing found`() {
        doReturn("").`when`(preferences).keyword
        doReturn(1).`when`(preferences).nextPage
        doReturn(1).`when`(preferences).lastPage

        val testObservable = sut.loadCached().test()

        testObservable.assertValue { it.keyword == "" }
        testObservable.assertValue { it.nextPage == 0 }
        testObservable.assertValue { it.lastPage == 0 }
        testObservable.assertValue { it.users.isEmpty() }
    }

    @Test
    fun `load cached data and have existed data`() {
        val entity = GitHubUserEntity(
                id = 777,
                loginName = "Joshua Bloch",
                avatarUrl = "https://joshua.bloch"
        )
        doReturn("Josh").`when`(preferences).keyword
        doReturn(5).`when`(preferences).nextPage
        doReturn(50).`when`(preferences).lastPage
        doReturn(Flowable.just(listOf(entity))).`when`(userDao).getAll()

        val testObservable = sut.loadCached().test()

        testObservable.assertValue { it.keyword == "Josh" }
        testObservable.assertValue { it.nextPage == 5 }
        testObservable.assertValue { it.lastPage == 50 }
        testObservable.assertValue {
            it.users.size == 1 && it.users[0].run {
                this.id == entity.id && this.loginName == entity.loginName && this.avatarUrl == entity.avatarUrl
            }
        }
    }

    @Test
    fun `query with different keyword`() {
        doReturn("Trump").`when`(preferences).keyword
        doReturn(true).`when`(networkChecker).isNetworkConnected()
        doNothing().`when`(userDao).clear()
        doNothing().`when`(userDao).upsert(any())

        mockWebServer.enqueue(
                MockResponse()
                        .addHeader(
                                "Link",
                                "<https://api.github.com/resource?page=2>; rel=\"next\",<https://api.github.com/resource?page=5>; rel=\"last\""
                        )
                        .setResponseCode(200)
                        .setBody("{\n" +
                                "  \"total_count\": 12,\n" +
                                "  \"incomplete_results\": false,\n" +
                                "  \"items\": [\n" +
                                "    {\n" +
                                "      \"login\": \"mojombo\",\n" +
                                "      \"id\": 1,\n" +
                                "      \"node_id\": \"MDQ6VXNlcjE=\",\n" +
                                "      \"avatar_url\": \"https://secure.gravatar.com/avatar/25c7c18223fb42a4c6ae1c8db6f50f9b?d=https://a248.e.akamai.net/assets.github.com%2Fimages%2Fgravatars%2Fgravatar-user-420.png\",\n" +
                                "      \"gravatar_id\": \"\",\n" +
                                "      \"url\": \"https://api.github.com/users/mojombo\",\n" +
                                "      \"html_url\": \"https://github.com/mojombo\",\n" +
                                "      \"followers_url\": \"https://api.github.com/users/mojombo/followers\",\n" +
                                "      \"subscriptions_url\": \"https://api.github.com/users/mojombo/subscriptions\",\n" +
                                "      \"organizations_url\": \"https://api.github.com/users/mojombo/orgs\",\n" +
                                "      \"repos_url\": \"https://api.github.com/users/mojombo/repos\",\n" +
                                "      \"received_events_url\": \"https://api.github.com/users/mojombo/received_events\",\n" +
                                "      \"type\": \"User\",\n" +
                                "      \"score\": 105.47857\n" +
                                "    }\n" +
                                "  ]\n" +
                                "}")

        )

        val testObserver = sut.query("Josh", 1).test()

        testObserver.assertValue {
            it.users.size == 1 && it.users[0].loginName == "mojombo"
        }
        testObserver.assertValue { it.nextPage == 2 }
        testObserver.assertValue { it.lastPage == 5 }
    }

    @Test
    fun `query with different keyword but has no available connection`() {
        doReturn("Trump").`when`(preferences).keyword
        doReturn(false).`when`(networkChecker).isNetworkConnected()
        doNothing().`when`(userDao).clear()
        doNothing().`when`(userDao).upsert(any())

        val testObserver = sut.query("Josh", 1).test()

        testObserver.assertError { it is NoConnectionException }
    }

    @Test
    fun `query with different keyword but encounter server error`() {
        doReturn("Trump").`when`(preferences).keyword
        doReturn(true).`when`(networkChecker).isNetworkConnected()
        doNothing().`when`(userDao).clear()
        doNothing().`when`(userDao).upsert(any())

        mockWebServer.enqueue(
                MockResponse()
                        .setResponseCode(500)
                        .setStatus("HTTP/1.1 500 Internal Server Error")
        )

        val testObserver = sut.query("Josh", 1).test()

        testObserver.assertError { it is ServerException }
    }
}

class GitHubServiceStub(baseUrl: HttpUrl) : GitHubService {

    private val service = Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .build()
            .create(GitHubService::class.java)


    override fun getUsers(
            query: String,
            page: Int,
            clientId: String,
            clientSecret: String
    ): Single<Result<GitHubResponse<UserResponse>>> {
        return service.getUsers(query, page, clientId, clientSecret)
    }
}