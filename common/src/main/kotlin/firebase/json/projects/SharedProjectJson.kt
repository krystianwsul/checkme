package com.krystianwsul.common.firebase.json.projects

import com.krystianwsul.common.firebase.json.users.UserJson

interface SharedProjectJson : ProjectJson {

    val name: String

    val users: Map<String, UserJson>
}