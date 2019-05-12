package com.krystianwsul.checkme.gui

import android.content.Intent
import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import com.krystianwsul.checkme.MyApplication
import com.krystianwsul.checkme.R
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
        }
    }
}