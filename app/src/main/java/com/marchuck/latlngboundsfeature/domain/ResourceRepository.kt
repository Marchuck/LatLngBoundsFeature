package com.marchuck.latlngboundsfeature.domain

interface ResourceRepository {
    fun getString(key: Int): String
}