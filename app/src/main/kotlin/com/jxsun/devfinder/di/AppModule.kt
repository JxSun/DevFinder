package com.jxsun.devfinder.di

import com.jxsun.devfinder.data.repository.GitHubUserRepository
import com.jxsun.devfinder.data.repository.Repository
import com.jxsun.devfinder.data.source.local.AppPreferences
import com.jxsun.devfinder.data.source.local.LocalDataMapper
import com.jxsun.devfinder.data.source.local.LocalDataSource
import com.jxsun.devfinder.data.source.local.database.AppDatabase
import com.jxsun.devfinder.data.source.remote.GitHubService
import com.jxsun.devfinder.data.source.remote.RemoteDataMapper
import com.jxsun.devfinder.data.source.remote.RemoteDataSource
import com.jxsun.devfinder.feature.DevListActionProcessor
import com.jxsun.devfinder.feature.DevListViewModel
import com.jxsun.devfinder.model.GitHubUser
import com.jxsun.devfinder.util.NetworkChecker
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single { AppPreferences(androidContext()) }

    single { NetworkChecker(androidContext()) }

    single { LocalDataMapper() }
    single { RemoteDataMapper() }

    single {
        LocalDataSource(
                preferences = get(),
                userDao = AppDatabase.getInstance(androidContext()).userDao(),
                localDataMapper = get()
        )
    }
    single {
        RemoteDataSource(
                gitHubService = GitHubService.Factory().create(),
                remoteDataMapper = get(),
                networkChecker = get()
        )
    }

    single<Repository<GitHubUser>> {
        GitHubUserRepository(
                remoteDataSource = get(),
                localDataSource = get()
        )
    }

    single { DevListActionProcessor(get()) }

    viewModel { DevListViewModel(get()) }
}