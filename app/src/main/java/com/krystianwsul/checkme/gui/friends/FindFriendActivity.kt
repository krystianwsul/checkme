package com.krystianwsul.checkme.gui.friends

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.firebase.DatabaseWrapper
import com.krystianwsul.checkme.firebase.UserData
import com.krystianwsul.checkme.gui.MainActivity
import junit.framework.Assert
import kotlinx.android.synthetic.main.activity_find_friend.*

class FindFriendActivity : AppCompatActivity() {

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

        Log.e("asdf", "onCreate " + hashCode())

        setSupportActionBar(findFriendToolbar)

        findFriendEmail.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                startSearch()
                return@setOnEditorActionListener true
            }
            false
        }

        findFriendUserLayout.setOnClickListener {
            Assert.assertTrue(!loading)

            val myUserInfo = MainActivity.getUserInfo()!!

            DatabaseWrapper.addFriend(myUserInfo, userData!!)

            finish()
        }
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)

        Assert.assertTrue(savedInstanceState.containsKey(LOADING_KEY))
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
        Assert.assertTrue(loading)
        Assert.assertTrue(findFriendEmail.text.isNotEmpty())

        val key = UserData.getKey(findFriendEmail.text.toString())

        Log.e("asdf", "starting")

        valueEventListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot?) {
                Log.e("asdf", "onDataChange " + hashCode())

                Assert.assertTrue(dataSnapshot != null)

                databaseReference!!.removeEventListener(valueEventListener!!)

                loading = false
                valueEventListener = null
                databaseReference = null

                if (dataSnapshot!!.exists()) {
                    userData = dataSnapshot.getValue(UserData::class.java)!!
                } else {
                    Toast.makeText(this@FindFriendActivity, R.string.userNotFound, Toast.LENGTH_SHORT).show()
                }

                updateLayout()
            }

            override fun onCancelled(databaseError: DatabaseError?) {
                Assert.assertTrue(databaseError != null)

                databaseReference!!.removeEventListener(valueEventListener!!)

                loading = false
                valueEventListener = null
                databaseReference = null

                updateLayout()

                MyCrashlytics.logException(databaseError!!.toException())

                Log.e("asdf", "onCancelled", databaseError.toException())

                Toast.makeText(this@FindFriendActivity, R.string.connectionError, Toast.LENGTH_SHORT).show()
            }
        }

        databaseReference = DatabaseWrapper.getUserDataDatabaseReference(key)

        Log.e("asdf", "addValueEventListener " + valueEventListener!!.hashCode())
        databaseReference!!.addValueEventListener(valueEventListener)
    }

    private fun updateLayout() {
        Log.e("asdf", "updateLayout " + hashCode())

        when {
            userData != null -> {
                Assert.assertTrue(!loading)

                findFriendEmail.isEnabled = true
                findFriendUserLayout.visibility = View.VISIBLE
                findFriendProgress.visibility = View.GONE

                findFriendUserName.text = userData!!.name
                findFriendUserEmail.text = userData!!.email
            }
            loading -> {
                findFriendEmail.isEnabled = false
                findFriendUserLayout.visibility = View.GONE
                findFriendProgress.visibility = View.VISIBLE
            }
            else -> {
                findFriendEmail.isEnabled = true
                findFriendUserLayout.visibility = View.GONE
                findFriendProgress.visibility = View.GONE
            }
        }
    }

    override fun onStop() {
        super.onStop()

        Log.e("asdf", "onStop")

        if (loading) {
            Assert.assertTrue(databaseReference != null)
            Assert.assertTrue(valueEventListener != null)

            Log.e("asdf", "removing listener " + valueEventListener!!.hashCode())

            databaseReference!!.removeEventListener(valueEventListener!!)
        } else {
            Assert.assertTrue(databaseReference == null)
            Assert.assertTrue(valueEventListener == null)
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
