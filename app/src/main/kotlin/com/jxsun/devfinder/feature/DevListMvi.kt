package com.jxsun.devfinder.feature

import com.jxsun.devfinder.model.GitHubUser
import com.jxsun.devfinder.mvi.MviAction
import com.jxsun.devfinder.mvi.MviIntent
import com.jxsun.devfinder.mvi.MviResult
import com.jxsun.devfinder.mvi.MviViewState

sealed class DevListIntent : MviIntent {
    object InitialIntent : DevListIntent()
    data class SearchIntent(val keyword: String) : DevListIntent()
    data class LoadMoreIntent(val keyword: String) : DevListIntent()
}

sealed class DevListAction : MviAction {
    object InitialAction : DevListAction()
    data class SearchAction(val keyword: String) : DevListAction()
    data class LoadMoreAction(val keyword: String) : DevListAction()
}

sealed class DevListResult : MviResult {
    data class Success(
            val keyword: String,
            val userList: List<GitHubUser>
    ) : DevListResult()

    data class InProgress(
            val keyword: String
    ) : DevListResult()

    data class Failure(
            val keyword: String,
            val error: Throwable?
    ) : DevListResult()
}

data class DevListViewState(
        val firstShow: Boolean,
        val keyword: String,
        val isLoading: Boolean,
        val userList: List<GitHubUser>,
        val error: Throwable? = null
) : MviViewState {
    companion object {
        val IDLE = DevListViewState(
                firstShow = true,
                keyword = "",
                isLoading = false,
                userList = listOf(),
                error = null
        )
    }
}