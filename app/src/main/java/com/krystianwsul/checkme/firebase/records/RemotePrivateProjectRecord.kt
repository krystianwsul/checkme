package com.krystianwsul.checkme.firebase.records

import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.UserInfo
import com.krystianwsul.checkme.firebase.json.ProjectJson

class RemotePrivateProjectRecord(
        create: Boolean,
        domainFactory: DomainFactory,
        id: String,
        projectJson: ProjectJson) : RemoteProjectRecord(create, domainFactory, id, projectJson) {

    constructor(domainFactory: DomainFactory, id: String, projectJson: ProjectJson) : this(
            false,
            domainFactory,
            id,
            projectJson)

    constructor(domainFactory: DomainFactory, userInfo: UserInfo, projectJson: ProjectJson) : this(
            true,
            domainFactory,
            userInfo.key,
            projectJson)

    override val createObject get() = createProjectJson

    override val childKey get() = key
}