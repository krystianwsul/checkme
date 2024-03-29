package com.krystianwsul.checkme.gui.tree.delegates.invisible_checkbox

import android.view.View
import android.widget.FrameLayout
import com.krystianwsul.checkme.gui.tree.BaseHolder

interface InvisibleCheckboxHolder : BaseHolder {

    val rowCheckBoxFrame: FrameLayout // delegate rename
    val rowMarginStart: View
}