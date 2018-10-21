package com.krystianwsul.checkme.gui.projects

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.LoaderManager
import android.support.v4.content.Loader
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import android.view.View
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.gui.AbstractActivity
import com.krystianwsul.checkme.gui.DiscardDialogFragment
import com.krystianwsul.checkme.gui.friends.UserListFragment
import com.krystianwsul.checkme.loaders.ShowProjectLoader

import kotlinx.android.synthetic.main.activity_show_project.*
import kotlinx.android.synthetic.main.toolbar_edit_text.*

class ShowProjectActivity : AbstractActivity(), LoaderManager.LoaderCallbacks<ShowProjectLoader.DomainData> {

    companion object {

        private const val PROJECT_ID_KEY = "projectId"

        private const val DISCARD_TAG = "discard"

        fun newIntent(context: Context, projectId: String) = Intent(context, ShowProjectActivity::class.java).apply {
            check(!TextUtils.isEmpty(projectId))

            putExtra(PROJECT_ID_KEY, projectId)
        }

        fun newIntent(context: Context) = Intent(context, ShowProjectActivity::class.java)
    }

    private lateinit var projectId: String

    private var data: ShowProjectLoader.DomainData? = null

    private lateinit var userListFragment: UserListFragment

    private var savedInstanceState: Bundle? = null

    private val discardDialogListener = this::finish

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_save, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        menu.findItem(R.id.action_save).isVisible = data != null
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_save -> {
                check(data != null)

                if (updateError())
                    return true

                @Suppress("DEPRECATION")
                supportLoaderManager.destroyLoader(0)

                userListFragment.save(toolbarEditText.text.toString())

                finish()
            }
            android.R.id.home -> {
                if (tryClose())
                    finish()
            }
            else -> throw UnsupportedOperationException()
        }
        return true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_show_project)

        this.savedInstanceState = savedInstanceState

        setSupportActionBar(toolbar)

        supportActionBar!!.run {
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.ic_close_white_24dp)
        }

        toolbarEditText.addTextChangedListener(object : TextWatcher {

            private var skip = true

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) = Unit

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) = Unit

            override fun afterTextChanged(s: Editable) {
                if (skip) {
                    skip = false
                    return
                }

                updateError()
            }
        })

        if (intent.hasExtra(PROJECT_ID_KEY)) {
            projectId = intent.getStringExtra(PROJECT_ID_KEY)
            check(!TextUtils.isEmpty(projectId))
        }

        userListFragment = supportFragmentManager.findFragmentById(R.id.show_project_frame) as? UserListFragment ?: UserListFragment.newInstance().also {
            supportFragmentManager
                    .beginTransaction()
                    .add(R.id.show_project_frame, it)
                    .commit()
        }

        userListFragment.setFab(showProjectFab)

        (supportFragmentManager.findFragmentByTag(DISCARD_TAG) as? DiscardDialogFragment)?.discardDialogListener = discardDialogListener

        @Suppress("DEPRECATION")
        supportLoaderManager.initLoader(0, null, this)
    }

    override fun onCreateLoader(id: Int, args: Bundle?) = ShowProjectLoader(this, projectId)

    override fun onLoadFinished(loader: Loader<ShowProjectLoader.DomainData>, data: ShowProjectLoader.DomainData?) {
        this.data = data

        if (savedInstanceState == null) {
            toolbarEditText.setText(data!!.name)
        } else {
            savedInstanceState = null
        }

        toolbarLayout.visibility = View.VISIBLE
        toolbarLayout.isHintAnimationEnabled = true

        invalidateOptionsMenu()

        userListFragment.initialize(projectId, data!!)
    }

    override fun onLoaderReset(loader: Loader<ShowProjectLoader.DomainData>) = Unit

    override fun onBackPressed() {
        if (tryClose())
            super.onBackPressed()
    }

    private fun tryClose(): Boolean {
        return if (dataChanged()) {
            val discardDialogFragment = DiscardDialogFragment.newInstance()
            discardDialogFragment.discardDialogListener = discardDialogListener
            discardDialogFragment.show(supportFragmentManager, DISCARD_TAG)

            false
        } else {
            true
        }
    }

    private fun dataChanged(): Boolean {
        if (data == null)
            return false

        if (TextUtils.isEmpty(toolbarEditText.text) != TextUtils.isEmpty(data!!.name))
            return true

        return if (!TextUtils.isEmpty(toolbarEditText.text) && toolbarEditText.text.toString() != data!!.name) true else userListFragment.dataChanged()
    }

    private fun updateError(): Boolean {
        check(data != null)

        return if (TextUtils.isEmpty(toolbarEditText.text)) {
            toolbarLayout.error = getString(R.string.nameError)

            true
        } else {
            toolbarLayout.error = null

            false
        }
    }
}
