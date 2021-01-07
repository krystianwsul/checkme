package com.krystianwsul.checkme.gui.base

import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.jakewharton.rxrelay3.BehaviorRelay
import com.krystianwsul.checkme.gui.utils.setNavBarTransparency
import com.krystianwsul.checkme.utils.isLandscape
import io.reactivex.rxjava3.kotlin.plusAssign

abstract class NavBarActivity : AbstractActivity() {

    protected abstract val rootView: View

    protected open val applyBottomInset = false

    protected var bottomInset = 0
        private set

    protected val keyboardInsetRelay = BehaviorRelay.create<Int>()

    override fun setContentView(view: View) {
        super.setContentView(view)

        val insetsRelay = BehaviorRelay.create<WindowInsetsCompat>()

        ViewCompat.setOnApplyWindowInsetsListener(rootView) { _, windowInsetsCompat ->
            insetsRelay.accept(windowInsetsCompat)

            val insets = windowInsetsCompat.getInsets(WindowInsetsCompat.Type.systemBars())

            val keyboardInsets = windowInsetsCompat.getInsets(WindowInsetsCompat.Type.ime())
            keyboardInsetRelay.accept(keyboardInsets.bottom)

            bottomInset = insets.bottom

            rootView.setPadding(
                    insets.left,
                    insets.top,
                    insets.right,
                    if (applyBottomInset) insets.bottom else 0
            )

            WindowInsetsCompat.CONSUMED
        }

        setNavBarTransparency(window, rootView, resources.isLandscape) { callback ->
            createDisposable += insetsRelay.subscribe { callback(it) }
        }
    }
}