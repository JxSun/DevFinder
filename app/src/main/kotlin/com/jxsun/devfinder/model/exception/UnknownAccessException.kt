package com.jxsun.devfinder.model.exception

data class UnknownAccessException(val httpCode: Int?)
    : Exception("Failed to access server due to unknown reason: HTTP $httpCode")