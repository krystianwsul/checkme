package com.krystianwsul.checkme.gui

import android.content.Intent
import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.krystianwsul.checkme.MyApplication
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.persistencemodel.SaveService
import com.krystianwsul.checkme.utils.toSingle
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

        supportFragmentManager
                .beginTransaction()
                .replace(R.id.settingsFrame, SettingsFragment())
                .commit()
    }

    class SettingsFragment : PreferenceFragmentCompat() {

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)

            findPreference<Preference>(getString(R.string.accountDetails))!!.setOnPreferenceClickListener {
                MyApplication.instance
                        .googleSigninClient
                        .silentSignIn()
                        .toSingle()
                        .subscribe { wrapper ->
                            if (wrapper.value != null) {
                                wrapper.value
                                        .photoUrl
                                        ?.let { url ->
                                            DomainFactory.addFirebaseListener {
                                                it.updatePhotoUrl(SaveService.Source.GUI, url.toString())
                                            }
                                        }
                            } else {
                                // todo login
                            }
                        }

                true
            }
        }
    }
}