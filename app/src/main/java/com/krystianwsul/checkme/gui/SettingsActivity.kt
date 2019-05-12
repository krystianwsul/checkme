package com.krystianwsul.checkme.gui

import android.content.Intent
import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.material.snackbar.Snackbar
import com.krystianwsul.checkme.MyApplication
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.persistencemodel.SaveService
import com.krystianwsul.checkme.utils.toSingle
import kotlinx.android.synthetic.main.settings_activity.*
import kotlinx.android.synthetic.main.toolbar.*

class SettingsActivity : AbstractActivity() {

    companion object {

        fun newIntent() = Intent(MyApplication.instance, SettingsActivity::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)

        setSupportActionBar(toolbar)

        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        supportFragmentManager.apply {
            if (findFragmentById(R.id.settingsFrame) == null)
                beginTransaction().replace(R.id.settingsFrame, SettingsFragment()).commit()
        }
    }

    private fun updateFromAccount(googleSignInAccount: GoogleSignInAccount) {
        googleSignInAccount.photoUrl?.let { url ->
            DomainFactory.addFirebaseListener {
                it.updatePhotoUrl(SaveService.Source.GUI, url.toString())
            }
        }

        Snackbar.make(settingsRoot, R.string.profileUpdated, Snackbar.LENGTH_SHORT).show()
    }

    class SettingsFragment : PreferenceFragmentCompat() {

        companion object {

            private const val RC_SIGN_IN = 1000
        }

        private val settingsActivity get() = (activity as SettingsActivity)

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)

            findPreference<Preference>(getString(R.string.accountDetails))!!.setOnPreferenceClickListener {
                MyApplication.instance
                        .googleSigninClient
                        .silentSignIn()
                        .toSingle()
                        .subscribe { wrapper ->
                            if (wrapper.value != null)
                                settingsActivity.updateFromAccount(wrapper.value)
                            else
                                startActivityForResult(MyApplication.instance.googleSigninClient.signInIntent, RC_SIGN_IN)
                        }

                true
            }
        }

        override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
            super.onActivityResult(requestCode, resultCode, data)

            check(requestCode == RC_SIGN_IN)

            Auth.GoogleSignInApi
                    .getSignInResultFromIntent(data)!!
                    .signInAccount
                    ?.let { settingsActivity.updateFromAccount(it) }
        }
    }
}