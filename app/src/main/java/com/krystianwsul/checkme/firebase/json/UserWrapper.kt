package com.krystianwsul.checkme.firebase.json

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
class UserWrapper(val friendOf: MutableMap<String, Boolean> = mutableMapOf(), val userData: UserJson = UserJson())
