package com.krystianwsul.checkme.gui.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.widget.Toolbar
import com.jakewharton.rxbinding3.widget.textChanges
import com.krystianwsul.checkme.databinding.ToolbarSearchInnerBinding

class SearchToolbar : Toolbar {

    @JvmOverloads
    constructor(context: Context, attrs: AttributeSet? = null) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    private val binding =
            ToolbarSearchInnerBinding.inflate(LayoutInflater.from(context), this, true)

    private val inputMethodManager by lazy {
        context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    }

    var text: String?
        get() = binding.searchToolbarText
                .text
                .toString()
        set(value) {
            binding.searchToolbarText.setText(value)
        }

    fun textChanges() = binding.searchToolbarText.textChanges()

    fun closeKeyboard() = inputMethodManager.hideSoftInputFromWindow(binding.searchToolbarText.windowToken, 0)

    fun showKeyboard() = inputMethodManager.showSoftInput(binding.searchToolbarText, InputMethodManager.SHOW_IMPLICIT)

    fun requestSearchFocus() = binding.searchToolbarText.requestFocus()
}