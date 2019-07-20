package com.jxsun.devfinder.data.source.remote

import com.jxsun.devfinder.model.exception.NoConnectionException
import com.jxsun.devfinder.model.exception.ServerException
import com.jxsun.devfinder.util.NetworkChecker
import com.nhaarman.mockitokotlin2.doReturn
import io.reactivex.Single
import okhttp3.HttpUrl
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.Result
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory

class RemoteDataSourceTest {

    @Mock
    private lateinit var networkChecker: NetworkChecker

    private lateinit var mockWebServer: MockWebServer

    private lateinit var sut: RemoteDataSource

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)

        mockWebServer = MockWebServer()

        sut = RemoteDataSource(
                gitHubService = GitHubServiceStub(mockWebServer.url("/")),
                remoteDataMapper = RemoteDataMapper(),
                networkChecker = networkChecker
        )
    }

    @Test
    fun `fetch but has no connection`() {
        doReturn(false).`when`(networkChecker).isNetworkConnected()

        val testObserver = sut.getUsers("Josh", 1).test()

        testObserver.assertError { it is NoConnectionException }
    }

    @Test
    fun `fetch remote data successfully`() {
        doReturn(true).`when`(networkChecker).isNetworkConnected()

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

        val testObserver = sut.getUsers("mo", 1).test()

        testObserver.assertValue {
            it.users.size == 1 && it.users[0].loginName == "mojombo"
        }
        testObserver.assertValue { it.nextPage == 2 }
        testObserver.assertValue { it.lastPage == 5 }
    }

    @Test
    fun `fetch remote data but encounter server error`() {
        doReturn(true).`when`(networkChecker).isNetworkConnected()

        mockWebServer.enqueue(
                MockResponse()
                        .setResponseCode(500)
                        .setStatus("HTTP/1.1 500 Internal Server Error")
        )

        val testObserver = sut.getUsers("Josh", 1).test()

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