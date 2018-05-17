package com.krystianwsul.checkme.gui.friends


import android.app.Dialog
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.TextUtils
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView

import com.afollestad.materialdialogs.MaterialDialog
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.gui.AbstractDialogFragment

import junit.framework.Assert

class FriendPickerFragment : AbstractDialogFragment() {

    companion object {

        fun newInstance() = FriendPickerFragment()
    }

    private lateinit var friendPickerProgress: ProgressBar
    private lateinit var friendPickerRecycler: RecyclerView

    private var friendDatas: List<FriendData>? = null

    private lateinit var listener: (String) -> Unit

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val materialDialog = MaterialDialog.Builder(requireActivity())
                .title(R.string.friend_dialog_title)
                .customView(R.layout.fragment_friend_picker, false)
                .negativeText(android.R.string.cancel)
                .onNegative { dialog, _ -> dialog.cancel() }
                .build()

        val linearLayout = materialDialog.customView as LinearLayout

        friendPickerProgress = linearLayout.findViewById<View>(R.id.friendPickerProgress) as ProgressBar

        friendPickerRecycler = linearLayout.findViewById<View>(R.id.friendPickerRecycler) as RecyclerView

        friendPickerRecycler.layoutManager = LinearLayoutManager(activity)

        return materialDialog
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        friendPickerProgress.visibility = View.GONE
        friendPickerRecycler.visibility = View.VISIBLE

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
        Assert.assertTrue(activity != null)
        Assert.assertTrue(friendDatas != null)

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
            Assert.assertTrue(!TextUtils.isEmpty(id))
            Assert.assertTrue(!TextUtils.isEmpty(name))
            Assert.assertTrue(!TextUtils.isEmpty(email))
        }
    }
}
