package com.krystianwsul.checkme.gui.widgets

import android.content.Context
import android.util.AttributeSet
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import com.google.android.material.textfield.TextInputLayout
import com.krystianwsul.checkme.R

class MyTextInputLayout : TextInputLayout {

    @JvmOverloads
    constructor(context: Context, attrs: AttributeSet? = null) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    init {
        @Suppress("DEPRECATION")
        endIconMode = END_ICON_CUSTOM
    }

    private val disallowSettingIcon = true

    init {
        clearOnEditTextAttachedListeners()
        clearOnEndIconChangedListeners()
        setEndIconTintList(null)

        setDropdownMode()
    }

    private lateinit var mode: Mode

    private fun setDrawableRes(@DrawableRes drawableRes: Int) {
        endIconDrawable = ContextCompat.getDrawable(context, drawableRes)!!.apply {
            setTint(ContextCompat.getColor(context, R.color.textInputIcon))
        }

        errorIconDrawable = ContextCompat.getDrawable(context, drawableRes)
    }

    fun setClose(listener: () -> Unit, iconListener: () -> Unit) {
        setListeners(listener, iconListener)
        mode = Mode.Close
        mode.updateIcon(this)
    }

    fun setDropdown(listener: () -> Unit) {
        setListeners(listener, listener)
        setDropdownMode()
    }

    private fun setDropdownMode() {
        mode = Mode.Dropdown()
        mode.updateIcon(this)
    }

    @Deprecated("")
    override fun setEndIconMode(endIconMode: Int) {
        if (disallowSettingIcon) throw UnsupportedOperationException()

        super.setEndIconMode(endIconMode)
    }

    private var disallowSettingListener = true

    private fun setListeners(listener: () -> Unit, iconListener: () -> Unit) {
        check(disallowSettingListener)

        disallowSettingListener = false

        editText!!.setOnClickListener { listener() }
        @Suppress("DEPRECATION")
        setEndIconOnClickListener { iconListener() }

        disallowSettingListener = true
    }

    @Deprecated("")
    override fun setEndIconOnClickListener(endIconOnClickListener: OnClickListener?) {
        if (disallowSettingListener) throw UnsupportedOperationException()

        super.setEndIconOnClickListener(endIconOnClickListener)
    }

    fun setText(text: String) = editText!!.setText(text)

    fun setChecked(isChecked: Boolean) {
        (mode as Mode.Dropdown).let { it.isChecked = isChecked }
        mode.updateIcon(this)
    }

    fun toggleChecked() {
        (mode as Mode.Dropdown).let { it.isChecked = !it.isChecked }
        mode.updateIcon(this)
    }

    private sealed class Mode {

        abstract fun updateIcon(myTextInputLayout: MyTextInputLayout)

        object Close : Mode() {

            override fun updateIcon(myTextInputLayout: MyTextInputLayout) =
                    myTextInputLayout.setDrawableRes(R.drawable.mtrl_ic_cancel)
        }

        class Dropdown(var isChecked: Boolean = false) : Mode() {

            override fun updateIcon(myTextInputLayout: MyTextInputLayout) {
                val icon = if (isChecked)
                    R.drawable.mtrl_ic_arrow_drop_up
                else
                    R.drawable.mtrl_ic_arrow_drop_down

                myTextInputLayout.setDrawableRes(icon)
            }
        }
    }
}