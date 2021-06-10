package com.krystianwsul.checkme.gui.projects

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import android.view.View
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.databinding.ActivityShowProjectBinding
import com.krystianwsul.checkme.databinding.BottomBinding
import com.krystianwsul.checkme.gui.base.AbstractActivity
import com.krystianwsul.checkme.gui.dialogs.ConfirmDialogFragment
import com.krystianwsul.checkme.gui.friends.UserListFragment
import com.krystianwsul.checkme.gui.utils.BottomFabMenuDelegate
import com.krystianwsul.checkme.utils.tryGetFragment
import com.krystianwsul.checkme.viewmodels.ShowProjectViewModel
import com.krystianwsul.checkme.viewmodels.getViewModel
import com.krystianwsul.common.utils.ProjectKey
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.plusAssign

class ShowProjectActivity : AbstractActivity(), UserListFragment.UserListListener {

    companion object {

        private const val PROJECT_ID_KEY = "projectId"

        private const val DISCARD_TAG = "discard"

        private const val KEY_BOTTOM_FAB_MENU_DELEGATE_STATE = "bottomFabMenuDelegateState"

        fun newIntent(
            context: Context,
            projectId: ProjectKey.Shared,
        ) = Intent(context, ShowProjectActivity::class.java).apply {
            putExtra(PROJECT_ID_KEY, projectId as Parcelable)
        }

        fun newIntent(context: Context) = Intent(context, ShowProjectActivity::class.java)
    }

    private var projectId: ProjectKey.Shared? = null

    private var data: ShowProjectViewModel.Data? = null

    private lateinit var userListFragment: UserListFragment

    private var savedInstanceState: Bundle? = null

    private val discardDialogListener: (Parcelable?) -> Unit = { finish() }

    private lateinit var showProjectViewModel: ShowProjectViewModel

    private var selectAllVisible = false

    private lateinit var binding: ActivityShowProjectBinding
    private lateinit var bottomBinding: BottomBinding

    private lateinit var bottomFabMenuDelegate: BottomFabMenuDelegate

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

                showProjectViewModel.stop()

                userListFragment.save(binding.showProjectToolbarEditTextInclude.toolbarEditText.text.toString())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe { finish() }
                        .addTo(createDisposable)
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

        binding = ActivityShowProjectBinding.inflate(layoutInflater)
        bottomBinding = BottomBinding.bind(binding.root)
        setContentView(binding.root)

        bottomFabMenuDelegate = BottomFabMenuDelegate(
            bottomBinding,
            binding.showProjectCoordinator,
            this,
            savedInstanceState?.getParcelable(KEY_BOTTOM_FAB_MENU_DELEGATE_STATE),
        )

        this.savedInstanceState = savedInstanceState

        setSupportActionBar(binding.showProjectToolbarEditTextInclude.toolbar)

        supportActionBar!!.run {
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.ic_close_white_24dp)
        }

        initBottomBar()

        binding.showProjectToolbarEditTextInclude
                .toolbarEditText
                .addTextChangedListener(object : TextWatcher {

                    private var skip = true

                    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) = Unit

                    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) = Unit

                    override fun afterTextChanged(s: Editable) {
                        if (skip) skip = false else updateError()
                    }
                })

        if (intent.hasExtra(PROJECT_ID_KEY)) projectId = intent.getParcelableExtra(PROJECT_ID_KEY)

        userListFragment = supportFragmentManager.findFragmentById(R.id.show_project_frame) as? UserListFragment
                ?: UserListFragment.newInstance().also {
                    supportFragmentManager
                            .beginTransaction()
                            .add(R.id.show_project_frame, it)
                            .commit()
                }

        userListFragment.setFab(bottomFabMenuDelegate.fabDelegate)

        tryGetFragment<ConfirmDialogFragment>(DISCARD_TAG)?.listener = discardDialogListener

        showProjectViewModel = getViewModel<ShowProjectViewModel>().apply {
            start(projectId)

            createDisposable += data.subscribe { onLoadFinished(it) }
        }
    }

    private fun onLoadFinished(data: ShowProjectViewModel.Data) {
        this.data = data

        if (savedInstanceState == null) {
            binding.showProjectToolbarEditTextInclude
                    .toolbarEditText
                    .setText(data.name)
        } else {
            savedInstanceState = null
        }

        binding.showProjectToolbarEditTextInclude
                .toolbarLayout
                .apply {
                    visibility = View.VISIBLE
                    isHintAnimationEnabled = true
                }

        invalidateOptionsMenu()

        userListFragment.initialize(projectId, data)
    }

    override fun onBackPressed() {
        if (tryClose())
            super.onBackPressed()
    }

    private fun tryClose(): Boolean {
        return if (dataChanged()) {
            ConfirmDialogFragment.newInstance(ConfirmDialogFragment.Parameters.Discard).also {
                it.listener = discardDialogListener
                it.show(supportFragmentManager, DISCARD_TAG)
            }

            false
        } else {
            true
        }
    }

    private fun dataChanged(): Boolean {
        if (data == null)
            return false

        if (binding.showProjectToolbarEditTextInclude.toolbarEditText.text.isNullOrEmpty() != data!!.name.isNullOrEmpty())
            return true

        return if (!binding.showProjectToolbarEditTextInclude
                        .toolbarEditText
                        .text
                        .isNullOrEmpty()
                && binding.showProjectToolbarEditTextInclude
                        .toolbarEditText
                        .text
                        .toString() != data!!.name
        ) {
            true
        } else {
            userListFragment.dataChanged()
        }
    }

    private fun updateError(): Boolean {
        check(data != null)

        return if (binding.showProjectToolbarEditTextInclude.toolbarEditText.text.isNullOrEmpty()) {
            binding.showProjectToolbarEditTextInclude
                    .toolbarLayout
                    .error = getString(R.string.nameError)

            true
        } else {
            binding.showProjectToolbarEditTextInclude
                    .toolbarLayout
                    .error = null

            false
        }
    }

    override fun initBottomBar() {
        bottomBinding.bottomAppBar.apply {
            replaceMenu(R.menu.menu_select_all)

            setOnMenuItemClickListener { item ->
                check(item.itemId == R.id.action_select_all)

                userListFragment.treeViewAdapter.selectAll()

                true
            }
        }
    }

    override fun getBottomBar() = bottomBinding.bottomAppBar

    private fun updateBottomMenu() {
        bottomBinding.bottomAppBar
                .menu
            .findItem(R.id.action_select_all)
            ?.isVisible = selectAllVisible
    }

    override fun setUserSelectAllVisibility(selectAllVisible: Boolean) {
        this.selectAllVisible = selectAllVisible

        updateBottomMenu()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putParcelable(KEY_BOTTOM_FAB_MENU_DELEGATE_STATE, bottomFabMenuDelegate.state)
    }

    override val snackbarParent get() = binding.showProjectCoordinator
}
