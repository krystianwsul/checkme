package com.krystianwsul.checkme.gui

import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.jakewharton.rxrelay2.BehaviorRelay
import com.krystianwsul.checkme.gui.utils.setNavBarTransparency
import com.krystianwsul.checkme.utils.isLandscape
import io.reactivex.rxkotlin.plusAssign

abstract class NavBarActivity : AbstractActivity() {

    protected abstract val rootView: View

    protected open val applyBottomInset = false

    override fun setContentView(layoutResID: Int) {
        super.setContentView(layoutResID)

        val insetsRelay = BehaviorRelay.create<WindowInsetsCompat>()

        ViewCompat.setOnApplyWindowInsetsListener(rootView) { _, windowInsetsCompat ->
            insetsRelay.accept(windowInsetsCompat)

            val insets = windowInsetsCompat.getInsets(WindowInsetsCompat.Type.systemBars())

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