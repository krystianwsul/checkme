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
        setDropdownIcon()
    }

    private fun setDrawableRes(@DrawableRes drawableRes: Int) {
        endIconDrawable = ContextCompat.getDrawable(context, drawableRes)!!.apply {
            setTint(ContextCompat.getColor(context, R.color.textInputIcon))
        }

        errorIconDrawable = ContextCompat.getDrawable(context, drawableRes)
    }

    fun setClose(listener: () -> Unit, iconListener: () -> Unit) {
        setDrawableRes(R.drawable.mtrl_ic_cancel)
        setListeners(listener, iconListener)
    }

    fun setDropdown(listener: () -> Unit) {
        setDropdownIcon()
        setListeners(listener, listener)
    }

    private fun setDropdownIcon() = setDrawableRes(R.drawable.mtrl_ic_arrow_drop_down)

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
}