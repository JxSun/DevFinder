package com.jxsun.devfinder.data.local

import com.jxsun.devfinder.data.local.database.GitHubUserEntity
import com.jxsun.devfinder.data.repository.Mapper
import com.jxsun.devfinder.model.GitHubUser

class LocalDataMapper : Mapper<GitHubUserEntity> {

    override fun toModel(implData: GitHubUserEntity): GitHubUser {
        return GitHubUser(
                id = implData.id,
                loginName = implData.loginName,
                avatarUrl = implData.avatarUrl
        )
    }

    override fun fromModel(model: GitHubUser): GitHubUserEntity {
        return GitHubUserEntity(
                id = model.id,
                loginName = model.loginName,
                avatarUrl = model.avatarUrl
        )
    }
}