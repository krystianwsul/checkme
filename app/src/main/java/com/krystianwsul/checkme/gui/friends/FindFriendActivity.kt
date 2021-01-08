package com.krystianwsul.checkme.gui.friends

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.view.inputmethod.EditorInfo
import androidx.activity.viewModels
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.jakewharton.rxbinding4.view.keys
import com.jakewharton.rxbinding4.widget.editorActionEvents
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.databinding.ActivityFindFriendBinding
import com.krystianwsul.checkme.databinding.RowListAvatarBinding
import com.krystianwsul.checkme.gui.base.NavBarActivity
import com.krystianwsul.checkme.utils.animateVisibility
import com.krystianwsul.checkme.utils.ignore
import com.krystianwsul.checkme.utils.loadPhoto
import com.krystianwsul.checkme.utils.toV3
import com.krystianwsul.checkme.viewmodels.FindFriendViewModel
import com.tbruyelle.rxpermissions2.RxPermissions
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.plusAssign

class FindFriendActivity : NavBarActivity() {

    companion object {

        fun newIntent(context: Context) = Intent(context, FindFriendActivity::class.java)
    }

    private val viewModel by viewModels<FindFriendViewModel>()

    override val rootView get() = binding.findFriendRoot

    private lateinit var binding: ActivityFindFriendBinding

    private val adapter by lazy { Adapter() }

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

        binding.findFriendRecycler.let {
            it.layoutManager = LinearLayoutManager(this)
            it.adapter = adapter
        }

        RxPermissions(this).request(Manifest.permission.READ_CONTACTS)
                .toV3()
                .filter { it }
                .subscribe { viewModel.fetchContacts() }
                .addTo(createDisposable)

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

                hide += binding.findFriendRecycler
                hide += binding.findFriendProgress
            }
            is FindFriendViewModel.State.Loading -> {
                binding.findFriendEmail.isEnabled = false

                hide += binding.findFriendRecycler
                show += binding.findFriendProgress
            }
            is FindFriendViewModel.State.Found -> {
                binding.findFriendEmail.isEnabled = true

                show += binding.findFriendRecycler
                hide += binding.findFriendProgress

                adapter.submitList(listOf(Item(state)))
            }
            is FindFriendViewModel.State.Error ->
                Snackbar.make(binding.root, state.stringRes, Snackbar.LENGTH_SHORT).show()
        }.ignore()

        animateVisibility(show, hide)
    }

    private fun startSearch() {
        viewModel.startSearch(binding.findFriendEmail.text.toString())
    }

    private inner class Adapter : ListAdapter<Item, Holder>(
            object : DiffUtil.ItemCallback<Item>() {

                override fun areItemsTheSame(oldItem: Item, newItem: Item) = true

                override fun areContentsTheSame(oldItem: Item, newItem: Item) = oldItem == newItem
            }
    ) {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
                Holder(RowListAvatarBinding.inflate(layoutInflater, parent, false))

        override fun onBindViewHolder(holder: Holder, position: Int) {
            holder.binding.apply {
                getItem(position).state
                        .userWrapper
                        .userData
                        .apply {
                            root.setOnClickListener {
                                viewModel.addFriend()
                                finish()
                            }

                            rowListAvatarImage.loadPhoto(photoUrl)

                            rowListAvatarName.text = name
                            rowListAvatarDetails.text = email

                            rowListAvatarChildren.isVisible = false

                            rowListAvatarSeparator.isInvisible = position == itemCount - 1
                        }
            }
        }
    }

    private class Holder(val binding: RowListAvatarBinding) : RecyclerView.ViewHolder(binding.root)

    private data class Item(val state: FindFriendViewModel.State.Found)
}
