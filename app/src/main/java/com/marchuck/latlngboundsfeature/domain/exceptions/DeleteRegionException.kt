package com.marchuck.latlngboundsfeature.domain.exceptions

import java.lang.Exception

class DeleteRegionException(val error: String) : Exception() {
}