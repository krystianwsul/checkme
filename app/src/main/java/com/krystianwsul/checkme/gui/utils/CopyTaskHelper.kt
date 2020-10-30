package com.krystianwsul.checkme.gui.utils

import android.content.Intent
import com.krystianwsul.checkme.gui.edit.EditActivity
import com.krystianwsul.checkme.gui.edit.EditParameters
import com.krystianwsul.checkme.gui.tasks.ShowTasksActivity
import com.krystianwsul.common.utils.TaskKey

fun getCopyTasksIntent(taskKeys: List<TaskKey>): Intent {
    check(taskKeys.isNotEmpty())

    return if (taskKeys.size > 1)
        ShowTasksActivity.newIntent(ShowTasksActivity.Parameters.Copy(taskKeys))
    else
        EditActivity.getParametersIntent(EditParameters.Copy(taskKeys.single()))
}