package com.krystianwsul.checkme.gui.friends

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.jakewharton.rxbinding3.view.keys
import com.jakewharton.rxbinding3.widget.editorActionEvents
import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.firebase.DatabaseWrapper
import com.krystianwsul.checkme.firebase.UserData
import com.krystianwsul.checkme.gui.NavBarActivity
import com.krystianwsul.checkme.utils.animateVisibility
import com.krystianwsul.checkme.utils.checkError
import com.krystianwsul.checkme.utils.loadPhoto
import io.reactivex.rxkotlin.plusAssign
import kotlinx.android.synthetic.main.activity_find_friend.*

class FindFriendActivity : NavBarActivity() {

    companion object {

        private const val USER_KEY = "user"
        private const val LOADING_KEY = "loading"

        fun newIntent(context: Context) = Intent(context, FindFriendActivity::class.java)
    }

    private var loading = false
    private var userData: UserData? = null

    private var databaseReference: DatabaseReference? = null
    private var valueEventListener: ValueEventListener? = null

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

        setContentView(R.layout.activity_find_friend)

        setSupportActionBar(findFriendToolbar)

        findFriendEmail.apply {
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

        findFriendUserLayout.setOnClickListener {
            check(!loading)

            DatabaseWrapper.addFriend(userData!!.key).checkError(DomainFactory.instance, "FindFriendActivity.addFriend")

            finish()
        }
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)

        check(savedInstanceState.containsKey(LOADING_KEY))
        loading = savedInstanceState.getBoolean(LOADING_KEY)

        if (savedInstanceState.containsKey(USER_KEY))
            userData = savedInstanceState.getParcelable(USER_KEY)!!

        updateLayout()
    }

    override fun onStart() {
        super.onStart()

        if (loading)
            loadUser()
    }

    private fun loadUser() {
        check(loading)
        check(findFriendEmail.text.isNotEmpty())

        val key = UserData.getKey(findFriendEmail.text.toString())

        valueEventListener = object : ValueEventListener {

            override fun onDataChange(dataSnapshot: DataSnapshot) {
                databaseReference!!.removeEventListener(valueEventListener!!)

                loading = false
                valueEventListener = null
                databaseReference = null

                if (dataSnapshot.exists()) {
                    userData = dataSnapshot.getValue(UserData::class.java)!!
                } else {
                    Snackbar.make(findFriendCoordinator, R.string.userNotFound, Snackbar.LENGTH_SHORT).show()
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

                Snackbar.make(findFriendCoordinator, R.string.connectionError, Snackbar.LENGTH_SHORT).show()
            }
        }

        databaseReference = DatabaseWrapper.getUserDataDatabaseReference(key)

        databaseReference!!.addValueEventListener(valueEventListener!!)
    }

    private fun updateLayout() {
        val hide = mutableListOf<View>()
        val show = mutableListOf<View>()

        when {
            userData != null -> {
                check(!loading)

                findFriendEmail.isEnabled = true
                show.add(findFriendUserLayout)
                hide.add(findFriendProgress)

                findFriendUserPhoto.loadPhoto(userData!!.photoUrl)
                findFriendUserName.text = userData!!.name
                findFriendUserEmail.text = userData!!.email
            }
            loading -> {
                findFriendEmail.isEnabled = false
                hide.add(findFriendUserLayout)
                show.add(findFriendProgress)
            }
            else -> {
                findFriendEmail.isEnabled = true
                hide.add(findFriendUserLayout)
                hide.add(findFriendProgress)
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

            userData?.let { putParcelable(USER_KEY, it) }
        }
    }

    private fun startSearch() {
        if (findFriendEmail.text.isEmpty())
            return

        loading = true
        userData = null

        updateLayout()

        loadUser()
    }
}
