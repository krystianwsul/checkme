package com.krystianwsul.checkme.gui

import android.app.Dialog
import android.os.Bundle
import android.os.Parcelable
import android.text.TextUtils
import com.afollestad.materialdialogs.MaterialDialog
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.utils.CustomTimeKey
import junit.framework.Assert
import kotlinx.android.parcel.Parcelize
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
        Assert.assertTrue(arguments!!.containsKey(CUSTOM_TIMES_KEY))

        val customTimeDatas = arguments!!.getParcelableArrayList<CustomTimeData>(CUSTOM_TIMES_KEY)!!

        val names = customTimeDatas.map { it.name }.toMutableList()
        names.add(getString(R.string.other))
        names.add(getString(R.string.add))

        return MaterialDialog.Builder(requireActivity())
                .title(R.string.time_dialog_title)
                .items(names)
                .itemsCallback { _, _, which, _ ->
                    Assert.assertTrue(which < names.size)
                    Assert.assertTrue(which < customTimeDatas.size + 2)

                    when {
                        which < customTimeDatas.size -> {
                            val customTimeKey = customTimeDatas[which].customTimeKey
                            timeDialogListener.onCustomTimeSelected(customTimeKey)
                        }
                        which == customTimeDatas.size -> timeDialogListener.onOtherSelected()
                        else -> {
                            Assert.assertTrue(which == customTimeDatas.size + 1)
                            timeDialogListener.onAddSelected()
                        }
                    }
                }
                .show()
    }

    interface TimeDialogListener {

        fun onCustomTimeSelected(customTimeKey: CustomTimeKey)

        fun onOtherSelected()

        fun onAddSelected()
    }

    @Parcelize
    class CustomTimeData(val customTimeKey: CustomTimeKey, val name: String) : Parcelable {

        init {
            Assert.assertTrue(!TextUtils.isEmpty(name))
        }
    }
}
