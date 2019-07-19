package com.jxsun.devfinder.data.remote

import com.jxsun.devfinder.data.repository.Mapper
import com.jxsun.devfinder.model.GitHubUser

class RemoteDataMapper : Mapper<UserResponse> {

    override fun toModel(implData: UserResponse): GitHubUser {
        return GitHubUser(
                id = implData.id,
                loginName = implData.name,
                avatarUrl = implData.avatarUrl
        )
    }

    override fun fromModel(model: GitHubUser): UserResponse {
        return UserResponse(
                id = model.id,
                name = model.loginName,
                avatarUrl = model.avatarUrl
        )
    }
}