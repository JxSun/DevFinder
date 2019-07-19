package com.jxsun.devfinder.data.remote

import com.jxsun.devfinder.model.exception.ClientException
import com.jxsun.devfinder.model.exception.ServerException
import com.jxsun.devfinder.model.exception.UnknownAccessException
import io.reactivex.Single
import io.reactivex.SingleTransformer
import okhttp3.Headers
import retrofit2.adapter.rxjava2.Result
import timber.log.Timber
import java.util.regex.Pattern

private const val LINK_HEADER = "Link"
private const val NEXT_PAGE_HEADER = "next"
private const val LAST_PAGE_HEADER = "last"
private const val LINK_FIELD_PATTERN = "(?<=page=)(\\d+)|(?<=rel=\").+?(?=\")"

data class ResultData(
        val link: Link,
        val userDataList: List<UserResponse>
) {
    data class Link(
            val nextPage: Int,
            val lastPage: Int
    )
}

class ResultDataParser {

    fun parse(): SingleTransformer<Result<GitHubResponse<UserResponse>>, ResultData> {
        return SingleTransformer { upstream ->
            upstream.flatMap { result ->
                // Check http response status
                val httpCode = result.response()?.code()
                if (httpCode != null) {
                    when {
                        httpCode in 400..499 -> throw ClientException(httpCode)
                        httpCode >= 500 -> throw ServerException(httpCode)
                    }
                }
                if (result.isError) {
                    result.error()?.let { throw it } ?: throw UnknownAccessException(httpCode)
                }

                val link = parsePagingInfo(result.response()?.headers())
                        .also {
                            Timber.d("link: next=${it.nextPage}, last=${it.lastPage}")
                        }
                val userList = result.response()?.body()?.items ?: listOf()

                Single.just(ResultData(
                        link = link,
                        userDataList = userList
                ))
            }
        }
    }

    /**
     * Parses the paging information from the Link header.
     */
    private fun parsePagingInfo(headers: Headers?): ResultData.Link {
        return headers?.get(LINK_HEADER)?.let { data ->
            val pattern = Pattern.compile(LINK_FIELD_PATTERN)
            val matcher = pattern.matcher(data)
            var parseNumber = true
            var page = 0
            var nextPage = 0
            var lastPage = 0
            while (matcher.find()) {
                if (parseNumber) {
                    page = matcher.group().toInt()
                    parseNumber = false
                } else {
                    when (matcher.group()) {
                        NEXT_PAGE_HEADER -> nextPage = page
                        LAST_PAGE_HEADER -> lastPage = page
                    }
                    parseNumber = true
                }
            }
            ResultData.Link(nextPage, lastPage)
        } ?: ResultData.Link(0, 0)
    }
}