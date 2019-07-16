package com.jxsun.devfinder.feature

import com.jxsun.devfinder.model.GitHubUser
import com.jxsun.devfinder.mvi.MviAction
import com.jxsun.devfinder.mvi.MviIntent
import com.jxsun.devfinder.mvi.MviResult
import com.jxsun.devfinder.mvi.MviViewState

sealed class DevListIntent : MviIntent {
    object InitialIntent : DevListIntent()
    data class SearchIntent(val keyword: String) : DevListIntent()
}

sealed class DevListAction : MviAction {
    object InitialAction : DevListAction()
    data class SearchAction(val keyword: String) : DevListAction()
}

sealed class DevListResult : MviResult {
    data class Success(val userList: List<GitHubUser>) : DevListResult()
    object InProgress : DevListResult()
    data class Failure(val error: Throwable) : DevListResult()
}

data class DevListViewState(
        val userList: List<GitHubUser>,
        val isLoading: Boolean,
        val error: Throwable? = null
) : MviViewState {
    companion object {
        val IDLE = DevListViewState(
                userList = listOf(),
                isLoading = false,
                error = null
        )
    }
}