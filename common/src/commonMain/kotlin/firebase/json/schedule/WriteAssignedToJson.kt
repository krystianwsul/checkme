package com.krystianwsul.common.firebase.json.schedule


interface WriteAssignedToJson : AssignedToJson {

    override var assignedTo: Map<String, Boolean>
}
