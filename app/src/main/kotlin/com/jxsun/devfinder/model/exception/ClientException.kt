package com.jxsun.devfinder.model.exception

data class ClientException(val httpCode: Int)
    : Exception("Client failed to reach server: HTTP $httpCode")