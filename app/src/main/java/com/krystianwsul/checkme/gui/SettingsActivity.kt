package com.krystianwsul.checkme.gui

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.material.snackbar.Snackbar
import com.krystianwsul.checkme.MyApplication
import com.krystianwsul.checkme.Preferences
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.persistencemodel.SaveService
import com.krystianwsul.checkme.viewmodels.SettingsViewModel
import com.krystianwsul.checkme.viewmodels.getViewModel
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        check(item.itemId == android.R.id.home)

        finish()

        return true
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

        private val settingsViewModel by lazy { getViewModel<SettingsViewModel>() }

        private val createDisposable = CompositeDisposable()

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)

            settingsViewModel.relay
                    .subscribe {
                        if (it.value != null)
                            settingsActivity.updateFromAccount(it.value)
                        else
                            startActivityForResult(MyApplication.instance.googleSigninClient.signInIntent, RC_SIGN_IN)
                    }
                    .addTo(createDisposable)

            findPreference<Preference>(getString(R.string.accountDetails))!!.setOnPreferenceClickListener {
                settingsViewModel.silentSignIn()

                true
            }

            findPreference<ListPreference>(getString(R.string.startPage))!!.apply {
                val initialTab = MainActivity.Tab.values()[Preferences.tab]

                value = getString(when (initialTab) {
                    MainActivity.Tab.INSTANCES -> R.string.instances
                    MainActivity.Tab.TASKS -> R.string.tasks
                    else -> throw IllegalArgumentException()
                })

                setOnPreferenceChangeListener { _, newValue ->
                    val newTab = when (newValue) {
                        getString(R.string.instances) -> MainActivity.Tab.INSTANCES
                        getString(R.string.tasks) -> MainActivity.Tab.TASKS
                        else -> throw IllegalArgumentException()
                    }

                    Preferences.tab = newTab.ordinal

                    true
                }
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

        override fun onDestroy() {
            createDisposable.dispose()

            super.onDestroy()
        }
    }
}