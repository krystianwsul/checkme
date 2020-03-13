package com.krystianwsul.checkme.gui.friends


import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.CustomItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.gui.AbstractDialogFragment
import com.krystianwsul.checkme.utils.animateVisibility
import com.krystianwsul.checkme.utils.loadPhoto
import com.krystianwsul.common.utils.UserKey
import kotlinx.android.synthetic.main.fragment_friend_picker.view.*
import kotlinx.android.synthetic.main.row_friend.view.*


class FriendPickerFragment : AbstractDialogFragment() {

    companion object {

        fun newInstance() = FriendPickerFragment()
    }

    private lateinit var friendPickerProgress: ProgressBar
    private lateinit var friendPickerRecycler: RecyclerView

    private var data: Data? = null

    private lateinit var listener: (UserKey) -> Unit

    @SuppressLint("InflateParams")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = requireActivity().layoutInflater
                .inflate(R.layout.fragment_friend_picker, null)
                .also {
                    friendPickerProgress = it.friendPickerProgress
                    friendPickerRecycler = it.friendPickerRecycler.apply {
                        layoutManager = LinearLayoutManager(activity)
                    }
                }

        return MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.friend_dialog_title)
                .setView(view)
                .setNegativeButton(android.R.string.cancel) { dialog, _ -> dialog.cancel() }
                .create()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        if (data != null)
            initialize()
    }

    fun initialize(data: Data, listener: (UserKey) -> Unit) {
        this.data = data
        this.listener = listener

        if (this::friendPickerRecycler.isInitialized)
            initialize()
    }

    private fun initialize() {
        checkNotNull(activity)
        checkNotNull(data)

        animateVisibility(friendPickerRecycler, friendPickerProgress, data!!.immediate)

        friendPickerRecycler.adapter = FriendListAdapter()
        friendPickerRecycler.itemAnimator = CustomItemAnimator()
    }

    private inner class FriendListAdapter : RecyclerView.Adapter<FriendListAdapter.FriendHolder>() {

        override fun getItemCount() = data!!.friendDatas.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FriendHolder {
            val friendRow = requireActivity().layoutInflater.inflate(R.layout.row_friend, parent, false)

            val friendImage = friendRow.friendImage
            val friendName = friendRow.findViewById<View>(R.id.friendName) as TextView
            val friendEmail = friendRow.findViewById<View>(R.id.friendEmail) as TextView

            return FriendHolder(friendRow, friendImage, friendName, friendEmail)
        }

        override fun onBindViewHolder(friendHolder: FriendHolder, position: Int) {
            val friendData = data!!.friendDatas[position]

            friendHolder.friendImage.loadPhoto(friendData.photoUrl)
            friendHolder.friendName.text = friendData.name
            friendHolder.friendEmail.text = friendData.email

            friendHolder.friendRow.setOnClickListener { friendHolder.onRowClick() }
        }

        private inner class FriendHolder(
                val friendRow: View,
                val friendImage: ImageView,
                val friendName: TextView,
                val friendEmail: TextView) : RecyclerView.ViewHolder(friendRow) {

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
