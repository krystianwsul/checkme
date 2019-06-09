package com.krystianwsul.checkme.gui

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentStatePagerAdapter
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.jakewharton.rxbinding3.view.clicks
import com.krystianwsul.checkme.MyApplication
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.utils.animateVisibility
import com.krystianwsul.checkme.viewmodels.TutorialViewModel
import com.krystianwsul.checkme.viewmodels.getViewModel
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.plusAssign
import kotlinx.android.synthetic.main.activity_tutorial.*

class TutorialActivity : NavBarActivity() {

    companion object {

        private const val HELP_KEY = "help"

        private const val RC_SIGN_IN = 1000

        fun newLoginIntent() = Intent(MyApplication.instance, TutorialActivity::class.java)
        fun newHelpIntent() = newLoginIntent().apply { putExtra(HELP_KEY, true) }
    }

    override val tickOnResume = false

    private val tutorialViewModel by lazy { getViewModel<TutorialViewModel>() }

    var help: Boolean = false
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        help = intent.hasExtra(HELP_KEY)

        if (!help && FirebaseAuth.getInstance().currentUser != null) {
            startMain()
            return
        }

        setContentView(R.layout.activity_tutorial)

        window.navigationBarColor = ContextCompat.getColor(this, R.color.primaryColor12Solid)

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
                            setSnackbar(object : SnackbarData {

                                override fun show(snackbarListener: SnackbarListener) {
                                    snackbarListener.showText(getString(R.string.signInAs) + " " + it.displayName, Snackbar.LENGTH_SHORT)
                                }
                            })

                            startMain()
                        }
                        TutorialViewModel.State.Error -> Snackbar.make(tutorialCoordinator, R.string.signInFailed, Snackbar.LENGTH_SHORT).show()
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
