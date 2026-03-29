package com.rsl.dictionary.utilities.data

import timber.log.Timber

object DecodingErrorLogger {
    fun log(error: Throwable, context: String) {
        Timber.e(error, "Decoding error in %s", context)
    }
}
