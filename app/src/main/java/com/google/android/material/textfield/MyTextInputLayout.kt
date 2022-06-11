package com.google.android.material.textfield

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import com.google.android.material.animation.AnimationUtils
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
    private var first = true

    init {
        clearOnEditTextAttachedListeners()
        clearOnEndIconChangedListeners()
        setEndIconTintList(null)

        setDropdownMode()
    }

    private lateinit var mode: Mode

    private fun getDrawable(@DrawableRes drawableId: Int) = ContextCompat.getDrawable(context, drawableId)!!

    private fun getAlphaAnimator(duration: Int, vararg values: Float): ValueAnimator {
        val animator = ValueAnimator.ofFloat(*values)
        animator.interpolator = AnimationUtils.LINEAR_INTERPOLATOR
        animator.duration = duration.toLong()
        animator.addUpdateListener { animation ->
            val alpha = animation.animatedValue as Float
            endIconView.alpha = alpha
        }
        return animator
    }

    private var previousDrawableRes: Int? = null

    private fun setDrawableRes(@DrawableRes drawableRes: Int) {
        if (previousDrawableRes == drawableRes) return
        previousDrawableRes = drawableRes

        val newEndIcon = getDrawable(drawableRes)

        if (first) {
            check(animators == null)

            endIconDrawable = newEndIcon
        } else {
            val outAnimator = getAlphaAnimator(50, 1f, 0f)
            val inAnimator = getAlphaAnimator(67, 0f, 1f)

            outAnimator.addListener(object : AnimatorListenerAdapter() {

                override fun onAnimationEnd(animation: Animator?) {
                    endIconDrawable = newEndIcon

                    inAnimator.start()
                }
            })

            disposeAnimation()
            animators = Pair(outAnimator, inAnimator)

            outAnimator.start()
        }
    }

    fun setClose(listener: () -> Unit, iconListener: () -> Unit) {
        setListeners(listener, iconListener)
        mode = Mode.Close
        mode.updateIcon(this)
        first = false
    }

    fun setDropdown(listener: () -> Unit) {
        setListeners(listener, listener)
        setDropdownMode()
        first = false
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

        @Suppress("DEPRECATION")
        setErrorIconOnClickListener { iconListener() }

        disallowSettingListener = true
    }

    @Deprecated("")
    override fun setEndIconOnClickListener(endIconOnClickListener: OnClickListener?) {
        if (disallowSettingListener) throw UnsupportedOperationException()

        super.setEndIconOnClickListener(endIconOnClickListener)
    }

    @Deprecated("")
    override fun setErrorIconOnClickListener(errorIconOnClickListener: OnClickListener?) {
        if (disallowSettingListener) throw UnsupportedOperationException()

        super.setErrorIconOnClickListener(errorIconOnClickListener)
    }

    fun setText(text: String) = editText!!.setText(text)

    fun setChecked(isChecked: Boolean) {
        (mode as Mode.Dropdown).isChecked = isChecked
        mode.updateIcon(this)
    }

    private var animators: Pair<Animator, Animator>? = null

    private fun disposeAnimation() {
        animators?.let {
            it.first.cancel()
            it.second.cancel()
        }
        animators = null
    }

    override fun onDetachedFromWindow() {
        disposeAnimation()

        super.onDetachedFromWindow()
    }

    private sealed class Mode {

        abstract fun updateIcon(myTextInputLayout: MyTextInputLayout)

        object Close : Mode() {

            override fun updateIcon(myTextInputLayout: MyTextInputLayout) =
                myTextInputLayout.setDrawableRes(R.drawable.copied_mtrl_ic_cancel)
        }

        class Dropdown(var isChecked: Boolean = false) : Mode() {

            override fun updateIcon(myTextInputLayout: MyTextInputLayout) {
                val icon = if (isChecked)
                    R.drawable.copied_mtrl_ic_arrow_drop_up
                else
                    R.drawable.copied_mtrl_ic_arrow_drop_down

                myTextInputLayout.setDrawableRes(icon)
            }
        }
    }
}