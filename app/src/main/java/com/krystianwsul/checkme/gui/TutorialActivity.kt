package com.krystianwsul.checkme.gui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.FragmentStatePagerAdapter
import com.google.firebase.auth.FirebaseAuth
import com.jakewharton.rxbinding2.view.clicks
import com.krystianwsul.checkme.MyApplication
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.utils.animateVisibility
import com.krystianwsul.checkme.viewmodels.TutorialViewModel
import com.krystianwsul.checkme.viewmodels.getViewModel
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.plusAssign
import kotlinx.android.synthetic.main.activity_tutorial.*

class TutorialActivity : AbstractActivity() {

    companion object {

        private const val HELP_KEY = "help"

        private const val RC_SIGN_IN = 1000

        fun newLoginIntent() = Intent(MyApplication.instance, TutorialActivity::class.java)
        fun newHelpIntent() = newLoginIntent().apply { putExtra(HELP_KEY, true) }
    }

    private val tutorialViewModel by lazy { getViewModel<TutorialViewModel>() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val help = intent.hasExtra(HELP_KEY)

        if (!help && FirebaseAuth.getInstance().currentUser != null) {
            startMain()
            return
        }

        setContentView(R.layout.activity_tutorial)

        tutorialPager.adapter = object : FragmentStatePagerAdapter(supportFragmentManager) {

            override fun getCount() = 4

            override fun getItem(position: Int) = TutorialFragment.newInstance(position)
        }

        tutorialFab.clicks()
                .subscribe {
                    if (tutorialPager.currentItem == tutorialPager.adapter!!.count - 1) {
                        if (help)
                            finish()
                        else
                            startSignIn()
                    } else {
                        tutorialPager.currentItem += 1
                    }
                }
                .addTo(createDisposable)

        tutorialDots.setupWithViewPager(tutorialPager)

        tutorialSignIn.apply {
            visibility = if (help) {
                View.GONE
            } else {
                createDisposable += clicks().subscribe { startSignIn() }
                View.VISIBLE
            }
        }

        tutorialViewModel.state
                .subscribe {
                    when (it) {
                        TutorialViewModel.State.Initial -> animateVisibility(tutorialLayout, tutorialProgress)
                        TutorialViewModel.State.Progress -> animateVisibility(tutorialProgress, tutorialLayout)
                        is TutorialViewModel.State.Success -> {
                            Toast.makeText(this, getString(R.string.signInAs) + " " + it.displayName, Toast.LENGTH_SHORT).show()

                            startMain()
                        }
                        TutorialViewModel.State.Error -> Toast.makeText(this, R.string.signInFailed, Toast.LENGTH_SHORT).show()
                    }
                }
                .addTo(createDisposable)
    }

    private fun startSignIn() {
        tutorialViewModel.startSignIn()
        startActivityForResult(MyApplication.instance.googleSigninClient.signInIntent, RC_SIGN_IN)
    }

    private fun startMain() {
        startActivity(MainActivity.newIntent())
        finish()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        check(requestCode == RC_SIGN_IN)

        tutorialViewModel.onActivityResult(data)
    }
}
