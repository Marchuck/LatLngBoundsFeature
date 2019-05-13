package com.marchuck.latlngboundsfeature.domain

interface ResourceRepository {

    fun getString(key: Int): String

    fun getString(key: Int, vararg args: Any): String
}