package com.jxsun.devfinder.di

import com.jxsun.devfinder.data.local.AppPreferences
import com.jxsun.devfinder.data.local.LocalDataMapper
import com.jxsun.devfinder.data.local.database.AppDatabase
import com.jxsun.devfinder.data.remote.GitHubService
import com.jxsun.devfinder.data.remote.GitHubServiceImpl
import com.jxsun.devfinder.data.remote.RemoteDataMapper
import com.jxsun.devfinder.data.repository.GitHubUserRepository
import com.jxsun.devfinder.data.repository.Repository
import com.jxsun.devfinder.feature.DevListActionProcessor
import com.jxsun.devfinder.feature.DevListViewModel
import com.jxsun.devfinder.model.GitHubUser
import com.jxsun.devfinder.util.NetworkChecker
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single<GitHubService> { GitHubServiceImpl() }

    single { AppPreferences(androidContext()) }

    single { NetworkChecker(androidContext()) }

    single { LocalDataMapper() }
    single { RemoteDataMapper() }

    single<Repository<GitHubUser>> {
        GitHubUserRepository(
                gitHubService = get(),
                userDao = AppDatabase.getInstance(androidContext()).userDao(),
                preferences = get(),
                localDataMapper = get(),
                remoteDataMapper = get(),
                networkChecker = get()
        )
    }

    single { DevListActionProcessor(get()) }

    viewModel { DevListViewModel(get()) }
}