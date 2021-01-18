package com.krystianwsul.checkme.gui.widgets

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.TransitionDrawable
import android.util.AttributeSet
import android.util.Log
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import com.google.android.material.textfield.TextInputLayout
import com.krystianwsul.checkme.R
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.kotlin.subscribeBy
import java.util.concurrent.TimeUnit


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

    @DrawableRes
    private var previousDrawableRes: Int? = null

    private var animationDisposable: Disposable? = null

    private fun getDrawable(@DrawableRes drawableRes: Int) = getBitmapFromVectorDrawable(drawableRes)

    private fun getBitmapFromVectorDrawable(drawableId: Int): Drawable {
        val drawable = ContextCompat.getDrawable(context, drawableId)!!.apply {
            setTint(ContextCompat.getColor(context, R.color.textInputIcon))
        }

        val bitmap = Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return BitmapDrawable(context.resources, bitmap)
    }

    private val animationTime by lazy { resources.getInteger(android.R.integer.config_shortAnimTime) }

    private fun setDrawableRes(@DrawableRes drawableRes: Int) {
        if (drawableRes == previousDrawableRes) return

        Log.e("asdf", "magic setDrawableRes " + hashCode())

        val oldEndIcon = previousDrawableRes?.let(::getDrawable)
        val newEndIcon = getDrawable(drawableRes)

        if (oldEndIcon == null) {
            check(animationDisposable == null)

            endIconDrawable = newEndIcon
        } else {
            disposeAnimation()

            val transitionDrawable = TransitionDrawable(arrayOf(oldEndIcon, newEndIcon)).apply {
                isCrossFadeEnabled = true
            }

            endIconDrawable = oldEndIcon

            endIconDrawable = transitionDrawable
            transitionDrawable.startTransition(animationTime)

            animationDisposable = Single.just(Unit)
                    .delay(animationTime.toLong(), TimeUnit.MILLISECONDS)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeBy {
                        Log.e("asdf", "magic replacing after anim")
                        endIconDrawable = newEndIcon
                    }
        }

        previousDrawableRes = drawableRes

        errorIconDrawable = ContextCompat.getDrawable(context, drawableRes)
    }

    fun setClose(listener: () -> Unit, iconListener: () -> Unit) {
        Log.e("asdf", "magic setClose")
        setListeners(listener, iconListener)
        mode = Mode.Close
        mode.updateIcon(this)
    }

    fun setDropdown(listener: () -> Unit) {
        Log.e("asdf", "magic setDropdown")
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

    private fun disposeAnimation() {
        animationDisposable?.dispose()
        animationDisposable = null
    }

    override fun onDetachedFromWindow() {
        disposeAnimation()

        super.onDetachedFromWindow()
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