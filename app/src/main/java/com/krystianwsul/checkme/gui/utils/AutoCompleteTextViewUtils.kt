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
    fun setIsChecked(isChecked: Boolean) {
        getTextInputLayout().apply {
            fun setChecked() {
                getEndIconView().isChecked = isChecked
            }

            setChecked()
            postDelayed(::setChecked, 500)
        }
    }

    var isChecked = getTextInputLayout().getEndIconView().isChecked

    setOnClickListener {
        listener()

        isChecked = !isChecked
        setIsChecked(isChecked)
    }

    getTextInputLayout().let { textInputLayout ->
        textInputLayout.setEndIconOnClickListener {
            listener()

            isChecked = !isChecked
            setIsChecked(isChecked)
        }
    }
}

fun AutoCompleteTextView.setFixedOnClickListener(listener: () -> Unit, iconListener: () -> Unit) {
    setOnClickListener { listener() }

    getTextInputLayout().setEndIconOnClickListener { iconListener() }
}