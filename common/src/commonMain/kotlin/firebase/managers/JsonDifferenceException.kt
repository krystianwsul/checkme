package com.krystianwsul.common.firebase.managers

import com.krystianwsul.common.ErrorLogger

class JsonDifferenceException private constructor(oldJson: Any?, newJson: Any?) :
        Exception("difference found, old: $oldJson, new: $newJson") {

    companion object {

        var throwIfDifferent = true

        fun <T : Any> compare(oldJson: T?, newJson: T?) {
            if (oldJson != newJson) {
                val jsonDifferenceException = JsonDifferenceException(oldJson, newJson)

                if (throwIfDifferent) {
                    throw jsonDifferenceException
                } else {
                    ErrorLogger.instance.logException(jsonDifferenceException)
                }
            }
        }
    }
}