package com.krystianwsul.checkme.gui.main

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.material.snackbar.Snackbar
import com.krystianwsul.checkme.MyApplication
import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.Preferences
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.databinding.SettingsActivityBinding
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.extensions.updateDefaultReminder
import com.krystianwsul.checkme.domainmodel.extensions.updateDefaultTab
import com.krystianwsul.checkme.domainmodel.extensions.updatePhotoUrl
import com.krystianwsul.checkme.gui.base.NavBarActivity
import com.krystianwsul.checkme.persistencemodel.SaveService
import com.krystianwsul.checkme.utils.animateVisibility
import com.krystianwsul.checkme.viewmodels.SettingsViewModel
import com.krystianwsul.checkme.viewmodels.getViewModel
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.plusAssign

class SettingsActivity : NavBarActivity() {

    companion object {

        fun newIntent() = Intent(MyApplication.instance, SettingsActivity::class.java)
    }

    private val settingsViewModel by lazy { getViewModel<SettingsViewModel>() }

    override val rootView get() = binding.settingsRoot

    private lateinit var binding: SettingsActivityBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = SettingsActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.settingsToolbarInclude.toolbar)

        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        supportFragmentManager.apply {
            if (findFragmentById(R.id.settingsFrame) == null)
                beginTransaction().replace(R.id.settingsFrame, SettingsFragment()).commit()
        }

        settingsViewModel.apply {
            start()

            createDisposable += data.subscribe {
                binding.run { animateVisibility(settingsFrame, settingsProgress, immediate = it.immediate) }
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        check(item.itemId == android.R.id.home)

        finish()

        return true
    }

    private fun updateFromAccount(googleSignInAccount: GoogleSignInAccount) {
        googleSignInAccount.photoUrl?.let { url ->
            DomainFactory.addFirebaseListener { it.updatePhotoUrl(SaveService.Source.GUI, url.toString()) }
        }

        Snackbar.make(binding.settingsRoot, R.string.profileUpdated, Snackbar.LENGTH_SHORT).show()
    }

    class SettingsFragment : PreferenceFragmentCompat() {

        companion object {

            private const val RC_SIGN_IN = 1000
        }

        private val settingsActivity get() = (activity as SettingsActivity)

        private val createDisposable = CompositeDisposable()

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)

            settingsActivity.settingsViewModel
                    .relay
                    .subscribe {
                        if (it.value != null)
                            settingsActivity.updateFromAccount(it.value)
                        else
                            startActivityForResult(MyApplication.instance.googleSignInClient.signInIntent, RC_SIGN_IN)
                    }
                    .addTo(createDisposable)

            findPreference<Preference>(getString(R.string.accountDetails))!!.setOnPreferenceClickListener {
                settingsActivity.settingsViewModel.silentSignIn()

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
                    val newTab = when (newValue as String) {
                        getString(R.string.instances) -> MainActivity.Tab.INSTANCES
                        getString(R.string.tasks) -> MainActivity.Tab.TASKS
                        else -> throw IllegalArgumentException()
                    }.ordinal

                    Preferences.tab = newTab
                    DomainFactory.instance.updateDefaultTab(SaveService.Source.GUI, newTab)

                    true
                }
            }

            val defaultReminderPreference = findPreference<SwitchPreferenceCompat>(getString(R.string.defaultReminder))!!

            settingsActivity.settingsViewModel
                    .data
                    .subscribe {
                        defaultReminderPreference.apply {
                            isChecked = it.defaultReminder

                            setOnPreferenceChangeListener { _, newValue ->
                                DomainFactory.instance.updateDefaultReminder(
                                        it.dataId,
                                        SaveService.Source.GUI,
                                        newValue as Boolean
                                )

                                true
                            }
                        }
                    }
                    .addTo(createDisposable)

            findPreference<ListPreference>(getString(R.string.notifications))!!.apply {
                value = getString(when (Preferences.notificationLevel) {
                    Preferences.NotificationLevel.HIGH -> R.string.highPriorityNotifications
                    Preferences.NotificationLevel.MEDIUM -> R.string.mediumPriorityNotifications
                    Preferences.NotificationLevel.NONE -> R.string.noNotifications
                })

                setOnPreferenceChangeListener { _, newValue ->
                    Preferences.notificationLevel = when (newValue as String) {
                        getString(R.string.highPriorityNotifications) -> Preferences.NotificationLevel.HIGH
                        getString(R.string.mediumPriorityNotifications) -> Preferences.NotificationLevel.MEDIUM
                        getString(R.string.noNotifications) -> Preferences.NotificationLevel.NONE
                        else -> throw IllegalArgumentException()
                    }

                    true
                }
            }
        }

        override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
            super.onActivityResult(requestCode, resultCode, data)

            check(requestCode == RC_SIGN_IN)

            val result = Auth.GoogleSignInApi.getSignInResultFromIntent(data)!!
            val account = result.signInAccount

            if (account != null)
                settingsActivity.updateFromAccount(account)
            else
                MyCrashlytics.logException(SettingsSignInException(result.status.toString()))
        }

        override fun onDestroy() {
            createDisposable.dispose()

            super.onDestroy()
        }
    }

    private class SettingsSignInException(message: String) : Exception(message)
}