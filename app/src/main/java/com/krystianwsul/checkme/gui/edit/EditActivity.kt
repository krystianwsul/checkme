package com.krystianwsul.checkme.gui.edit

import android.annotation.SuppressLint
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.CustomItemAnimator
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import com.jakewharton.rxrelay3.BehaviorRelay
import com.jakewharton.rxrelay3.PublishRelay
import com.krystianwsul.checkme.MyApplication
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.databinding.*
import com.krystianwsul.checkme.gui.base.NavBarActivity
import com.krystianwsul.checkme.gui.dialogs.ConfirmDialogFragment
import com.krystianwsul.checkme.gui.edit.delegates.EditDelegate
import com.krystianwsul.checkme.gui.edit.dialogs.AllRemindersDialogFragment
import com.krystianwsul.checkme.gui.edit.dialogs.AssignToDialogFragment
import com.krystianwsul.checkme.gui.edit.dialogs.CameraGalleryFragment
import com.krystianwsul.checkme.gui.edit.dialogs.ParentPickerFragment
import com.krystianwsul.checkme.gui.edit.dialogs.schedule.ScheduleDialogFragment
import com.krystianwsul.checkme.gui.edit.dialogs.schedule.ScheduleDialogParameters
import com.krystianwsul.checkme.gui.edit.dialogs.schedule.ScheduleDialogResult
import com.krystianwsul.checkme.gui.tasks.ShowTaskActivity
import com.krystianwsul.checkme.utils.*
import com.krystianwsul.common.time.Date
import com.krystianwsul.common.time.HourMinute
import com.krystianwsul.common.time.TimePair
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.ScheduleType
import com.krystianwsul.common.utils.TaskKey
import com.krystianwsul.common.utils.UserKey
import com.krystianwsul.treeadapter.FilterCriteria
import com.krystianwsul.treeadapter.getCurrentValue
import com.miguelbcr.ui.rx_paparazzo2.entities.FileData
import com.miguelbcr.ui.rx_paparazzo2.entities.Response
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.BackpressureStrategy
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.merge
import io.reactivex.rxjava3.kotlin.plusAssign
import io.reactivex.rxjava3.kotlin.subscribeBy
import kotlinx.parcelize.Parcelize
import kotlin.properties.Delegates.observable


class EditActivity : NavBarActivity() {

    companion object {

        private const val DISCARD_TAG = "discard"

        const val KEY_PARAMETERS = "parameters"

        const val KEY_PARENT_PROJECT_TYPE = "parentProjectType"
        const val KEY_PARENT_PROJECT_KEY = "parentProjectKey"
        const val KEY_PARENT_TASK = "parentTask"

        private const val PARENT_PICKER_FRAGMENT_TAG = "parentPickerFragment"

        private const val NOTE_KEY = "note"
        private const val NOTE_HAS_FOCUS_KEY = "noteHasFocus"

        private const val SCHEDULE_DIALOG_TAG = "scheduleDialog"
        private const val TAG_CAMERA_GALLERY = "cameraGallery"
        private const val TAG_ALL_REMINDERS = "allReminders"
        private const val TAG_ASSIGN_TO = "assignTo"

        private const val REQUEST_CREATE_PARENT = 982

        fun getParametersIntent(editParameters: EditParameters) = Intent(MyApplication.instance, EditActivity::class.java).apply {
            putExtra(KEY_PARAMETERS, editParameters)
        }

        fun getShortcutIntent(parentTaskKeyHint: TaskKey) = Intent(MyApplication.instance, EditActivity::class.java).apply {
            action = Intent.ACTION_DEFAULT

            putExtra(KEY_PARENT_PROJECT_KEY, (parentTaskKeyHint as TaskKey.Project).projectKey.key) // todo task after model
            putExtra(KEY_PARENT_PROJECT_TYPE, parentTaskKeyHint.projectKey.type.ordinal)
            putExtra(KEY_PARENT_TASK, parentTaskKeyHint.taskId)
        }

        var createdTaskKey: TaskKey? = null
    }

    private var savedInstanceState: Bundle? = null

    private lateinit var parameters: EditParameters

    private lateinit var createTaskAdapter: CreateTaskAdapter

    private val discardDialogListener: (Parcelable?) -> Unit = { finish() }

    private val parentFragmentDelegate = object : ParentPickerFragment.Delegate {

        override val adapterDataObservable by lazy {
            editViewModel.parentPickerData.map { ParentPickerFragment.AdapterData(it.parentTreeDatas) }
        }

        private val queryRelay = BehaviorRelay.create<String>()

        override val filterCriteriaObservable = queryRelay.distinctUntilChanged().map { FilterCriteria.Full(it) }

        override fun onEntrySelected(entryData: ParentPickerFragment.EntryData) {
            editViewModel.delegate
                    .parentScheduleManager
                    .parent = (entryData as EditViewModel.ParentTreeData).toParent()
        }

        override fun onEntryDeleted() {
            editViewModel.delegate
                    .parentScheduleManager
                    .parent = null
        }

        override fun onNewEntry(nameHint: String?) = startActivityForResult(
                getParametersIntent(EditParameters.Create(
                        null,
                        editViewModel.delegate
                                .parentScheduleManager
                                .run {
                                    ParentScheduleState(
                                            schedules.map { ScheduleEntry(it.scheduleDataWrapper) }.toMutableList(),
                                            assignedTo,
                                    )
                                },
                        nameHint,
                )),
                REQUEST_CREATE_PARENT,
        )

        override fun onSearch(query: String) = queryRelay.accept(query)

        override fun onPaddingShown() = throw IllegalStateException()
    }

    private var note: String? = null

    private val noteHasFocusRelay = BehaviorRelay.createDefault(false) // keyboard hack

    private val onChildAttachStateChangeListener = object : RecyclerView.OnChildAttachStateChangeListener { // keyboard hack

        override fun onChildViewAttachedToWindow(view: View) {
            view.findViewById<TextInputEditText>(R.id.noteText)?.let {
                binding.editRecycler.removeOnChildAttachStateChangeListener(this)

                it.requestFocus()

                window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
            }
        }

        override fun onChildViewDetachedFromWindow(view: View) = Unit
    }

    val editViewModel by viewModels<EditViewModel>()

    private val parametersRelay = PublishRelay.create<ScheduleDialogParameters>()

    private val timeRelay = BehaviorRelay.createDefault(Unit) // this is just a trigger to re-check schedule errors

    private val timeReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) = timeRelay.accept(Unit)
    }

    private val allRemindersListener = { allReminders: Boolean -> save(false, allReminders) }

    override val rootView get() = binding.root

    private val noteChanges = PublishRelay.create<Unit>()

    private val imageHeightRelay = BehaviorRelay.create<Int>()

    private lateinit var binding: ActivityEditBinding

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_save, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        menu.findItem(R.id.action_save).isVisible = editViewModel.hasDelegate
        menu.findItem(R.id.action_save_and_open).isVisible = editViewModel.hasDelegate &&
                editViewModel.delegate.showSaveAndOpen

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        fun trySave(andOpen: Boolean) {
            if (updateError()) return

            val showAllRemindersPlural = editViewModel.delegate.showAllRemindersDialog()

            if (showAllRemindersPlural != null) {
                check(!andOpen)

                AllRemindersDialogFragment.newInstance(showAllRemindersPlural)
                        .apply { listener = allRemindersListener }
                        .show(supportFragmentManager, TAG_ALL_REMINDERS)
            } else {
                save(andOpen, true)
            }
        }

        when (item.itemId) {
            R.id.action_save -> trySave(false)
            R.id.action_save_and_open -> trySave(true)
            android.R.id.home -> if (tryClose()) finish()
            else -> throw UnsupportedOperationException()
        }

        return true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityEditBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.editToolbarEditTextInclude.toolbar)

        supportActionBar!!.run {
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.ic_close_white_24dp)
        }

        this.savedInstanceState = savedInstanceState

        binding.editRecycler.layoutManager = LinearLayoutManager(this)

        parameters = EditParameters.fromIntent(intent)

        supportFragmentManager.run {
            @Suppress("UNCHECKED_CAST")
            fun <T : Fragment> find(tag: String) = findFragmentByTag(tag) as? T

            find<ConfirmDialogFragment>(DISCARD_TAG)?.listener = discardDialogListener
            find<AllRemindersDialogFragment>(TAG_ALL_REMINDERS)?.listener = allRemindersListener
        }

        if (!noteHasFocusRelay.value!!)// keyboard hack
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)

        editViewModel.apply {
            start(parameters, this@EditActivity)

            createDisposable += mainData.subscribe { onLoadFinished() }
        }

        hideKeyboardOnClickOutside(binding.root)

        listOfNotNull(
                parametersRelay.toFlowable(BackpressureStrategy.DROP).flatMapSingle(
                        {
                            ScheduleDialogFragment.newInstance(it).run {
                                initialize(editViewModel.delegate.customTimeDatas)
                                show(supportFragmentManager, SCHEDULE_DIALOG_TAG)
                                result.firstOrError()
                            }
                        },
                        false,
                        1,
                ).toObservable(),
                (supportFragmentManager.findFragmentByTag(SCHEDULE_DIALOG_TAG) as? ScheduleDialogFragment)?.let { Observable.just(it) }?.flatMapSingle { it.result.firstOrError() }
        ).merge()
                .subscribe { result ->
                    when (result) {
                        is ScheduleDialogResult.Change -> {
                            if (result.scheduleDialogData.scheduleType == ScheduleType.MONTHLY_DAY) {
                                check(result.scheduleDialogData.monthlyDay)
                            } else if (result.scheduleDialogData.scheduleType == ScheduleType.MONTHLY_WEEK) {
                                check(!result.scheduleDialogData.monthlyDay)
                            }

                            editViewModel.delegate.run {
                                if (result.position == null) {
                                    parentScheduleManager.addSchedule(result.scheduleDialogData.toScheduleEntry())
                                } else {
                                    setSchedule(result.position, result.scheduleDialogData)
                                }
                            }
                        }
                        is ScheduleDialogResult.Delete -> removeSchedule(result.position)
                        is ScheduleDialogResult.Cancel -> Unit
                    }
                }
                .addTo(createDisposable)

        startTicks(timeReceiver)

        Observable.combineLatest(
                keyboardInsetRelay,
                noteHasFocusRelay,
                noteChanges,
                imageHeightRelay,
        ) { keyboardInset, noteHasFocus, _, imageHeight ->
            if (noteHasFocus) {
                binding.editToolbarEditTextInclude
                        .editToolbarAppBar
                        .setExpanded(false)

                val padding = (keyboardInset - imageHeight).coerceAtLeast(0)

                binding.editRecycler.apply {
                    updatePadding(bottom = padding)
                    smoothScrollBy(0, padding)
                }
            } else {
                binding.editRecycler.updatePadding(bottom = 0)
            }
        }
                .subscribe()
                .addTo(createDisposable)

        tryGetFragment<AssignToDialogFragment>(TAG_ASSIGN_TO)?.listener = ::assignTo
    }

    private fun removeSchedule(position: Int) {
        check(position >= 0)

        editViewModel.delegate.removeSchedule(position)
    }

    @SuppressLint("CheckResult")
    fun getImage(single: Observable<Response<EditActivity, FileData>>) {
        single.observeOn(AndroidSchedulers.mainThread()).subscribe {
            if (it.resultCode() == Activity.RESULT_OK) {
                it.targetUI()
                        .editViewModel
                        .setEditImageState(EditImageState.Selected(it.data().file))
            }
        }
    }

    public override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.run {
            if (editViewModel.hasDelegate) {
                putString(NOTE_KEY, note)
                putBoolean(NOTE_HAS_FOCUS_KEY, noteHasFocusRelay.value!!)
            }
        }
    }

    private var first = true

    private fun onLoadFinished() {
        if (!first) return
        first = false

        binding.editToolbarEditTextInclude
                .toolbarLayout
                .run {
                    visibility = View.VISIBLE
                    isHintAnimationEnabled = true
                }

        binding.editToolbarEditTextInclude
                .toolbarEditText
                .run {
                    if (savedInstanceState == null) setText(editViewModel.delegate.initialName)

                    addTextChangedListener(object : TextWatcher {

                        private var skip = savedInstanceState != null

                        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) = Unit

                        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) = Unit

                        override fun afterTextChanged(s: Editable) {
                            if (skip) skip = false else updateNameError()
                        }
                    })
                }

        if (savedInstanceState?.containsKey(NOTE_HAS_FOCUS_KEY) == true) {
            savedInstanceState!!.run {
                check(containsKey(NOTE_KEY))

                note = getString(NOTE_KEY)
                noteHasFocusRelay.accept(getBoolean(NOTE_HAS_FOCUS_KEY))
            }
        } else {
            note = editViewModel.delegate.initialNote
        }

        tryGetFragment<ParentPickerFragment>(PARENT_PICKER_FRAGMENT_TAG)?.initialize(parentFragmentDelegate)

        invalidateOptionsMenu()

        tryGetFragment<ScheduleDialogFragment>(SCHEDULE_DIALOG_TAG)?.initialize(editViewModel.delegate.customTimeDatas)

        createTaskAdapter = CreateTaskAdapter()

        binding.editRecycler.apply {
            adapter = createTaskAdapter
            itemAnimator = CustomItemAnimator()

            if (noteHasFocusRelay.value!!) { // keyboard hack
                addOnChildAttachStateChangeListener(onChildAttachStateChangeListener)

                (layoutManager as LinearLayoutManager).scrollToPosition(createTaskAdapter.notePosition)
            }
        }
    }

    override fun onBackPressed() {
        if (tryClose()) super.onBackPressed()
    }

    private fun tryClose(): Boolean {
        return if (dataChanged()) {
            ConfirmDialogFragment.newInstance(ConfirmDialogFragment.Parameters.Discard).let {
                it.listener = discardDialogListener
                it.show(supportFragmentManager, DISCARD_TAG)
            }

            false
        } else {
            true
        }
    }

    private fun updateNameError(): Boolean {
        val error = getString(R.string.nameError).takeIf {
            binding.editToolbarEditTextInclude
                    .toolbarEditText
                    .text
                    .isNullOrEmpty()
        }

        binding.editToolbarEditTextInclude
                .toolbarLayout
                .error = error

        return error != null
    }

    private fun updateError() = updateNameError() || editViewModel.delegate
            .parentScheduleManager
            .schedules
            .any { editViewModel.delegate.getError(it) != null }
            .also { if (it) timeRelay.accept(Unit) }

    override fun onDestroy() {
        unregisterReceiver(timeReceiver)

        super.onDestroy()
    }

    private fun dataChanged(): Boolean {
        if (!editViewModel.hasDelegate) return false

        return editViewModel.delegate.checkDataChanged(
                editViewModel.editImageState,
                binding.editToolbarEditTextInclude
                        .toolbarEditText
                        .text
                        .toString(),
                note,
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CREATE_PARENT) {
            if (resultCode == Activity.RESULT_OK) {
                val taskKey = data!!.getParcelableExtra<TaskKey>(ShowTaskActivity.TASK_KEY_KEY)!!

                editViewModel.delegate.setParentTask(taskKey)
            }
        }
    }

    private fun save(andOpen: Boolean, allReminders: Boolean) {
        val name = binding.editToolbarEditTextInclude
                .toolbarEditText
                .text
                .toString()
                .trim { it <= ' ' }

        check(name.isNotEmpty())

        editViewModel.stop()

        val createParameters = EditDelegate.CreateParameters(name, note, allReminders, editViewModel.editImageState)

        editViewModel.delegate
                .createTask(createParameters)
                .subscribeBy {
                    if (andOpen) startActivity(it.intent)

                    setResult(
                            Activity.RESULT_OK,
                            Intent().putExtra(ShowTaskActivity.TASK_KEY_KEY, it.taskKey as Parcelable)
                    )

                    finish()
                }
                .addTo(createDisposable)
    }

    private fun assignTo(userKeys: Set<UserKey>) {
        val allUserKeys = editViewModel.delegate
                .parentScheduleManager
                .parent!!
                .projectUsers
                .values
                .map { it.key }
                .toSet()

        val finalUserKeys = userKeys.takeIf { it != allUserKeys } ?: setOf()

        editViewModel.delegate
                .parentScheduleManager
                .assignedTo = finalUserKeys
    }

    sealed class Hint : Parcelable {

        abstract fun toCurrentParent(): EditViewModel.CurrentParentSource

        @Parcelize
        class Schedule(val date: Date, val timePair: TimePair) : Hint() {

            constructor(
                    date: Date,
                    pair: Pair<Date, HourMinute> = HourMinute.getNextHour(date),
            ) : this(pair.first, TimePair(pair.second))

            override fun toCurrentParent() = EditViewModel.CurrentParentSource.None
        }

        @Parcelize
        class Task(val taskKey: TaskKey) : Hint() {

            override fun toCurrentParent() = EditViewModel.CurrentParentSource.Set(EditViewModel.ParentKey.Task(taskKey))
        }

        @Parcelize
        class Project(val projectKey: ProjectKey.Shared) : Hint() {

            override fun toCurrentParent() =
                    EditViewModel.CurrentParentSource.Set(EditViewModel.ParentKey.Project(projectKey))
        }
    }

    enum class HolderType {

        SCHEDULE {

            override fun newHolder(layoutInflater: LayoutInflater, parent: ViewGroup) =
                    ScheduleHolder(RowScheduleBinding.inflate(layoutInflater, parent, false))
        },

        NOTE {

            override fun newHolder(layoutInflater: LayoutInflater, parent: ViewGroup) =
                    NoteHolder(RowNoteBinding.inflate(layoutInflater, parent, false))
        },

        ASSIGNED {

            override fun newHolder(layoutInflater: LayoutInflater, parent: ViewGroup) =
                    AssignedHolder(RowAssignedBinding.inflate(layoutInflater, parent, false))
        },

        IMAGE {

            override fun newHolder(layoutInflater: LayoutInflater, parent: ViewGroup) =
                    ImageHolder(RowImageBinding.inflate(layoutInflater, parent, false))
        };

        abstract fun newHolder(layoutInflater: LayoutInflater, parent: ViewGroup): Holder
    }

    @Suppress("PrivatePropertyName")
    private inner class CreateTaskAdapter : RecyclerView.Adapter<Holder>() {

        private var items: List<Item> by observable(
                editViewModel.delegate
                        .adapterItemObservable
                        .getCurrentValue()
        ) { _, oldItems, newItems ->
            DiffUtil.calculateDiff(object : DiffUtil.Callback() {

                override fun getOldListSize() = oldItems.size

                override fun getNewListSize() = newItems.size

                override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int) =
                        oldItems[oldItemPosition].same(newItems[newItemPosition])

                override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int) =
                        oldItems[oldItemPosition] == newItems[newItemPosition]
            }).dispatchUpdatesTo(this)
        }

        init {
            editViewModel.delegate
                    .adapterItemObservable
                    .subscribe { items = it }
                    .addTo(createDisposable)
        }

        val notePosition get() = items.indexOf(Item.Note)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
                HolderType.values()[viewType].run { newHolder(layoutInflater, parent) }

        override fun onBindViewHolder(holder: Holder, position: Int) =
                items[position].bind(this@EditActivity, holder)

        override fun onViewAttachedToWindow(holder: Holder) {
            super.onViewAttachedToWindow(holder)

            fun getItem() = holder.adapterPosition
                    .takeIf { it >= 0 }
                    ?.let { items[it] }

            editViewModel.editImageStateObservable
                    .subscribe { getItem()?.onNewImageState(it, holder) }
                    .addTo(holder.compositeDisposable)

            editViewModel.delegate
                    .parentScheduleManager
                    .parentObservable
                    .subscribe { getItem()?.onNewParent(this@EditActivity, holder) }
                    .addTo(holder.compositeDisposable)

            editViewModel.delegate
                    .parentScheduleManager
                    .assignedToObservable
                    .subscribe { getItem()?.onNewAssignedTo(this@EditActivity, holder) }
                    .addTo(holder.compositeDisposable)

            holder.compositeDisposable += timeRelay.subscribe {
                getItem()?.onTimeChanged(this@EditActivity, holder)
            }

            createDisposable += holder.compositeDisposable
        }

        override fun onViewDetachedFromWindow(holder: Holder) {
            holder.compositeDisposable.clear()

            createDisposable.remove(holder.compositeDisposable)

            super.onViewDetachedFromWindow(holder)
        }

        override fun getItemCount() = items.size

        override fun getItemViewType(position: Int) = items[position].holderType.ordinal
    }

    abstract class Holder(view: View) : RecyclerView.ViewHolder(view) {

        val compositeDisposable = CompositeDisposable()
    }

    class ScheduleHolder(val rowScheduleBinding: RowScheduleBinding) : Holder(rowScheduleBinding.root)

    class NoteHolder(val rowNoteBinding: RowNoteBinding) : Holder(rowNoteBinding.root)

    class AssignedHolder(val rowAssignedBinding: RowAssignedBinding) : Holder(rowAssignedBinding.root)

    class ImageHolder(val rowImageBinding: RowImageBinding) : Holder(rowImageBinding.root)

    sealed class Item {

        abstract val holderType: HolderType

        abstract fun bind(activity: EditActivity, holder: Holder)

        open fun onNewImageState(imageState: EditImageState, holder: Holder) = Unit

        open fun onNewParent(activity: EditActivity, holder: Holder) = Unit

        open fun onTimeChanged(activity: EditActivity, holder: Holder) = Unit

        open fun onNewAssignedTo(activity: EditActivity, holder: Holder) = Unit

        open fun same(other: Item) = other == this

        object Parent : Item() {

            override val holderType get() = HolderType.SCHEDULE

            override fun bind(activity: EditActivity, holder: Holder) {
                (holder as ScheduleHolder).apply {
                    rowScheduleBinding.apply {
                        scheduleMargin.isVisible = true

                        scheduleLayout.run {
                            hint = activity.getString(R.string.parentTask)
                            error = null
                            isHintAnimationEnabled = false

                            addOneShotGlobalLayoutListener { isHintAnimationEnabled = true }
                        }
                    }

                    onNewParent(activity, this)
                }
            }

            override fun onNewParent(activity: EditActivity, holder: Holder) {
                val parent = activity.editViewModel
                        .delegate
                        .parentScheduleManager
                        .parent

                (holder as ScheduleHolder).rowScheduleBinding.apply {
                    scheduleLayout.apply {
                        fun clickListener() = ParentPickerFragment.newInstance(parent != null, true).let {
                            it.show(activity.supportFragmentManager, PARENT_PICKER_FRAGMENT_TAG)
                            it.initialize(activity.parentFragmentDelegate)
                        }

                        if (parent != null) {
                            setClose(::clickListener) {
                                activity.editViewModel
                                        .delegate
                                        .parentScheduleManager
                                        .parent = null
                            }
                        } else {
                            setDropdown(::clickListener)
                        }
                    }

                    scheduleText.setText(parent?.name)
                }
            }
        }

        data class Schedule(private val scheduleEntry: ScheduleEntry) : Item() {

            override val holderType = HolderType.SCHEDULE

            override fun bind(activity: EditActivity, holder: Holder) {
                (holder as ScheduleHolder).rowScheduleBinding.apply {
                    scheduleMargin.isVisible = false

                    scheduleLayout.run {
                        hint = null
                        isHintAnimationEnabled = false

                        setClose(
                                {
                                    val parameters = ScheduleDialogParameters(
                                            holder.adapterPosition,
                                            scheduleEntry.scheduleDataWrapper.getScheduleDialogData(
                                                    activity.editViewModel
                                                            .delegate
                                                            .scheduleHint
                                            ),
                                            true,
                                    )

                                    activity.parametersRelay.accept(parameters)
                                },
                                { activity.removeSchedule(holder.adapterPosition) }
                        )
                    }

                    scheduleText.setText(scheduleEntry.scheduleDataWrapper.getText(
                            activity.editViewModel
                                    .delegate
                                    .customTimeDatas,
                            activity,
                    ))
                }
            }

            override fun onTimeChanged(activity: EditActivity, holder: Holder) {
                activity.editViewModel
                        .delegate
                        .getError(scheduleEntry)
                        ?.let {
                            (holder as ScheduleHolder).rowScheduleBinding
                                    .scheduleLayout
                                    .error = activity.getString(it.resource)
                        }
            }

            private fun same(other: ScheduleEntry): Boolean {
                if (scheduleEntry.id == other.id) return true

                return scheduleEntry.scheduleDataWrapper === other.scheduleDataWrapper
            }

            override fun same(other: Item): Boolean {
                if (other !is Schedule) return false

                return same(other.scheduleEntry)
            }
        }

        object NewSchedule : Item() {

            override val holderType = HolderType.SCHEDULE

            override fun bind(activity: EditActivity, holder: Holder) {
                (holder as ScheduleHolder).rowScheduleBinding.apply {
                    scheduleMargin.isVisible = false

                    scheduleLayout.run {
                        hint = activity.getString(R.string.addReminder)
                        error = null
                        isHintAnimationEnabled = false

                        setDropdown {
                            val parameters = ScheduleDialogParameters(
                                    null,
                                    activity.editViewModel
                                            .delegate
                                            .firstScheduleEntry
                                            .scheduleDataWrapper
                                            .getScheduleDialogData(activity.editViewModel.delegate.scheduleHint),
                                    false
                            )

                            activity.parametersRelay.accept(parameters)
                        }
                    }

                    scheduleText.text = null
                }
            }
        }

        object Note : Item() {

            private var activity: EditActivity? = null

            private val textListener = object : TextWatcher {

                override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) = Unit

                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) = Unit

                override fun afterTextChanged(s: Editable) {
                    activity?.note = s.toString()
                }
            }

            private val layoutChangeListener = View.OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
                activity?.noteChanges?.accept(Unit)
            }

            override val holderType = HolderType.NOTE

            override fun bind(activity: EditActivity, holder: Holder) {
                this.activity = activity

                (holder as NoteHolder).rowNoteBinding.apply {
                    noteLayout.isHintAnimationEnabled = true

                    noteText.run {
                        setText(activity.note)

                        removeTextChangedListener(textListener)
                        addTextChangedListener(textListener)

                        removeOnLayoutChangeListener(layoutChangeListener)
                        addOnLayoutChangeListener(layoutChangeListener)

                        setOnFocusChangeListener { _, hasFocus -> activity.noteHasFocusRelay.accept(hasFocus) }
                    }
                }
            }
        }

        object Image : Item() {

            private var activity: EditActivity? = null

            private val layoutChangeListener = View.OnLayoutChangeListener { _, _, top, _, bottom, _, _, _, _ ->
                activity?.imageHeightRelay?.accept(bottom - top)
            }

            override val holderType = HolderType.IMAGE

            override fun bind(activity: EditActivity, holder: Holder) {
                this.activity = activity

                (holder as ImageHolder).rowImageBinding.apply {
                    fun listener() = CameraGalleryFragment.newInstance(
                            activity.editViewModel
                                    .editImageState
                                    .loader != null
                    ).show(activity.supportFragmentManager, TAG_CAMERA_GALLERY)

                    imageImage.setOnClickListener { listener() }
                    imageEdit.setOnClickListener { listener() }

                    imageLayout.setDropdown(::listener)

                    root.removeOnLayoutChangeListener(layoutChangeListener)
                    root.addOnLayoutChangeListener(layoutChangeListener)

                    root.updatePadding(bottom = activity.bottomInset)
                }
            }

            override fun onNewImageState(imageState: EditImageState, holder: Holder) {
                (holder as ImageHolder).rowImageBinding.apply {
                    if (imageState.loader != null) {
                        imageProgress.visibility = View.VISIBLE
                        imageImage.visibility = View.VISIBLE
                        imageLayout.visibility = View.GONE
                        imageEdit.visibility = View.VISIBLE

                        imageState.loader!!(imageImage)
                    } else {
                        imageProgress.visibility = View.GONE
                        imageImage.visibility = View.GONE
                        imageLayout.visibility = View.VISIBLE
                        imageEdit.visibility = View.GONE
                    }
                }
            }
        }

        object AssignTo : Item() {

            override val holderType = HolderType.ASSIGNED

            private fun openDialog(editActivity: EditActivity) {
                val projectUsers = editActivity.editViewModel
                        .delegate
                        .parentScheduleManager
                        .parent!!
                        .projectUsers
                        .values
                        .toList()

                AssignToDialogFragment.newInstance(
                        projectUsers,
                        editActivity.editViewModel
                                .delegate
                                .parentScheduleManager
                                .assignedTo
                                .toList()
                                .takeIf { it.isNotEmpty() }
                                ?: projectUsers.map { it.key },
                )
                        .apply { listener = editActivity::assignTo }
                        .show(editActivity.supportFragmentManager, TAG_ASSIGN_TO)
            }

            override fun bind(activity: EditActivity, holder: Holder) {
                viewsMap.clear()

                (holder as AssignedHolder).rowAssignedBinding
                        .assignedLayout
                        .setDropdown { openDialog(activity) }

                onNewAssignedTo(activity, holder)
            }

            private val viewsMap = mutableMapOf<UserKey, View>()

            override fun onNewAssignedTo(activity: EditActivity, holder: Holder) {
                (holder as AssignedHolder).rowAssignedBinding.apply {
                    val users = activity.editViewModel
                            .delegate
                            .parentScheduleManager
                            .assignedToUsers

                    if (users.isEmpty()) {
                        assignedLayout.isVisible = true
                        assignedChipGroup.isVisible = false
                    } else {
                        assignedLayout.isVisible = false
                        assignedChipGroup.isVisible = true

                        val removeUserKeys = viewsMap.keys - users.keys
                        val addUserKeys = users.keys - viewsMap.keys

                        removeUserKeys.forEach {
                            assignedChipGroup.removeView(viewsMap.getValue(it))
                            viewsMap.remove(it)
                        }

                        addUserKeys.forEach { userKey ->
                            val user = users.getValue(userKey)

                            viewsMap[userKey] = RowAssignedChipBinding.inflate(
                                    LayoutInflater.from(root.context),
                                    assignedChipGroup,
                                    true
                            )
                                    .root
                                    .apply {
                                        text = user.name
                                        loadPhoto(user.photoUrl)

                                        setOnClickListener { openDialog(activity) }

                                        setOnCloseIconClickListener {
                                            activity.editViewModel
                                                    .delegate
                                                    .parentScheduleManager
                                                    .removeAssignedTo(user.key)
                                        }
                                    }
                        }
                    }
                }
            }
        }
    }
}
