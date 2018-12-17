package com.krystianwsul.checkme.gui

import android.content.Intent
import android.os.Bundle
import android.preference.PreferenceManager
import androidx.fragment.app.FragmentStatePagerAdapter
import com.jakewharton.rxbinding2.view.clicks
import com.krystianwsul.checkme.MyApplication
import com.krystianwsul.checkme.R
import io.reactivex.rxkotlin.addTo
import kotlinx.android.synthetic.main.activity_tutorial.*

class TutorialActivity : AbstractActivity() {

    companion object {

        private const val FORCE_KEY = "force"

        private const val TUTORIAL_SHOWN_KEY = "tutorialShown"

        fun newIntent() = Intent(MyApplication.instance, TutorialActivity::class.java).apply { putExtra(FORCE_KEY, true) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!intent.hasExtra(FORCE_KEY)) {
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)

            if (sharedPreferences.getBoolean(TUTORIAL_SHOWN_KEY, false)) {
                startMain()
                return
            } else {
                sharedPreferences.edit()
                        .putBoolean(TUTORIAL_SHOWN_KEY, true)
                        .apply()
            }
        }

        setContentView(R.layout.activity_tutorial)

        tutorialPager.adapter = object : FragmentStatePagerAdapter(supportFragmentManager) {

            override fun getCount() = 4

            override fun getItem(position: Int) = TutorialFragment.newInstance(position)
        }

        tutorialFab.clicks()
                .subscribe {
                    if (tutorialPager.currentItem == tutorialPager.adapter!!.count - 1)
                        startMain()
                    else
                        tutorialPager.currentItem += 1
                }
                .addTo(createDisposable)

        tutorialDots.setupWithViewPager(tutorialPager)
    }

    private fun startMain() {
        startActivity(MainActivity.newIntent())
        finish()
    }
}
