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

        viewModel.viewStateObservable
                .subscribe(::updateLayout)
                .addTo(createDisposable)
    }

    private fun updateLayout(state: FindFriendViewModel.ViewState) {
        val hide = mutableListOf<View>()
        val show = mutableListOf<View>()

        @Suppress("IMPLICIT_CAST_TO_ANY")
        when (state) {
            FindFriendViewModel.ViewState.Permissions ->
                RxPermissions(this).request(Manifest.permission.READ_CONTACTS)
                        .toV3()
                        .subscribe { viewModel.viewActionRelay.accept(FindFriendViewModel.ViewAction.Permissions(it)) }
                        .addTo(createDisposable)
            is FindFriendViewModel.ViewState.Loading -> {
                binding.findFriendEmail.isEnabled = false

                hide += binding.findFriendRecycler
                show += binding.findFriendProgress
            }
            is FindFriendViewModel.ViewState.Loaded -> {
                binding.findFriendEmail.isEnabled = true

                show += binding.findFriendRecycler
                hide += binding.findFriendProgress

                adapter.submitList(state.contacts) { binding.findFriendRecycler.smoothScrollToPosition(0) }
            }
            is FindFriendViewModel.ViewState.Error ->
                Snackbar.make(binding.root, state.stringRes, Snackbar.LENGTH_SHORT).show()
        }.ignore()

        animateVisibility(show, hide)
    }

    private fun startSearch() {
        binding.findFriendEmail
                .text
                .toString()
                .takeIf { it.isNotEmpty() }
                ?.let { viewModel.viewActionRelay.accept(FindFriendViewModel.ViewAction.Search(it)) }
    }

    private inner class Adapter : ListAdapter<FindFriendViewModel.Contact, Holder>(
            object : DiffUtil.ItemCallback<FindFriendViewModel.Contact>() {

                override fun areItemsTheSame(
                        oldItem: FindFriendViewModel.Contact,
                        newItem: FindFriendViewModel.Contact,
                ) = oldItem.email == newItem.email

                override fun areContentsTheSame(
                        oldItem: FindFriendViewModel.Contact,
                        newItem: FindFriendViewModel.Contact,
                ) = oldItem == newItem
            }
    ) {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
                Holder(RowListAvatarBinding.inflate(layoutInflater, parent, false))

        override fun onBindViewHolder(holder: Holder, position: Int) {
            holder.binding.apply {
                getItem(position).apply {
                    root.setOnClickListener {
                        viewModel.viewActionRelay.accept(FindFriendViewModel.ViewAction.AddFriend)
                        finish()
                    }

                    rowListAvatarImage.loadPhoto(photoUri)

                    rowListAvatarName.text = displayName
                    rowListAvatarDetails.text = email

                    rowListAvatarChildren.isVisible = false

                    rowListAvatarSeparator.isInvisible = position == itemCount - 1
                }
            }
        }
    }

    private class Holder(val binding: RowListAvatarBinding) : RecyclerView.ViewHolder(binding.root)
}
