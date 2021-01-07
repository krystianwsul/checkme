package com.krystianwsul.checkme.gui.friends

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.activity.viewModels
import com.google.android.material.snackbar.Snackbar
import com.jakewharton.rxbinding3.view.keys
import com.jakewharton.rxbinding3.widget.editorActionEvents
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.databinding.ActivityFindFriendBinding
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.extensions.addFriend
import com.krystianwsul.checkme.gui.base.NavBarActivity
import com.krystianwsul.checkme.persistencemodel.SaveService
import com.krystianwsul.checkme.utils.animateVisibility
import com.krystianwsul.checkme.utils.ignore
import com.krystianwsul.checkme.utils.loadPhoto
import com.krystianwsul.checkme.viewmodels.FindFriendViewModel
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.plusAssign

class FindFriendActivity : NavBarActivity() {

    companion object {

        fun newIntent(context: Context) = Intent(context, FindFriendActivity::class.java)
    }

    private val viewModel by viewModels<FindFriendViewModel>()

    override val rootView get() = binding.findFriendRoot

    private lateinit var binding: ActivityFindFriendBinding

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_find_friend, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_search -> startSearch()
            else -> throw UnsupportedOperationException()
        }
        return true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityFindFriendBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.findFriendToolbar)

        binding.findFriendEmail.apply {
            createDisposable += editorActionEvents {
                if (it.actionId == EditorInfo.IME_ACTION_SEARCH) {
                    startSearch()
                    true
                } else {
                    false
                }
            }.subscribe()

            createDisposable += keys {
                if (it.action == KeyEvent.ACTION_DOWN && it.keyCode == KeyEvent.KEYCODE_ENTER) {
                    startSearch()
                    true
                } else {
                    false
                }
            }.subscribe()
        }

        binding.findFriendUserLayout.setOnClickListener {
            (viewModel.state as FindFriendViewModel.State.Found).apply { // todo friend
                DomainFactory.instance.addFriend(SaveService.Source.GUI, userKey, userWrapper)
            }

            finish()
        }

        viewModel.stateObservable
                .subscribe { updateLayout(it) }
                .addTo(createDisposable)
    }

    private fun updateLayout(state: FindFriendViewModel.State) {
        val hide = mutableListOf<View>()
        val show = mutableListOf<View>()

        when (state) {
            FindFriendViewModel.State.None -> {
                binding.findFriendEmail.isEnabled = true

                hide += binding.findFriendUserLayout
                hide += binding.findFriendProgress
            }
            is FindFriendViewModel.State.Loading -> {
                binding.findFriendEmail.isEnabled = false

                hide += binding.findFriendUserLayout
                show += binding.findFriendProgress
            }
            is FindFriendViewModel.State.Found -> {
                state.userWrapper
                        .userData
                        .apply {
                            binding.findFriendUserPhoto.loadPhoto(photoUrl)
                            binding.findFriendUserName.text = name
                            binding.findFriendUserEmail.text = email
                        }

                binding.findFriendEmail.isEnabled = true

                show += binding.findFriendUserLayout
                hide += binding.findFriendProgress
            }
            is FindFriendViewModel.State.Error ->
                Snackbar.make(binding.findFriendCoordinator, state.stringRes, Snackbar.LENGTH_SHORT).show()
        }.ignore()

        animateVisibility(show, hide)
    }

    private fun startSearch() {
        viewModel.startSearch(binding.findFriendEmail.text.toString())
    }
}
