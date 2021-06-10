package com.krystianwsul.checkme.gui.utils

import android.app.Activity
import android.content.Context
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.gui.edit.EditActivity
import com.krystianwsul.checkme.gui.edit.EditParameters
import kotlinx.parcelize.Parcelize

@Parcelize
class ReminderOrNoteMenuDelegate(private val hint: EditActivity.Hint?) : BottomFabMenuDelegate.MenuDelegate {

    override fun getItems(): List<BottomFabMenuDelegate.MenuDelegate.Item> {
        return listOf(
            object : BottomFabMenuDelegate.MenuDelegate.Item {

                override fun getText(context: Context) = context.getString(R.string.addNote)

                override fun onClick(activity: Activity) {
                    activity.startActivity(
                        EditActivity.getParametersIntent(
                            EditParameters.Create(
                                hint = hint.takeIf { it is EditActivity.Hint.Project },
                                showFirstSchedule = false,
                            )
                        )
                    )
                }
            },
            object : BottomFabMenuDelegate.MenuDelegate.Item {

                override fun getText(context: Context) = context.getString(R.string.addReminder)

                override fun onClick(activity: Activity) {
                    activity.startActivity(EditActivity.getParametersIntent(EditParameters.Create(hint)))
                }
            },
        )
    }
}