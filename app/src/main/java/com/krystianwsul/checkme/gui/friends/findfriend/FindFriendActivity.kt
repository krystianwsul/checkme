package com.krystianwsul.checkme.gui.friends.findfriend

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
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
import com.krystianwsul.checkme.domainmodel.DomainListenerManager
import com.krystianwsul.checkme.domainmodel.extensions.tryAddFriend
import com.krystianwsul.checkme.gui.base.NavBarActivity
import com.krystianwsul.checkme.gui.dialogs.ConfirmDialogFragment
import com.krystianwsul.checkme.gui.friends.findfriend.viewmodel.FindFriendViewEvent
import com.krystianwsul.checkme.gui.friends.findfriend.viewmodel.FindFriendViewModel
import com.krystianwsul.checkme.gui.friends.findfriend.viewmodel.FindFriendViewState
import com.krystianwsul.checkme.persistencemodel.SaveService
import com.krystianwsul.checkme.utils.*
import com.tbruyelle.rxpermissions2.RxPermissions
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.subscribeBy

class FindFriendActivity : NavBarActivity() {

    companion object {

        private const val TAG_CONFIRM_DIALOG = "confirmDialog"

        fun newIntent(context: Context) = Intent(context, FindFriendActivity::class.java)
    }

    private val viewModel by viewModels<FindFriendViewModel>()

    override val rootView get() = binding.findFriendRoot

    private lateinit var binding: ActivityFindFriendBinding

    private val adapter by lazy { Adapter() }

    private val confirmDialogListener: (Parcelable?) -> Unit = { payload ->
        val contact = payload as FindFriendViewModel.Person

        startActivity(
                Intent(Intent.ACTION_SEND)
                        .setType("text/plain")
                        .putExtra(Intent.EXTRA_EMAIL, arrayOf(contact.email))
                        .putExtra(Intent.EXTRA_SUBJECT, getString(R.string.downloadCheckMeTitle))
                        .putExtra(Intent.EXTRA_TEXT, getString(R.string.downloadCheckMeLink))
        )
    }

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
                .subscribe { viewModel.viewActionRelay.accept(FindFriendViewEvent.Search(it.toString())) }
                .addTo(createDisposable)

        binding.findFriendRecycler.let {
            it.layoutManager = LinearLayoutManager(this)
            it.adapter = adapter
        }

        viewModel.viewStateObservable
                .subscribe(::updateLayout)
                .addTo(createDisposable)

        tryGetFragment<ConfirmDialogFragment>(TAG_CONFIRM_DIALOG)?.listener = confirmDialogListener
    }

    private fun updateLayout(state: FindFriendViewState) {
        val hide = mutableListOf<View>()
        val show = mutableListOf<View>()

        @Suppress("IMPLICIT_CAST_TO_ANY")
        when (state) {
            FindFriendViewState.Permissions ->
                RxPermissions(this).request(Manifest.permission.READ_CONTACTS)
                        .toV3()
                        .subscribe { viewModel.viewActionRelay.accept(FindFriendViewEvent.Permissions(it)) }
                        .addTo(createDisposable)
            is FindFriendViewState.Loading -> {
                binding.findFriendEmail.isEnabled = false

                hide += binding.findFriendRecycler
                show += binding.findFriendProgress
            }
            is FindFriendViewState.Loaded -> {
                binding.findFriendEmail.isEnabled = true

                show += binding.findFriendRecycler
                hide += binding.findFriendProgress

                adapter.submitList(state.people) { binding.findFriendRecycler.smoothScrollToPosition(0) }
            }
        }.ignore()

        animateVisibility(show, hide)
    }

    private inner class Adapter : ListAdapter<FindFriendViewModel.Person, Holder>(
            object : DiffUtil.ItemCallback<FindFriendViewModel.Person>() {

                override fun areItemsTheSame(
                        oldItem: FindFriendViewModel.Person,
                        newItem: FindFriendViewModel.Person,
                ) = oldItem.email == newItem.email

                override fun areContentsTheSame(
                        oldItem: FindFriendViewModel.Person,
                        newItem: FindFriendViewModel.Person,
                ) = oldItem == newItem
            }
    ) {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
                Holder(RowListAvatarBinding.inflate(layoutInflater, parent, false))

        override fun onBindViewHolder(holder: Holder, position: Int) {
            holder.binding.apply {
                getItem(position).let { contact ->
                    root.setOnClickListener {
                        hideSoftKeyboard(binding.root)

                        if (contact.userWrapper != null) {
                            DomainFactory.instance
                                    .tryAddFriend(
                                            DomainListenerManager.NotificationType.All,
                                            SaveService.Source.GUI,
                                            contact.userWrapper,
                                    )
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribeBy {
                                        if (it) {
                                            setSnackbar {
                                                it.showText(R.string.addedUserToFriends, Snackbar.LENGTH_SHORT)
                                            }

                                            finish()
                                        } else {
                                            Snackbar.make(
                                                    binding.root,
                                                    getString(R.string.userAlreadyInFriends),
                                                    Snackbar.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                    .addTo(createDisposable)
                        } else {
                            ConfirmDialogFragment.newInstance(
                                    ConfirmDialogFragment.Parameters(
                                            R.string.inviteTitle,
                                            R.string.share,
                                            R.string.userNotFound,
                                            contact
                                    )
                            )
                                    .apply { listener = confirmDialogListener }
                                    .show(supportFragmentManager, TAG_CONFIRM_DIALOG)
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
