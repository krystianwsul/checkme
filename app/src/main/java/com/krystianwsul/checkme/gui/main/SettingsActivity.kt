package com.krystianwsul.checkme.gui.main

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import androidx.annotation.CheckResult
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.material.snackbar.Snackbar
import com.krystianwsul.checkme.*
import com.krystianwsul.checkme.databinding.SettingsActivityBinding
import com.krystianwsul.checkme.domainmodel.DomainListenerManager
import com.krystianwsul.checkme.domainmodel.extensions.updatePhotoUrl
import com.krystianwsul.checkme.domainmodel.update.AndroidDomainUpdater
import com.krystianwsul.checkme.gui.base.AbstractActivity
import com.krystianwsul.checkme.gui.base.NavBarActivity
import com.krystianwsul.checkme.viewmodels.SettingsViewModel
import com.krystianwsul.checkme.viewmodels.getViewModel
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo

class SettingsActivity : NavBarActivity() {

    companion object {

        fun newIntent() = Intent(MyApplication.instance, SettingsActivity::class.java)
    }

    private val settingsViewModel by lazy { getViewModel<SettingsViewModel>() }

    override val rootView get() = binding.settingsRoot

    private lateinit var binding: SettingsActivityBinding

    override val titleId = R.string.settings

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
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        check(item.itemId == android.R.id.home)

        finish()

        return true
    }

    @CheckResult
    private fun updateFromAccount(googleSignInAccount: GoogleSignInAccount): Completable {
        Snackbar.make(binding.settingsRoot, R.string.profileUpdated, Snackbar.LENGTH_SHORT).show()

        return Maybe.fromCallable<Uri> { googleSignInAccount.photoUrl }.flatMapCompletable {
            AndroidDomainUpdater.updatePhotoUrl(DomainListenerManager.NotificationType.All, it.toString())
        }
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
                    .flatMapCompletable {
                        if (it.value != null) {
                            settingsActivity.updateFromAccount(it.value)
                        } else {
                            @Suppress("Deprecation")
                            startActivityForResult(MyApplication.instance.googleSignInClient.signInIntent, RC_SIGN_IN)

                            Completable.complete()
                        }
                    }
                    .subscribe()
                    .addTo(createDisposable)

            findPreference<Preference>(getString(R.string.accountDetails))!!.setOnPreferenceClickListener {
                settingsActivity.settingsViewModel.silentSignIn()

                true
            }

            findPreference<ListPreference>(getString(R.string.startPage))!!.apply {
                val initialTab = MainActivity.Tab.values()[Preferences.tab]

                value = getString(when (initialTab) {
                    MainActivity.Tab.INSTANCES -> R.string.instances
                    MainActivity.Tab.NOTES -> R.string.notes
                    else -> throw IllegalArgumentException()
                })

                setOnPreferenceChangeListener { _, newValue ->
                    val newTab = when (newValue as String) {
                        getString(R.string.instances) -> MainActivity.Tab.INSTANCES
                        getString(R.string.notes) -> MainActivity.Tab.NOTES
                        else -> throw IllegalArgumentException()
                    }.ordinal

                    Preferences.tab = newTab

                    true
                }
            }

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

            findPreference<Preference>(getString(R.string.tooltips))!!.setOnPreferenceClickListener {
                TooltipManager.Type
                        .values()
                        .forEach { Preferences.setTooltipShown(it, false) }

                Snackbar.make(
                        settingsActivity.binding.settingsRoot,
                        R.string.tooltipsRestarted,
                        Snackbar.LENGTH_SHORT
                ).show()

                true
            }

            findPreference<ListPreference>(getString(R.string.language))!!.apply {
                val initialLanguage = Preferences.language

                value = getString(when (initialLanguage) {
                    Preferences.Language.DEFAULT -> R.string.setAutomatically
                    Preferences.Language.ENGLISH -> R.string.english
                    Preferences.Language.POLISH -> R.string.polish
                })

                setOnPreferenceChangeListener { _, newValue ->
                    val newLanguage = when (newValue as String) {
                        getString(R.string.setAutomatically) -> Preferences.Language.DEFAULT
                        getString(R.string.english) -> Preferences.Language.ENGLISH
                        getString(R.string.polish) -> Preferences.Language.POLISH
                        else -> throw IllegalArgumentException()
                    }

                    Preferences.language = newLanguage

                    newLanguage.applySetting(requireActivity() as AbstractActivity)

                    true
                }
            }
        }

        @Suppress("OVERRIDE_DEPRECATION")
        override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
            @Suppress("Deprecation")
            super.onActivityResult(requestCode, resultCode, data)

            check(requestCode == RC_SIGN_IN)

            val result = Auth.GoogleSignInApi.getSignInResultFromIntent(data!!)!!
            val account = result.signInAccount

            if (account != null) {
                settingsActivity.updateFromAccount(account)
                    .subscribe()
                    .addTo(createDisposable)
            } else {
                MyCrashlytics.logException(SettingsSignInException(result.status.toString()))
            }
        }

        override fun onDestroy() {
            createDisposable.dispose()

            super.onDestroy()
        }
    }

    private class SettingsSignInException(message: String) : Exception(message)
}