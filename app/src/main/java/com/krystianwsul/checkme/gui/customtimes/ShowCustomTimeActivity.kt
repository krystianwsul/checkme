package com.krystianwsul.checkme.gui.customtimes

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.core.view.isVisible
import androidx.transition.TransitionManager
import com.google.android.material.timepicker.MaterialTimePicker
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.databinding.ActivityShowCustomTimeBinding
import com.krystianwsul.checkme.domainmodel.DomainListenerManager
import com.krystianwsul.checkme.domainmodel.extensions.createCustomTime
import com.krystianwsul.checkme.domainmodel.extensions.updateCustomTime
import com.krystianwsul.checkme.domainmodel.update.AndroidDomainUpdater
import com.krystianwsul.checkme.gui.base.NavBarActivity
import com.krystianwsul.checkme.gui.dialogs.ConfirmDialogFragment
import com.krystianwsul.checkme.gui.dialogs.newMaterialTimePicker
import com.krystianwsul.checkme.gui.dialogs.setListener
import com.krystianwsul.checkme.gui.widgets.MyTextInputLayout
import com.krystianwsul.checkme.utils.tryGetFragment
import com.krystianwsul.checkme.viewmodels.ShowCustomTimeViewModel
import com.krystianwsul.checkme.viewmodels.getViewModel
import com.krystianwsul.common.time.DayOfWeek
import com.krystianwsul.common.time.HourMinute
import com.krystianwsul.common.utils.CustomTimeKey
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.plusAssign
import io.reactivex.rxjava3.kotlin.subscribeBy
import java.util.*

class ShowCustomTimeActivity : NavBarActivity() {

    companion object {

        const val CUSTOM_TIME_KEY = "customTimeKey"
        private const val NEW_KEY = "new"

        private const val KEY_HOUR_MINUTES = "hourMinutes"
        private const val KEY_ALL_DAYS_EXPANDED = "allDaysExpanded"

        private const val TAG_TIME_PICKER = "timePicker"
        private const val TAG_TIME_PICKER_ALL_DAYS = "timePickerAllDays"
        private const val DISCARD_TAG = "discard"

        const val CREATE_CUSTOM_TIME_REQUEST_CODE = 1

        private val defaultHourMinute = HourMinute(9, 0)

        fun getEditIntent(
                customTimeKey: CustomTimeKey.Project.Private,
                context: Context,
        ) = Intent(context, ShowCustomTimeActivity::class.java).apply {
            @Suppress("CAST_NEVER_SUCCEEDS")
            putExtra(CUSTOM_TIME_KEY, customTimeKey as Parcelable)
        }

        fun getCreateIntent(context: Context) = Intent(context, ShowCustomTimeActivity::class.java).apply {
            putExtra(NEW_KEY, true)
        }
    }

    private var customTimeKey: CustomTimeKey.Project.Private? = null

    private var data: ShowCustomTimeViewModel.Data? = null

    private lateinit var timeViews: Map<DayOfWeek, MyTextInputLayout>
    private var hourMinutes = mutableMapOf<DayOfWeek, HourMinute>()

    private var savedInstanceState: Bundle? = null

    private val discardDialogListener: (Parcelable?) -> Unit = { finish() }

    private val timePickerDialogFragmentListener = { hourMinute: HourMinute, dayOfWeek: DayOfWeek ->
        check(timeViews.containsKey(dayOfWeek))
        check(hourMinutes.containsKey(dayOfWeek))

        hourMinutes[dayOfWeek] = hourMinute
        timeViews.getValue(dayOfWeek).setText(hourMinute.toString())

        if (dayOfWeek == DayOfWeek.SUNDAY) binding.timeAllDaysTime.setText(hourMinute.toString())
    }

    private val allDaysListener = { hourMinute: HourMinute ->
        DayOfWeek.values().forEach { dayOfWeek ->
            hourMinutes[dayOfWeek] = hourMinute
            timeViews.getValue(dayOfWeek).setText(hourMinute.toString())
        }

        binding.timeAllDaysTime.setText(hourMinute.toString())
    }

    private var showCustomTimeViewModel: ShowCustomTimeViewModel? = null

    private var allDaysExpanded = false

    override val rootView get() = binding.showCustomTimeRoot

    private lateinit var binding: ActivityShowCustomTimeBinding

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_save, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        menu.findItem(R.id.action_save).isVisible = customTimeKey == null || data != null

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_save -> {
                check(hourMinutes.isNotEmpty())

                updateError()

                val name = binding.showCustomTimeToolbarEditTextInclude
                        .toolbarEditText
                        .text
                        .toString()
                        .trim { it <= ' ' }

                if (name.isNotEmpty()) {
                    showCustomTimeViewModel?.stop()

                    if (data != null) {
                        AndroidDomainUpdater.updateCustomTime(
                                DomainListenerManager.NotificationType.All,
                                data!!.key,
                                name,
                                hourMinutes
                        )
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe { finish() }
                                .addTo(createDisposable)
                    } else {
                        AndroidDomainUpdater.createCustomTime(
                                DomainListenerManager.NotificationType.All,
                                name,
                                hourMinutes,
                        )
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribeBy {
                                    setResult(
                                            Activity.RESULT_OK,
                                            Intent().putExtra(CUSTOM_TIME_KEY, it as Parcelable),
                                    )

                                    finish()
                                }
                                .addTo(createDisposable)
                    }
                }
            }
            android.R.id.home -> if (tryClose()) finish()
            else -> throw UnsupportedOperationException()
        }
        return true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityShowCustomTimeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.showCustomTimeToolbarEditTextInclude.toolbar)

        supportActionBar!!.run {
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.ic_close_white_24dp)
        }

        this.savedInstanceState = savedInstanceState

        timeViews = binding.run {
            listOf(
                    Triple(DayOfWeek.SUNDAY, timeSundayName, timeSundayTime),
                    Triple(DayOfWeek.MONDAY, timeMondayName, timeMondayTime),
                    Triple(DayOfWeek.TUESDAY, timeTuesdayName, timeTuesdayTime),
                    Triple(DayOfWeek.WEDNESDAY, timeWednesdayName, timeWednesdayTime),
                    Triple(DayOfWeek.THURSDAY, timeThursdayName, timeThursdayTime),
                    Triple(DayOfWeek.FRIDAY, timeFridayName, timeFridayTime),
                    Triple(DayOfWeek.SATURDAY, timeSaturdayName, timeSaturdayTime)
            )
        }.associate { (dayOfWeek, name, time) ->
            name.text = dayOfWeek.toString()

            dayOfWeek to time
        }

        if (savedInstanceState?.containsKey(KEY_HOUR_MINUTES) == true) {
            check(hourMinutes.isEmpty())

            @Suppress("UNCHECKED_CAST")
            hourMinutes = savedInstanceState.getSerializable(KEY_HOUR_MINUTES) as HashMap<DayOfWeek, HourMinute>

            allDaysExpanded = savedInstanceState.getBoolean(KEY_ALL_DAYS_EXPANDED)

            updateGui()
        } else {
            if (intent.hasExtra(CUSTOM_TIME_KEY)) {
                check(!intent.hasExtra(NEW_KEY))
            } else {
                check(intent.hasExtra(NEW_KEY))
                check(hourMinutes.isEmpty())

                for (dayOfWeek in DayOfWeek.values())
                    hourMinutes[dayOfWeek] = defaultHourMinute

                updateGui()
            }
        }

        if (intent.hasExtra(CUSTOM_TIME_KEY)) {
            check(!intent.hasExtra(NEW_KEY))

            customTimeKey = intent.getParcelableExtra(CUSTOM_TIME_KEY)

            showCustomTimeViewModel = getViewModel<ShowCustomTimeViewModel>().apply {
                start(customTimeKey!!)

                createDisposable += data.subscribe { onLoadFinished(it) }
            }
        } else {
            check(intent.hasExtra(NEW_KEY))

            setUpAllDaysToggle()
        }

        tryGetFragment<ConfirmDialogFragment>(DISCARD_TAG)?.listener = discardDialogListener

        binding.timeAllDaysTimeLayout.setDropdown {
            newMaterialTimePicker(
                    this,
                    supportFragmentManager,
                    TAG_TIME_PICKER_ALL_DAYS,
                    allDaysHourMinute,
            ).setListener(allDaysListener)
        }

        tryGetFragment<MaterialTimePicker>(TAG_TIME_PICKER_ALL_DAYS)?.setListener(allDaysListener)

        DayOfWeek.values().forEach { dayOfWeek ->
            timeViews.getValue(dayOfWeek).setDropdown {
                newMaterialTimePicker(
                        this,
                        supportFragmentManager,
                        TAG_TIME_PICKER,
                        hourMinutes.getValue(dayOfWeek),
                        dayOfWeek,
                ).setListener(timePickerDialogFragmentListener)
            }
        }

        tryGetFragment<MaterialTimePicker>(TAG_TIME_PICKER)?.setListener(timePickerDialogFragmentListener)

        binding.showCustomTimeToolbarEditTextInclude
                .toolbarEditText
                .addTextChangedListener(object : TextWatcher {
                    private var skip = savedInstanceState != null

                    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) = Unit

                    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) = Unit

                    override fun afterTextChanged(s: Editable) {
                        if (skip) skip = false else updateError()
                    }
                })
    }

    public override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        if (hourMinutes.isNotEmpty()) {
            outState.putSerializable(KEY_HOUR_MINUTES, HashMap(hourMinutes))
            outState.putBoolean(KEY_ALL_DAYS_EXPANDED, allDaysExpanded)
        }
    }

    private val allDaysHourMinute get() = hourMinutes.getValue(DayOfWeek.SUNDAY)

    private fun updateGui() {
        check(hourMinutes.isNotEmpty())

        binding.showCustomTimeToolbarEditTextInclude
                .toolbarLayout
                .visibility = View.VISIBLE

        binding.showCustomTimeContainer.visibility = View.VISIBLE

        TransitionManager.beginDelayedTransition(binding.showCustomTimeContainer)
        binding.timeIndividualDaysLayout.isVisible = allDaysExpanded
        binding.timeAllDaysTimeLayout.isVisible = !allDaysExpanded

        binding.timeAllDaysTime.setText(allDaysHourMinute.toString())

        for (dayOfWeek in DayOfWeek.values()) {
            val timeView = timeViews.getValue(dayOfWeek)
            val hourMinute = hourMinutes.getValue(dayOfWeek)

            timeView.setText(hourMinute.toString())
        }
    }

    private fun setUpAllDaysToggle() {
        if (allDaysExpanded) binding.timeAllDaysTextLayout.setChecked(true)

        binding.timeAllDaysTextLayout.setDropdown {
            allDaysExpanded = !allDaysExpanded
            binding.timeAllDaysTextLayout.setChecked(allDaysExpanded)

            if (!allDaysExpanded) {
                val hourMinute = hourMinutes.getValue(DayOfWeek.SUNDAY)

                hourMinutes = DayOfWeek.values()
                        .associate { it to hourMinute }
                        .toMutableMap()
            }

            updateGui()
        }
    }

    private fun onLoadFinished(data: ShowCustomTimeViewModel.Data) {
        this.data = data

        if (savedInstanceState?.containsKey(KEY_HOUR_MINUTES) != true) {
            check(hourMinutes.isEmpty())

            binding.showCustomTimeToolbarEditTextInclude
                    .toolbarEditText
                    .setText(data.name)

            hourMinutes = HashMap(data.hourMinutes)

            allDaysExpanded = hourMinutes.values
                    .distinct()
                    .size > 1

            updateGui()

            setUpAllDaysToggle()
        }

        invalidateOptionsMenu()
    }

    override fun onBackPressed() {
        if (tryClose()) super.onBackPressed()
    }

    private fun tryClose() = if (dataChanged()) {
        ConfirmDialogFragment.newInstance(ConfirmDialogFragment.Parameters.Discard).also {
            it.listener = discardDialogListener
            it.show(supportFragmentManager, DISCARD_TAG)
        }

        false
    } else {
        true
    }

    private fun dataChanged(): Boolean {
        val name = binding.showCustomTimeToolbarEditTextInclude
                .toolbarEditText
                .text

        if (customTimeKey == null) {
            check(data == null)

            if (!name.isNullOrEmpty()) return true

            return DayOfWeek.values().any { hourMinutes[it] != defaultHourMinute }
        } else {
            if (data == null) return false

            if (name.toString() != data!!.name) return true

            return DayOfWeek.values().any { hourMinutes[it] != data!!.hourMinutes[it] }
        }
    }

    private fun updateError() {
        binding.showCustomTimeToolbarEditTextInclude
                .toolbarLayout
                .apply {
                    if (binding.showCustomTimeToolbarEditTextInclude.toolbarEditText.text.isNullOrEmpty()) {
                        error = getString(R.string.nameError)
                        setPadding(0, 0, 0, 0)
                    } else {
                        error = null
                    }
                }
    }
}