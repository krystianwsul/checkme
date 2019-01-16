package com.krystianwsul.checkme.gui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.fragment.app.FragmentStatePagerAdapter
import com.google.android.gms.auth.api.Auth
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.jakewharton.rxbinding2.view.clicks
import com.krystianwsul.checkme.MyApplication
import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.R
import io.reactivex.rxkotlin.addTo
import kotlinx.android.synthetic.main.activity_tutorial.*

class TutorialActivity : AbstractActivity() {

    companion object {

        private const val HELP_KEY = "help"

        private const val RC_SIGN_IN = 1000

        fun newLoginIntent() = Intent(MyApplication.instance, TutorialActivity::class.java)
        fun newHelpIntent() = newLoginIntent().apply { putExtra(HELP_KEY, true) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!intent.hasExtra(HELP_KEY) && FirebaseAuth.getInstance().currentUser != null) {
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
                        startSignIn()
                    } else {
                        tutorialPager.currentItem += 1
                    }
                }
                .addTo(createDisposable)

        tutorialDots.setupWithViewPager(tutorialPager)

        tutorialSignIn.clicks()
                .subscribe { startSignIn() }
                .addTo(createDisposable)
    }

    private fun startSignIn() = startActivityForResult(MyApplication.instance.googleSigninClient.signInIntent, RC_SIGN_IN)

    private fun startMain() {
        startActivity(MainActivity.newIntent())
        finish()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val googleSignInResult = Auth.GoogleSignInApi.getSignInResultFromIntent(data)!!

            if (googleSignInResult.isSuccess) {
                val googleSignInAccount = googleSignInResult.signInAccount!!

                val credential = GoogleAuthProvider.getCredential(googleSignInAccount.idToken, null)

                FirebaseAuth.getInstance()
                        .signInWithCredential(credential)
                        .addOnCompleteListener {
                            if (it.isSuccessful) {
                                Toast.makeText(this, getString(R.string.signInAs) + " " + it.result!!.user.displayName, Toast.LENGTH_SHORT).show()

                                startMain()
                            } else {
                                val exception = it.exception!!

                                Toast.makeText(this, R.string.signInFailed, Toast.LENGTH_SHORT).show()

                                MyCrashlytics.logException(exception)

                                MyApplication.instance.googleSigninClient.signOut()
                            }
                        }
            } else {
                val message = "google signin error: $googleSignInResult"

                Log.e("asdf", message)

                Toast.makeText(this, R.string.signInFailed, Toast.LENGTH_SHORT).show()

                MyCrashlytics.logException(GoogleSignInException("isSuccess: " + googleSignInResult.isSuccess + ", status: " + googleSignInResult.status))
            }
        }
    }

    private class GoogleSignInException(message: String) : Exception(message)
}
