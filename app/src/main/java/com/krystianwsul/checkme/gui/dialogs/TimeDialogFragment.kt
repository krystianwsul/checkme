package com.krystianwsul.checkme.gui.dialogs

import android.app.Dialog
import android.os.Bundle
import android.os.Parcelable
import android.text.TextUtils
import android.widget.ArrayAdapter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.gui.base.AbstractDialogFragment
import com.krystianwsul.common.utils.CustomTimeKey
import kotlinx.parcelize.Parcelize
import java.util.*

class TimeDialogFragment : AbstractDialogFragment() {

    companion object {

        private const val CUSTOM_TIMES_KEY = "customTimes"

        fun newInstance(customTimeDatas: ArrayList<CustomTimeData>) = TimeDialogFragment().apply {
            arguments = Bundle().apply {
                putParcelableArrayList(CUSTOM_TIMES_KEY, customTimeDatas)
            }
        }
    }

    lateinit var timeDialogListener: TimeDialogListener

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        check(requireArguments().containsKey(CUSTOM_TIMES_KEY))

        val customTimeDatas = requireArguments().getParcelableArrayList<CustomTimeData>(CUSTOM_TIMES_KEY)!!

        val names = customTimeDatas.map { it.name } + getString(R.string.add) + getString(R.string.other)

        val adapter = ArrayAdapter(requireContext(), R.layout.row_time, R.id.timeRowText, names)

        return MaterialAlertDialogBuilder(requireContext())
                .setAdapter(adapter) { _, which ->
                    check(which < names.size)
                    check(which < customTimeDatas.size + 2)

                    when {
                        which < customTimeDatas.size -> {
                            val customTimeKey = customTimeDatas[which].customTimeKey
                            timeDialogListener.onCustomTimeSelected(customTimeKey)
                        }
                        which == customTimeDatas.size -> timeDialogListener.onAddSelected()
                        else -> {
                            check(which == customTimeDatas.size + 1)
                            timeDialogListener.onOtherSelected()
                        }
                    }
                }
                .setNegativeButton(android.R.string.cancel) { dialog, _ -> dialog.cancel() }
                .create()
    }

    interface TimeDialogListener {

        fun onCustomTimeSelected(customTimeKey: CustomTimeKey<*>)

        fun onOtherSelected()

        fun onAddSelected()
    }

    @Parcelize
    class CustomTimeData(val customTimeKey: CustomTimeKey<*>, val name: String) : Parcelable {

        init {
            check(!TextUtils.isEmpty(name))
        }
    }
}
