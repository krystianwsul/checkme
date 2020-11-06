package com.krystianwsul.checkme.gui.utils

import android.widget.AutoCompleteTextView
import com.google.android.material.internal.CheckableImageButton
import com.google.android.material.textfield.TextInputLayout
import com.krystianwsul.checkme.utils.getPrivateField


fun AutoCompleteTextView.setFixedOnClickListener(listener: () -> Unit) = setFixedOnClickListener(listener, listener)

private fun AutoCompleteTextView.getTextInputLayout() = parent.parent as TextInputLayout

fun TextInputLayout.getEndIconView(): CheckableImageButton = getPrivateField("endIconView")

fun TextInputLayout.setChecked() {
    getEndIconView().isChecked = true
}

fun AutoCompleteTextView.setFixedOnClickListenerAndFixIcon(listener: () -> Unit) {

    //val isChecked = getTextInputLayout().getEndIconView().isChecked

    setOnClickListener { listener() }

    getTextInputLayout().let { textInputLayout ->
        textInputLayout.setEndIconOnClickListener {
            listener()

            //textInputLayout.getEndIconView().toggle()
        }
    }
}

fun AutoCompleteTextView.setFixedOnClickListener(listener: () -> Unit, iconListener: () -> Unit) {
    setOnClickListener { listener() }

    getTextInputLayout().setEndIconOnClickListener { iconListener() }
}