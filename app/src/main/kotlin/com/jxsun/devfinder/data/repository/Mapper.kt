package com.jxsun.devfinder.data.repository

import com.jxsun.devfinder.model.GitHubUser

interface Mapper<I> {

    fun toModel(implData: I): GitHubUser

    fun fromModel(model: GitHubUser): I
}