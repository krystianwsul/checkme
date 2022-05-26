package com.krystianwsul.common.firebase.models.project

import com.krystianwsul.common.firebase.records.project.ForeignProjectRecord
import com.krystianwsul.common.utils.ProjectType

interface ForeignProject<T : ProjectType> : Project<T> {

    override val projectRecord: ForeignProjectRecord<T>
}
