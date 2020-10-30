package com.krystianwsul.checkme.gui.base

import androidx.appcompat.view.ActionMode

interface ActionModeListener {

    fun onCreateActionMode(actionMode: ActionMode)

    fun onDestroyActionMode()
}