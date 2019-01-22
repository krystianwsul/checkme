package com.krystianwsul.checkme.firebase.records

import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.firebase.DatabaseWrapper
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

    constructor(domainFactory: DomainFactory, projectJson: ProjectJson) : this(
            true,
            domainFactory,
            DatabaseWrapper.getRootRecordId(),
            projectJson)

    override val createObject get() = createProjectJson
}