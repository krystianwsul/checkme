package com.krystianwsul.checkme.gui.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import androidx.annotation.DrawableRes
import com.jakewharton.rxbinding3.widget.textChanges
import com.krystianwsul.checkme.databinding.ToolbarSearchInnerBinding

class SearchToolbar @JvmOverloads constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int = 0) :
        FrameLayout(context, attrs, defStyleAttr) {

    private val binding =
            ToolbarSearchInnerBinding.inflate(LayoutInflater.from(context), this, true)

    private val inputMethodManager by lazy {
        context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    }

    var text: String?
        get() = binding.searchText
                .text
                .toString()
        set(value) {
            binding.searchText.setText(value)
        }

    val menu get() = binding.searchToolbar.menu

    fun textChanges() = binding.searchText.textChanges()

    fun closeKeyboard() = inputMethodManager.hideSoftInputFromWindow(binding.searchText.windowToken, 0)

    fun showKeyboard() = inputMethodManager.showSoftInput(binding.searchText, InputMethodManager.SHOW_IMPLICIT)

    fun requestSearchFocus() = binding.searchText.requestFocus()

    fun setOnMenuItemClickListener(listener: (Int) -> Unit) = binding.searchToolbar.setOnMenuItemClickListener {
        listener(it.itemId)
        true
    }

    fun setNavigationIcon(@DrawableRes resId: Int) = binding.searchToolbar.setNavigationIcon(resId)

    fun setNavigationOnClickListener(listener: () -> Unit) =
            binding.searchToolbar.setNavigationOnClickListener { listener() }
}