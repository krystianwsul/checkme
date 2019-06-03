package com.krystianwsul.checkme.gui.instances

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.View
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.gui.*
import com.krystianwsul.checkme.gui.customtimes.ShowCustomTimeActivity
import com.krystianwsul.checkme.persistencemodel.SaveService
import com.krystianwsul.checkme.utils.CustomTimeKey
import com.krystianwsul.checkme.utils.InstanceKey
import com.krystianwsul.checkme.utils.fixClicks
import com.krystianwsul.checkme.utils.time.*
import com.krystianwsul.checkme.utils.time.Date
import com.krystianwsul.checkme.viewmodels.EditInstancesViewModel
import com.krystianwsul.checkme.viewmodels.getViewModel
import io.reactivex.rxkotlin.addTo
import kotlinx.android.synthetic.main.fragment_edit_instances.view.*
import java.util.*

class EditInstancesFragment : NoCollapseBottomSheetDialogFragment() {

    companion object {

        private const val INSTANCE_KEYS = "instanceKeys"

        private const val DATE_KEY = "date"
        private const val TIME_PAIR_PERSIST_KEY = "timePairPersist"
        private const val INITIAL_HOUR_MINUTE_KEY = "initialHourMinute"
        private const val INITIAL_DATE_KEY = "initialDate"

        private const val DATE_FRAGMENT_TAG = "dateFragment"
        private const val TIME_FRAGMENT_TAG = "timeFragment"
        private const val TIME_DIALOG_FRAGMENT_TAG = "timeDialogFragment"

        fun newInstance(instanceKeys: List<InstanceKey>) = EditInstancesFragment().apply {
            check(instanceKeys.isNotEmpty())

            arguments = Bundle().apply {
                putParcelableArrayList(INSTANCE_KEYS, ArrayList(instanceKeys))
            }
        }
    }

    private var date: Date? = null
    private var data: EditInstancesViewModel.Data? = null

    private var savedInstanceState: Bundle? = null

    private val broadcastReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            if (data != null)
                updateError()
        }
    }

    private var timePairPersist: TimePairPersist? = null

    private var first = true

    private val timeDialogListener = object : TimeDialogFragment.TimeDialogListener {

        override fun onCustomTimeSelected(customTimeKey: CustomTimeKey<*>) {
            checkNotNull(data)

            timePairPersist!!.customTimeKey = customTimeKey

            updateTimeText()

            updateError()
        }

        override fun onOtherSelected() {
            checkNotNull(data)

            TimePickerDialogFragment.newInstance(timePairPersist!!.hourMinute).also {
                it.listener = timePickerDialogFragmentListener
                it.show(childFragmentManager, TIME_FRAGMENT_TAG)
            }
        }

        override fun onAddSelected() = startActivityForResult(ShowCustomTimeActivity.getCreateIntent(requireContext()), ShowCustomTimeActivity.CREATE_CUSTOM_TIME_REQUEST_CODE)
    }

    private val timePickerDialogFragmentListener = { hourMinute: HourMinute ->
        checkNotNull(data)

        timePairPersist!!.hourMinute = hourMinute
        updateTimeText()
        updateError()
    }

    private val datePickerDialogFragmentListener = { date: Date ->
        this.date = date
        updateDateText()
    }

    private var initialTimePair: TimePair? = null
    private var initialDate: Date? = null

    private lateinit var editInstancesViewModel: EditInstancesViewModel

    private lateinit var myView: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        this.savedInstanceState = savedInstanceState

        val datePickerDialogFragment = childFragmentManager.findFragmentByTag(DATE_FRAGMENT_TAG) as? DatePickerDialogFragment
        datePickerDialogFragment?.listener = datePickerDialogFragmentListener

        if (savedInstanceState != null && savedInstanceState.containsKey(DATE_KEY)) {
            date = savedInstanceState.getParcelable(DATE_KEY)!!

            check(savedInstanceState.containsKey(TIME_PAIR_PERSIST_KEY))
            timePairPersist = savedInstanceState.getParcelable(TIME_PAIR_PERSIST_KEY)!!

            check(savedInstanceState.containsKey(INITIAL_HOUR_MINUTE_KEY))
            initialTimePair = savedInstanceState.getParcelable(INITIAL_HOUR_MINUTE_KEY)!!

            check(savedInstanceState.containsKey(INITIAL_DATE_KEY))
            initialDate = savedInstanceState.getParcelable(INITIAL_DATE_KEY)!!
        }

        val instanceKeys = arguments!!.getParcelableArrayList<InstanceKey>(INSTANCE_KEYS)!!
        check(instanceKeys.isNotEmpty())

        editInstancesViewModel = getViewModel<EditInstancesViewModel>().apply { start(instanceKeys) }
    }

    @SuppressLint("InflateParams")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        myView = requireActivity().layoutInflater
                .inflate(R.layout.fragment_edit_instances, null)
                .apply {
                    editInstanceDate.apply {
                        setOnClickListener {
                            val datePickerDialogFragment = DatePickerDialogFragment.newInstance(date!!)
                            datePickerDialogFragment.listener = datePickerDialogFragmentListener
                            datePickerDialogFragment.show(childFragmentManager, DATE_FRAGMENT_TAG)
                        }

                        fixClicks()
                    }

                    editInstanceTime.fixClicks()

                    editInstanceSave.setOnClickListener {
                        checkNotNull(date)
                        checkNotNull(data)
                        check(isValidDate)
                        check(isValidDateTime)

                        editInstancesViewModel.stop()

                        DomainFactory.instance.setInstancesDateTime(data!!.dataId, SaveService.Source.GUI, data!!.instanceDatas.keys, date!!, timePairPersist!!.timePair)

                        dismiss()
                    }

                    editInstanceCancel.setOnClickListener { dialog!!.cancel() }
                }

        return BottomSheetDialog(requireContext(), R.style.BottomSheetDialogTheme).apply {
            setCancelable(true)
            setContentView(myView)
        }
    }

    override fun onStart() {
        super.onStart()

        editInstancesViewModel.data
                .subscribe { onLoadFinished(it) }
                .addTo(startDisposable)
    }

    override fun onResume() {
        super.onResume()

        requireActivity().registerReceiver(broadcastReceiver, IntentFilter(Intent.ACTION_TIME_TICK))

        if (data != null)
            updateError()
    }

    override fun onPause() {
        requireActivity().unregisterReceiver(broadcastReceiver)

        super.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        if (data != null) {
            checkNotNull(date)
            outState.putParcelable(DATE_KEY, date)

            checkNotNull(timePairPersist)
            outState.putParcelable(TIME_PAIR_PERSIST_KEY, timePairPersist)

            checkNotNull(initialTimePair)
            outState.putParcelable(INITIAL_HOUR_MINUTE_KEY, initialTimePair)

            checkNotNull(initialDate)
            outState.putParcelable(INITIAL_DATE_KEY, initialDate)
        }
    }

    private fun onLoadFinished(data: EditInstancesViewModel.Data) {
        this.data = data

        if (data.instanceDatas.any { it.value.done }) {
            AbstractActivity.setSnackbar(object : SnackbarData {

                override fun show(snackbarListener: SnackbarListener) = snackbarListener.showInstanceMarkedDone()
            })

            dialog!!.cancel()
            return
        }

        myView.editInstanceLayout.visibility = View.VISIBLE

        if (first && (savedInstanceState == null || !savedInstanceState!!.containsKey(DATE_KEY))) {
            check(date == null)
            check(timePairPersist == null)
            check(initialTimePair == null)
            check(initialDate == null)
            check(data.instanceDatas.isNotEmpty())

            first = false

            val dateTime = data.instanceDatas
                    .values
                    .map { it.instanceDateTime }
                    .sorted()
                    .first()

            date = dateTime.date
            timePairPersist = TimePairPersist(dateTime.time.timePair)

            initialTimePair = timePairPersist!!.timePair
            initialDate = date
        }

        updateDateText()

        val timePickerDialogFragment = childFragmentManager.findFragmentByTag(TIME_FRAGMENT_TAG) as? TimePickerDialogFragment
        timePickerDialogFragment?.listener = timePickerDialogFragmentListener

        myView.editInstanceTime.setOnClickListener {
            val customTimeDatas = ArrayList<TimeDialogFragment.CustomTimeData>(data.customTimeDatas.values
                    .filter { it.customTimeKey is CustomTimeKey.Private }
                    .sortedBy { it.hourMinutes[date!!.dayOfWeek] }
                    .map { TimeDialogFragment.CustomTimeData(it.customTimeKey, it.name + " (" + it.hourMinutes[date!!.dayOfWeek] + ")") })

            val timeDialogFragment = TimeDialogFragment.newInstance(customTimeDatas)

            timeDialogFragment.timeDialogListener = timeDialogListener

            timeDialogFragment.show(childFragmentManager, TIME_DIALOG_FRAGMENT_TAG)
        }

        val timeDialogFragment = childFragmentManager.findFragmentByTag(TIME_DIALOG_FRAGMENT_TAG) as? TimeDialogFragment
        timeDialogFragment?.timeDialogListener = timeDialogListener
    }

    private fun updateDateText() {
        checkNotNull(date)

        myView.editInstanceDate.setText(date!!.getDisplayText())

        updateTimeText()

        updateError()
    }

    @SuppressLint("SetTextI18n")
    private fun updateTimeText() {
        checkNotNull(timePairPersist)
        checkNotNull(data)
        checkNotNull(date)

        if (timePairPersist!!.customTimeKey != null) {
            val customTimeData = data!!.customTimeDatas.getValue(timePairPersist!!.customTimeKey!!) // todo crashes on adding time through menu

            myView.editInstanceTime.setText(customTimeData.name + " (" + customTimeData.hourMinutes[date!!.dayOfWeek] + ")")
        } else {
            myView.editInstanceTime.setText(timePairPersist!!.hourMinute.toString())
        }
    }

    private val isValidDate: Boolean
        get() = if (data != null) date!! >= Date.today() else false

    //cached data doesn't contain new custom time
    private val isValidDateTime: Boolean
        get() {
            if (data != null) {
                val hourMinute = if (timePairPersist!!.customTimeKey != null) {
                    if (!data!!.customTimeDatas.containsKey(timePairPersist!!.customTimeKey))
                        return false

                    data!!.customTimeDatas.getValue(timePairPersist!!.customTimeKey!!).hourMinutes[date!!.dayOfWeek]!!
                } else {
                    timePairPersist!!.hourMinute
                }

                return TimeStamp(date!!, hourMinute) > TimeStamp.now
            } else {
                return false
            }
        }

    private fun updateError() {
        if (isValidDate) {
            myView.editInstanceDateLayout.error = null
            myView.editInstanceTimeLayout.error = if (isValidDateTime) null else getString(R.string.error_time)
        } else {
            myView.editInstanceDateLayout.error = getString(R.string.error_date)
            myView.editInstanceTimeLayout.error = null
        }

        myView.editInstanceSave.isEnabled = isValidDateTime
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        check(requestCode == ShowCustomTimeActivity.CREATE_CUSTOM_TIME_REQUEST_CODE)
        checkNotNull(timePairPersist)

        if (resultCode == Activity.RESULT_OK)
            timePairPersist!!.customTimeKey = data!!.getSerializableExtra(ShowCustomTimeActivity.CUSTOM_TIME_ID_KEY) as CustomTimeKey.Private
    }
}
