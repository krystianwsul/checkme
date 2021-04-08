package com.krystianwsul.common.firebase.managers

class JsonDifferenceException(oldJson: Any?, newJson: Any?) : Exception("difference found, old: $oldJson, new: $newJson")