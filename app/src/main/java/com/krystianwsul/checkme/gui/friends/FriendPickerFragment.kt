package com.krystianwsul.checkme.gui.friends


import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.customview.getCustomView
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.gui.AbstractDialogFragment
import com.krystianwsul.checkme.utils.animateVisibility
import kotlinx.android.synthetic.main.fragment_friend_picker.view.*


class FriendPickerFragment : AbstractDialogFragment() {

    companion object {

        fun newInstance() = FriendPickerFragment()
    }

    private lateinit var friendPickerProgress: ProgressBar
    private lateinit var friendPickerRecycler: RecyclerView

    private var friendDatas: List<FriendData>? = null

    private lateinit var listener: (String) -> Unit

    override fun onCreateDialog(savedInstanceState: Bundle?) = MaterialDialog(requireActivity()).apply {
        title(R.string.friend_dialog_title)
        customView(R.layout.fragment_friend_picker)
        negativeButton(android.R.string.cancel) { it.cancel() }

        getCustomView()!!.also {
            friendPickerProgress = it.friendPickerProgress
            friendPickerRecycler = it.friendPickerRecycler.apply {
                layoutManager = LinearLayoutManager(activity)
            }
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        animateVisibility(friendPickerRecycler, friendPickerProgress)

        if (friendDatas != null)
            initialize()
    }

    fun initialize(friendDatas: List<FriendData>, listener: (String) -> Unit) {
        this.friendDatas = friendDatas
        this.listener = listener

        if (activity != null)
            initialize()
    }

    private fun initialize() {
        check(activity != null)
        check(friendDatas != null)

        friendPickerRecycler.adapter = FriendListAdapter()
    }

    private inner class FriendListAdapter : RecyclerView.Adapter<FriendListAdapter.FriendHolder>() {

        override fun getItemCount() = friendDatas!!.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FriendListAdapter.FriendHolder {
            val friendRow = requireActivity().layoutInflater.inflate(R.layout.row_friend, parent, false)

            val friendName = friendRow.findViewById<View>(R.id.friendName) as TextView

            val friendEmail = friendRow.findViewById<View>(R.id.friendEmail) as TextView

            return FriendHolder(friendRow, friendName, friendEmail)
        }

        override fun onBindViewHolder(friendHolder: FriendListAdapter.FriendHolder, position: Int) {
            val friendData = friendDatas!![position]

            friendHolder.friendName.text = friendData.name
            friendHolder.friendEmail.text = friendData.email

            friendHolder.friendRow.setOnClickListener { friendHolder.onRowClick() }
        }

        private inner class FriendHolder(val friendRow: View, val friendName: TextView, val friendEmail: TextView) : RecyclerView.ViewHolder(friendRow) {

            fun onRowClick() {
                val friendData = friendDatas!![adapterPosition]

                dismiss()

                listener(friendData.id)
            }
        }
    }

    class FriendData(val id: String, val name: String, val email: String) {

        init {
            check(!TextUtils.isEmpty(id))
            check(!TextUtils.isEmpty(name))
            check(!TextUtils.isEmpty(email))
        }
    }
}
