package com.jxsun.devfinder.di

import com.jxsun.devfinder.data.local.AppPreferences
import com.jxsun.devfinder.data.local.database.AppDatabase
import com.jxsun.devfinder.data.remote.GitHubService
import com.jxsun.devfinder.data.remote.GitHubServiceImpl
import com.jxsun.devfinder.data.repository.GitHubUserRepository
import com.jxsun.devfinder.data.repository.Repository
import com.jxsun.devfinder.feature.DevListActionProcessor
import com.jxsun.devfinder.feature.DevListViewModel
import com.jxsun.devfinder.model.GitHubUser
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single<GitHubService> { GitHubServiceImpl() }

    single { AppPreferences(androidContext()) }

    single<Repository<GitHubUser>> {
        GitHubUserRepository(
                gitHubService = get(),
                database = AppDatabase.getInstance(androidContext()),
                preferences = get()
        )
    }

    single { DevListActionProcessor(get()) }

    viewModel { DevListViewModel(get()) }
}