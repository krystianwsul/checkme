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
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.jakewharton.rxbinding3.view.keys
import com.jakewharton.rxbinding3.widget.editorActionEvents
import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.databinding.ActivityFindFriendBinding
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.extensions.addFriend
import com.krystianwsul.checkme.firebase.AndroidDatabaseWrapper
import com.krystianwsul.checkme.gui.base.NavBarActivity
import com.krystianwsul.checkme.persistencemodel.SaveService
import com.krystianwsul.checkme.utils.animateVisibility
import com.krystianwsul.checkme.utils.loadPhoto
import com.krystianwsul.checkme.viewmodels.FindFriendViewModel
import com.krystianwsul.common.firebase.UserData
import com.krystianwsul.common.firebase.json.UserWrapper
import com.krystianwsul.common.utils.UserKey
import io.reactivex.rxkotlin.plusAssign

class FindFriendActivity : NavBarActivity() {

    companion object {

        private const val KEY_USER_KEY = "userKey"
        private const val KEY_USER_WRAPPER = "userWrapper"
        private const val LOADING_KEY = "loading"

        fun newIntent(context: Context) = Intent(context, FindFriendActivity::class.java)
    }

    private val viewModel by viewModels<FindFriendViewModel>()

    private var loading = false
    private var userPair: Pair<UserKey, UserWrapper>? = null

    private var databaseReference: DatabaseReference? = null
    private var valueEventListener: ValueEventListener? = null

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
            check(!loading)

            DomainFactory.instance.addFriend(SaveService.Source.GUI, userPair!!.first, userPair!!.second)

            finish()
        }
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)

        savedInstanceState.apply {
            check(containsKey(LOADING_KEY))
            loading = getBoolean(LOADING_KEY)

            if (containsKey(KEY_USER_KEY)) {
                check(containsKey(KEY_USER_WRAPPER))

                val userKey = getParcelable<UserKey>(KEY_USER_KEY)!!
                val userWrapper = getParcelable<UserWrapper>(KEY_USER_WRAPPER)!!

                userPair = userKey to userWrapper
            }
        }

        updateLayout()
    }

    override fun onStart() {
        super.onStart()

        if (loading) loadUser()
    }

    private fun loadUser() {
        check(loading)
        check(binding.findFriendEmail.text.isNotEmpty())

        val key = UserData.getKey(binding.findFriendEmail.text.toString())

        valueEventListener = object : ValueEventListener {

            override fun onDataChange(dataSnapshot: DataSnapshot) {
                databaseReference!!.removeEventListener(valueEventListener!!)

                loading = false
                valueEventListener = null
                databaseReference = null

                if (dataSnapshot.exists()) {
                    userPair = UserKey(dataSnapshot.key!!) to dataSnapshot.getValue(UserWrapper::class.java)!!
                } else {
                    Snackbar.make(binding.findFriendCoordinator, R.string.userNotFound, Snackbar.LENGTH_SHORT).show()
                }

                updateLayout()
            }

            override fun onCancelled(databaseError: DatabaseError) {
                databaseReference!!.removeEventListener(valueEventListener!!)

                loading = false
                valueEventListener = null
                databaseReference = null

                updateLayout()

                MyCrashlytics.logException(databaseError.toException())

                Snackbar.make(binding.findFriendCoordinator, R.string.connectionError, Snackbar.LENGTH_SHORT).show()
            }
        }

        databaseReference = AndroidDatabaseWrapper.getUserDataDatabaseReference(key)

        databaseReference!!.addValueEventListener(valueEventListener!!)
    }

    private fun updateLayout() {
        val hide = mutableListOf<View>()
        val show = mutableListOf<View>()

        when {
            userPair != null -> {
                check(!loading)

                userPair!!.second
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
            loading -> {
                binding.findFriendEmail.isEnabled = false

                hide += binding.findFriendUserLayout
                show += binding.findFriendProgress
            }
            else -> {
                binding.findFriendEmail.isEnabled = true

                hide += binding.findFriendUserLayout
                hide += binding.findFriendProgress
            }
        }

        animateVisibility(show, hide)
    }

    override fun onStop() {
        super.onStop()

        if (loading) {
            checkNotNull(databaseReference)
            checkNotNull(valueEventListener)

            databaseReference!!.removeEventListener(valueEventListener!!)
        } else {
            check(databaseReference == null)
            check(valueEventListener == null)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.run {
            putBoolean(LOADING_KEY, loading)

            userPair?.let {
                putParcelable(KEY_USER_KEY, it.first)
                putParcelable(KEY_USER_WRAPPER, it.second)
            }
        }
    }

    private fun startSearch() {
        if (binding.findFriendEmail.text.isEmpty()) return

        loading = true
        userPair = null

        updateLayout()

        loadUser()
    }
}
