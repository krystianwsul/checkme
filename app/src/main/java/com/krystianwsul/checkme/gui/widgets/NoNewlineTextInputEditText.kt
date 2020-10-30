package com.krystianwsul.checkme.gui.widgets

import android.content.Context
import android.text.InputFilter
import android.util.AttributeSet
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import com.google.android.material.textfield.TextInputEditText


class NoNewlineTextInputEditText : TextInputEditText {

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    init {
        filters = arrayOf(InputFilter { source, _, _, _, _, _ ->
            if (source?.contains('\n') == true) {
                source.replace("\n".toRegex(), "")
            } else {
                null
            }
        })
    }

    override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection? {
        val connection = super.onCreateInputConnection(outAttrs)

        val imeActions = outAttrs.imeOptions and EditorInfo.IME_MASK_ACTION

        if (imeActions and EditorInfo.IME_ACTION_DONE != 0) {
            outAttrs.imeOptions = outAttrs.imeOptions xor imeActions
            outAttrs.imeOptions = outAttrs.imeOptions or EditorInfo.IME_ACTION_DONE
        }

        if (outAttrs.imeOptions and EditorInfo.IME_FLAG_NO_ENTER_ACTION != 0) {
            outAttrs.imeOptions = outAttrs.imeOptions and EditorInfo.IME_FLAG_NO_ENTER_ACTION.inv()
        }

        return connection
    }
}