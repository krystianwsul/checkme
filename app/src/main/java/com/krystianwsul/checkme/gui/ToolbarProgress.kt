package com.krystianwsul.checkme.gui

import android.animation.ValueAnimator
import android.content.Context
import android.os.Handler
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.utils.dpToPx
import kotlinx.android.synthetic.main.progress_bar_saving.view.*

class ToolbarProgress @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    init {
        View.inflate(context, R.layout.progress_bar_saving, this)

        elevation = dpToPx(6)

        clippedProgress.apply {
            fun loop(hide: Boolean) {
                Handler().postDelayed(
                        {
                            fun setHeight(height: Int) {
                                layoutParams = layoutParams.also {
                                    it.height = height
                                }
                            }

                            val animation = ValueAnimator.ofInt(height, if (hide) 0 else dpToPx(4).toInt())
                            animation.addUpdateListener {
                                val height = it.animatedValue as Int
                                setHeight(height)
                            }
                            animation.duration = resources.getInteger(android.R.integer.config_mediumAnimTime).toLong()
                            animation.start()

                            loop(!hide)
                        },
                        2000
                )
            }

            loop(true)
        }
    }

}