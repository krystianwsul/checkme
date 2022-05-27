package com.krystianwsul.checkme.firebase.projects

import com.krystianwsul.checkme.firebase.loaders.ProjectProvider
import com.krystianwsul.common.firebase.json.Parsable
import com.krystianwsul.common.firebase.records.project.ProjectRecord
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.ProjectType

interface ProjectsManager<TYPE : ProjectType, PARSABLE : Parsable, RECORD : ProjectRecord<TYPE>> :
    ProjectProvider.ProjectManager<TYPE, PARSABLE, RECORD> {

    fun remove(projectKey: ProjectKey<TYPE>)

    fun newProjectRecord(parsable: PARSABLE): RECORD
}