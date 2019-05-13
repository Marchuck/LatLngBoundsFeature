package com.marchuck.latlngboundsfeature.domain

import android.content.res.Resources

class AndroidResourceRepository(val resources: Resources) : ResourceRepository {

    override fun getString(key: Int): String {
        return resources.getString(key)
    }
}