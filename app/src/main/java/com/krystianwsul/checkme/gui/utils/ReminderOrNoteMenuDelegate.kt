package com.krystianwsul.checkme.gui.utils

import android.app.Activity
import android.content.Context
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.gui.edit.EditActivity
import com.krystianwsul.checkme.gui.edit.EditParameters
import com.krystianwsul.common.utils.ProjectKey
import kotlinx.parcelize.Parcelize

@Parcelize
class ReminderOrNoteMenuDelegate(
    private val hint: EditActivity.Hint?,
    private val defaultLabelTask: Boolean = false,
    private val noteProjectKey: ProjectKey.Shared? = null,
) : BottomFabMenuDelegate.MenuDelegate {

    override fun getItems(): List<BottomFabMenuDelegate.MenuDelegate.Item> {
        return listOf(
            object : BottomFabMenuDelegate.MenuDelegate.Item {

                override fun getText(context: Context) = context.getString(R.string.addNote)

                override fun onClick(activity: Activity) {
                    activity.startActivity(
                        EditActivity.getParametersIntent(
                            EditParameters.Create(
                                hint = noteProjectKey?.let(EditActivity.Hint::Project),
                                showFirstSchedule = false,
                            )
                        )
                    )
                }
            },
            object : BottomFabMenuDelegate.MenuDelegate.Item {

                override fun getText(context: Context) =
                    context.getString(if (defaultLabelTask) R.string.add_task else R.string.addReminder)

                override fun onClick(activity: Activity) {
                    activity.startActivity(EditActivity.getParametersIntent(EditParameters.Create(hint)))
                }
            },
        )
    }
}