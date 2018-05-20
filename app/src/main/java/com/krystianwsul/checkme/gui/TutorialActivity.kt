package com.krystianwsul.checkme.gui

import android.content.Intent
import android.os.Bundle
import android.support.v4.app.FragmentStatePagerAdapter
import com.jakewharton.rxbinding2.view.clicks
import com.krystianwsul.checkme.MyApplication
import com.krystianwsul.checkme.R
import io.reactivex.rxkotlin.addTo
import kotlinx.android.synthetic.main.activity_tutorial.*

class TutorialActivity : AbstractActivity() {

    companion object {

        fun newIntent() = Intent(MyApplication.instance, TutorialActivity::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
