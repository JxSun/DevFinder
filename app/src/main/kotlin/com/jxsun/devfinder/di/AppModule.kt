package com.jxsun.devfinder.di

import com.jxsun.devfinder.data.remote.GitHubService
import com.jxsun.devfinder.data.remote.GitHubServiceImpl
import com.jxsun.devfinder.feature.DevListActionProcessor
import com.jxsun.devfinder.feature.DevListViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single<GitHubService> { GitHubServiceImpl() }

    single { DevListActionProcessor(get()) }

    viewModel { DevListViewModel(get()) }
}