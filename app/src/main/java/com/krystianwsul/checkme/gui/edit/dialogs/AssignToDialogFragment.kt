package com.krystianwsul.checkme.gui.edit.dialogs

import android.app.Dialog
import android.os.Bundle
import android.os.Parcelable
import android.text.TextUtils
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.jakewharton.rxrelay3.BehaviorRelay
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.gui.base.AbstractDialogFragment
import com.krystianwsul.checkme.gui.edit.EditViewModel
import com.krystianwsul.common.utils.CustomTimeKey
import com.krystianwsul.common.utils.UserKey
import io.reactivex.rxjava3.kotlin.plusAssign
import kotlinx.parcelize.Parcelize
import java.util.*

class AssignToDialogFragment : AbstractDialogFragment() {

    companion object {

        private const val KEY_USER_DATAS = "userDatas"
        private const val KEY_CHECKED = "checked"

        fun newInstance(
                userDatas: List<EditViewModel.UserData>,
                checked: List<UserKey>,
        ) = AssignToDialogFragment().apply {
            arguments = Bundle().apply {
                putParcelableArrayList(KEY_USER_DATAS, ArrayList(userDatas))
                putParcelableArrayList(KEY_CHECKED, ArrayList(checked))
            }
        }
    }

    lateinit var listener: (Set<UserKey>) -> Unit

    private val buttonEnabledRelay = BehaviorRelay.createDefault(true)

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val userDatas = requireArguments().getParcelableArrayList<EditViewModel.UserData>(KEY_USER_DATAS)!!
        val checked = requireArguments().getParcelableArrayList<UserKey>(KEY_CHECKED)!!.toMutableSet()

        val checkedArr = userDatas.map { it.key in checked }.toBooleanArray()

        return MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.assignTask)
                .setMultiChoiceItems(userDatas.map { it.name }.toTypedArray(), checkedArr) { _, which, isChecked ->
                    val userKey = userDatas[which].key

                    checked.apply { if (isChecked) add(userKey) else remove(userKey) }

                    buttonEnabledRelay.accept(checked.isNotEmpty())
                }
                .setNegativeButton(android.R.string.cancel) { dialog, _ -> dialog.cancel() }
                .setPositiveButton(android.R.string.ok) { _, _ -> listener(checked) }
                .create()
    }

    override fun onStart() {
        super.onStart()

        startDisposable += buttonEnabledRelay.subscribe {
            (requireDialog() as AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = it
        }
    }

    @Parcelize
    class CustomTimeData(val customTimeKey: CustomTimeKey<*>, val name: String) : Parcelable {

        init {
            check(!TextUtils.isEmpty(name))
        }
    }
}
