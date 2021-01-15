package com.krystianwsul.checkme.gui.friends


import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.text.TextUtils
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.CustomItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.databinding.FragmentFriendPickerBinding
import com.krystianwsul.checkme.databinding.RowFriendBinding
import com.krystianwsul.checkme.gui.base.AbstractDialogFragment
import com.krystianwsul.checkme.gui.friends.findfriend.FindFriendActivity
import com.krystianwsul.checkme.gui.utils.ResettableProperty
import com.krystianwsul.checkme.utils.animateVisibility
import com.krystianwsul.checkme.utils.loadPhoto
import com.krystianwsul.common.utils.UserKey


class FriendPickerFragment : AbstractDialogFragment() {

    companion object {

        fun newInstance() = FriendPickerFragment()
    }

    private var data: Data? = null

    private lateinit var listener: (UserKey) -> Unit

    private val bindingProperty = ResettableProperty<FragmentFriendPickerBinding>()
    private var binding by bindingProperty

    @SuppressLint("InflateParams")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        binding = FragmentFriendPickerBinding.inflate(layoutInflater)

        binding.friendPickerRecycler.layoutManager = LinearLayoutManager(activity)

        return MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.friend_dialog_title)
                .setView(binding.root)
                .setPositiveButton(R.string.addFriend) { _, _ -> }
                .setNegativeButton(android.R.string.cancel) { dialog, _ -> dialog.cancel() }
                .create()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        if (data != null) initialize()
    }

    override fun onStart() {
        super.onStart()

        (requireDialog() as AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            startActivity(FindFriendActivity.newIntent(requireContext()))
        }
    }

    fun initialize(data: Data, listener: (UserKey) -> Unit) {
        this.data = data
        this.listener = listener

        if (bindingProperty.isSet) initialize()
    }

    private fun initialize() {
        check(activity != null)
        checkNotNull(data)

        animateVisibility(binding.friendPickerRecycler, binding.friendPickerProgress, data!!.immediate)

        binding.friendPickerRecycler.apply {
            adapter = FriendListAdapter()
            itemAnimator = CustomItemAnimator()
        }
    }

    override fun onDestroyView() {
        bindingProperty.reset()

        super.onDestroyView()
    }

    private inner class FriendListAdapter : RecyclerView.Adapter<FriendListAdapter.FriendHolder>() {

        override fun getItemCount() = data!!.friendDatas.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = FriendHolder(RowFriendBinding.inflate(layoutInflater, parent, false))

        override fun onBindViewHolder(friendHolder: FriendHolder, position: Int) {
            val friendData = data!!.friendDatas[position]

            friendHolder.binding.apply {
                friendImage.loadPhoto(friendData.photoUrl)
                friendName.text = friendData.name
                friendEmail.text = friendData.email

                root.setOnClickListener { friendHolder.onRowClick() }
            }
        }

        private inner class FriendHolder(val binding: RowFriendBinding) : RecyclerView.ViewHolder(binding.root) {

            fun onRowClick() {
                val friendData = data!!.friendDatas[adapterPosition]

                dismiss()

                listener(friendData.id)
            }
        }
    }

    data class Data(val immediate: Boolean, val friendDatas: List<FriendData>)

    class FriendData(
            val id: UserKey,
            val name: String,
            val email: String,
            val photoUrl: String?
    ) {

        init {
            check(!TextUtils.isEmpty(name))
            check(!TextUtils.isEmpty(email))
        }
    }
}
