package com.krystianwsul.checkme.gui.main

import android.content.Intent
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentStatePagerAdapter
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.jakewharton.rxbinding4.view.clicks
import com.krystianwsul.checkme.MyApplication
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.databinding.ActivityTutorialBinding
import com.krystianwsul.checkme.gui.base.NavBarActivity
import com.krystianwsul.checkme.utils.animateVisibility
import com.krystianwsul.checkme.viewmodels.TutorialViewModel
import com.krystianwsul.checkme.viewmodels.getViewModel
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.plusAssign

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

    override val rootView get() = binding.tutorialCoordinator

    override val applyBottomInset = true

    private lateinit var binding: ActivityTutorialBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        help = intent.hasExtra(HELP_KEY)

        if (!help && FirebaseAuth.getInstance().currentUser != null) {
            startMain()
            return
        }

        binding = ActivityTutorialBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.navigationBarColor = ContextCompat.getColor(this, R.color.primaryColor12)

        binding.tutorialPager.adapter = object : FragmentStatePagerAdapter(
                supportFragmentManager,
                BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT
        ) {

            override fun getCount() = 4

            override fun getItem(position: Int) = TutorialFragment.newInstance(position)
        }

        binding.tutorialFab
                .clicks()
                .subscribe {
                    binding.tutorialPager.apply {
                        if (currentItem == adapter!!.count - 1)
                            if (help) finish() else startSignIn()
                        else
                            currentItem += 1
                    }
                }
                .addTo(createDisposable)

        binding.tutorialDots.setupWithViewPager(binding.tutorialPager)

        binding.tutorialSignIn.apply {
            isVisible = if (help) {
                false
            } else {
                createDisposable += clicks().subscribe { startSignIn() }
                true
            }
        }

        tutorialViewModel.state
                .subscribe {
                    when (it) {
                        TutorialViewModel.State.Initial -> binding.apply {
                            animateVisibility(tutorialLayout, tutorialProgress)
                        }
                        TutorialViewModel.State.Progress -> binding.apply {
                            animateVisibility(tutorialProgress, tutorialLayout)
                        }
                        is TutorialViewModel.State.Success -> {
                            setSnackbar { snackbarListener ->
                                snackbarListener.showText(
                                        getString(R.string.signInAs) + " " + it.displayName,
                                        Snackbar.LENGTH_SHORT
                                )
                            }

                            startMain()
                        }
                        TutorialViewModel.State.Error -> Snackbar.make(
                                binding.tutorialCoordinator,
                                R.string.signInFailed,
                                Snackbar.LENGTH_SHORT
                        ).show()
                    }
                }
                .addTo(createDisposable)
    }

    private fun startSignIn() {
        tutorialViewModel.startSignIn()
        startActivityForResult(MyApplication.instance.googleSignInClient.signInIntent, RC_SIGN_IN)
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
