package com.krystianwsul.checkme.gui.widgets

import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.utils.dpToPx
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo

class ToolbarProgress @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    init {
        View.inflate(context, R.layout.progress_bar_saving, this)

        elevation = dpToPx(6)
    }

    private val compositeDisposable = CompositeDisposable()

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        DomainFactory.isSaved
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { showProgress ->
                    fun setHeight(height: Int) {
                        layoutParams = layoutParams.also {
                            it.height = height
                        }
                    }

                    val animation = ValueAnimator.ofInt(height, if (showProgress) dpToPx(4).toInt() else 0)
                    animation.addUpdateListener {
                        val height = it.animatedValue as Int
                        setHeight(height)
                    }
                    animation.duration = resources.getInteger(android.R.integer.config_mediumAnimTime).toLong()
                    animation.start()
                }
                .addTo(compositeDisposable)
    }

    override fun onDetachedFromWindow() {
        compositeDisposable.clear()

        super.onDetachedFromWindow()
    }
}