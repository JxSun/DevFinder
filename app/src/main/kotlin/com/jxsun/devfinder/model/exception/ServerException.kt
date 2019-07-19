package com.jxsun.devfinder.model.exception

data class ServerException(val httpCode: Int)
    : Exception("Service has some problems internally: HTTP $httpCode")