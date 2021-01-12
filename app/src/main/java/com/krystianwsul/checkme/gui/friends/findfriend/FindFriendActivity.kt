package com.krystianwsul.checkme.gui.friends.findfriend

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.activity.viewModels
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.jakewharton.rxbinding4.widget.textChanges
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.databinding.ActivityFindFriendBinding
import com.krystianwsul.checkme.databinding.RowListAvatarBinding
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.extensions.tryAddFriend
import com.krystianwsul.checkme.gui.base.NavBarActivity
import com.krystianwsul.checkme.gui.friends.findfriend.viewmodel.FindFriendViewModel
import com.krystianwsul.checkme.persistencemodel.SaveService
import com.krystianwsul.checkme.utils.animateVisibility
import com.krystianwsul.checkme.utils.ignore
import com.krystianwsul.checkme.utils.loadPhoto
import com.krystianwsul.checkme.utils.toV3
import com.tbruyelle.rxpermissions2.RxPermissions
import io.reactivex.rxjava3.kotlin.addTo

class FindFriendActivity : NavBarActivity() {

    companion object {

        fun newIntent(context: Context) = Intent(context, FindFriendActivity::class.java)
    }

    private val viewModel by viewModels<FindFriendViewModel>()

    override val rootView get() = binding.findFriendRoot

    private lateinit var binding: ActivityFindFriendBinding

    private val adapter by lazy { Adapter() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityFindFriendBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.findFriendToolbar)

        binding.findFriendToolbar.run {
            setContentInsetsAbsolute(contentInsetStart, contentInsetStart)
        }

        binding.findFriendEmail
                .textChanges()
                .subscribe { viewModel.viewActionRelay.accept(FindFriendViewModel.ViewAction.Search(it.toString())) }
                .addTo(createDisposable)

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
                getItem(position).let { contact ->
                    root.setOnClickListener {
                        if (contact.userWrapper != null) {
                            val added = DomainFactory.instance.tryAddFriend(SaveService.Source.GUI, contact.userWrapper)

                            if (added) {
                                setSnackbar {
                                    it.showText(getString(R.string.addedUserToFriends), Snackbar.LENGTH_SHORT)
                                }
                                finish()
                            } else {
                                Snackbar.make(
                                        binding.root,
                                        getString(R.string.userAlreadyInFriends),
                                        Snackbar.LENGTH_SHORT
                                ).show()
                            }
                        } else {
                            startActivity(
                                    Intent(Intent.ACTION_SENDTO).setData(Uri.parse("mailto:"))
                                            .putExtra(Intent.EXTRA_EMAIL, arrayOf(contact.email))
                                            .putExtra(Intent.EXTRA_SUBJECT, "I'm a subject!")
                                            .putExtra(Intent.EXTRA_TEXT, "I'm a body!")
                            )
                        }
                    }

                    rowListAvatarImage.loadPhoto(contact.photoUri)

                    rowListAvatarName.text = contact.displayName
                    rowListAvatarDetails.text = contact.email

                    rowListAvatarChildren.isVisible = false

                    rowListAvatarSeparator.isInvisible = position == itemCount - 1
                }
            }
        }
    }

    private class Holder(val binding: RowListAvatarBinding) : RecyclerView.ViewHolder(binding.root)
}
