package com.krystianwsul.checkme.gui.instances.tree

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.common.collect.HashMultimap
import com.jakewharton.rxrelay2.BehaviorRelay
import com.krystianwsul.checkme.DataDiff
import com.krystianwsul.checkme.Preferences
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.gui.*
import com.krystianwsul.checkme.gui.instances.EditInstanceActivity
import com.krystianwsul.checkme.gui.instances.EditInstancesActivity
import com.krystianwsul.checkme.gui.tasks.CreateTaskActivity
import com.krystianwsul.checkme.gui.tasks.ShowTaskActivity
import com.krystianwsul.checkme.persistencemodel.SaveService
import com.krystianwsul.checkme.utils.*
import com.krystianwsul.checkme.utils.time.*
import com.krystianwsul.checkme.utils.time.Date
import com.krystianwsul.treeadapter.TreeModelAdapter
import com.krystianwsul.treeadapter.TreeNode
import com.krystianwsul.treeadapter.TreeNodeCollection
import com.krystianwsul.treeadapter.TreeViewAdapter
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.plusAssign
import kotlinx.android.parcel.Parcelize
import kotlinx.android.synthetic.main.empty_text.view.*
import kotlinx.android.synthetic.main.fragment_group_list.view.*
import java.util.*

class GroupListFragment @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0,
        defStyleRes: Int = 0) : RelativeLayout(context, attrs, defStyleAttr, defStyleRes), FabUser {

    companion object {

        private const val SUPER_STATE_KEY = "superState"
        const val EXPANSION_STATE_KEY = "expansionState"
        private const val LAYOUT_MANAGER_STATE = "layoutManagerState"

        private fun rangePositionToDate(timeRange: MainActivity.TimeRange, position: Int): Date {
            check(position >= 0)

            val calendar = Calendar.getInstance()

            if (position > 0) {
                when (timeRange) {
                    MainActivity.TimeRange.DAY -> calendar.add(Calendar.DATE, position)
                    MainActivity.TimeRange.WEEK -> {
                        calendar.add(Calendar.WEEK_OF_YEAR, position)
                        calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
                    }
                    MainActivity.TimeRange.MONTH -> {
                        calendar.add(Calendar.MONTH, position)
                        calendar.set(Calendar.DAY_OF_MONTH, 1)
                    }
                }
            }

            return Date(calendar)
        }

        private fun nodesToInstanceDatas(treeNodes: List<TreeNode>, includeGroups: Boolean): Set<InstanceData> {
            val instanceDatas = ArrayList<InstanceData>()
            treeNodes.map { it.modelNode }.forEach {
                when (it) {
                    is NotDoneGroupNode -> {
                        if (includeGroups || it.singleInstance())
                            instanceDatas.addAll(it.instanceDatas)
                    }
                    is NotDoneGroupNode.NotDoneInstanceNode -> instanceDatas.add(it.instanceData)
                    is DoneInstanceNode -> instanceDatas.add(it.instanceData)
                    else -> throw IllegalArgumentException()
                }
            }

            return instanceDatas.toSet()
        }

        fun recursiveExists(instanceData: InstanceData) {
            instanceData.Exists = true

            if (instanceData.instanceDataParent is InstanceData) {
                recursiveExists(instanceData.instanceDataParent as InstanceData)
            } else {
                check(instanceData.instanceDataParent is DataWrapper)
            }
        }
    }

    val activity get() = context as AbstractActivity
    val listener get() = context as GroupListListener

    lateinit var treeViewAdapter: TreeViewAdapter
        private set

    private val parametersRelay = BehaviorRelay.create<Parameters>()
    val parameters get() = parametersRelay.value!!

    private var state = State()

    val dragHelper: DragHelper by lazy {
        object : DragHelper() {

            override fun getTreeViewAdapter() = treeViewAdapter

            override fun onSetNewItemPosition() = selectionCallback.actionMode!!.finish()
        }
    }

    var forceSaveStateListener: (() -> Unit)? = null

    val selectionCallback = object : SelectionCallback() {

        override val activity get() = this@GroupListFragment.activity

        override fun getTreeViewAdapter() = treeViewAdapter

        override fun unselect(x: TreeViewAdapter.Placeholder) = treeViewAdapter.unselect(x)

        override val bottomBarData by lazy { Triple(listener.getBottomBar(), R.menu.menu_edit_groups_bottom, listener::initBottomBar) }

        override fun onMenuClick(itemId: Int, x: TreeViewAdapter.Placeholder) {
            val treeNodes = treeViewAdapter.selectedNodes

            val instanceDatas = nodesToInstanceDatas(treeNodes, true)
            check(instanceDatas.isNotEmpty())

            when (itemId) {
                R.id.action_group_edit_instance -> {
                    check(instanceDatas.isNotEmpty())

                    if (instanceDatas.size == 1) {
                        val instanceData = instanceDatas.single()
                        check(instanceData.IsRootInstance)

                        activity.startActivity(EditInstanceActivity.getIntent(instanceData.InstanceKey))
                    } else {
                        check(instanceDatas.size > 1)
                        check(instanceDatas.all { it.IsRootInstance })

                        val instanceKeys = ArrayList(instanceDatas.map { it.InstanceKey })

                        activity.startActivity(EditInstancesActivity.getIntent(instanceKeys))
                    }
                }
                R.id.action_group_share -> Utils.share(activity, getShareData(instanceDatas))
                R.id.action_group_show_task -> {
                    val instanceData = instanceDatas.single()
                    check(instanceData.TaskCurrent)

                    activity.startActivity(ShowTaskActivity.newIntent(instanceData.InstanceKey.taskKey))
                }
                R.id.action_group_edit_task -> {
                    val instanceData = instanceDatas.single()
                    check(instanceData.TaskCurrent)

                    activity.startActivity(CreateTaskActivity.getEditIntent(instanceData.InstanceKey.taskKey))
                }
                R.id.action_group_delete_task -> {
                    val taskKeys = instanceDatas.map { it.InstanceKey.taskKey }
                    check(taskKeys.isNotEmpty())
                    check(instanceDatas.all { it.TaskCurrent })

                    val undoAll = UndoAll()

                    removeFromGetter({ treeViewAdapter.selectedNodes.sortedByDescending { it.indentation } }) {
                        recursiveDelete(it, true, x, undoAll)
                    }

                    val dataId = (treeViewAdapter.treeModelAdapter as GroupAdapter).dataId
                    val taskUndoData = DomainFactory.instance.setTaskEndTimeStamps(dataId, SaveService.Source.GUI, taskKeys)

                    listener.showSnackbar(instanceDatas.size) {
                        fun Map<InstanceKey, InstanceData>.flattenMap(): List<InstanceData> = map {
                            listOf(
                                    listOf(it.value),
                                    it.value
                                            .children
                                            .flattenMap()
                            ).flatten()
                        }.flatten()

                        val allInstanceDatas = parameters.dataWrapper
                                .instanceDatas
                                .flattenMap()
                                .associateBy { it.InstanceKey }

                        undoAll.undos.forEach {
                            allInstanceDatas.getValue(it.key).let { instanceData ->
                                instanceData.TaskCurrent = it.value.taskCurrent
                                instanceData.IsRootTask = it.value.isRootTask
                            }
                        }

                        parameters.dataWrapper
                                .instanceDatas
                                .putAll(undoAll.removedRoots.map { it.InstanceKey to it })

                        undoAll.removedChildren
                                .asMap()
                                .forEach { (instanceKey, instanceDatas) ->
                                    allInstanceDatas.getValue(instanceKey).children.putAll(instanceDatas.map { it.InstanceKey to it })
                                }

                        initialize()

                        DomainFactory.instance.clearTaskEndTimeStamps(dataId, SaveService.Source.GUI, taskUndoData)
                    }
                }
                R.id.action_group_add_task -> {
                    val instanceData = instanceDatas.single()
                    check(instanceData.TaskCurrent)

                    activity.startActivity(CreateTaskActivity.getCreateIntent(instanceData.InstanceKey.taskKey))
                }
                R.id.action_group_join -> {
                    val taskKeys = ArrayList(instanceDatas.map { it.InstanceKey.taskKey })
                    check(taskKeys.size > 1)

                    if (parameters is Parameters.InstanceKey) {
                        activity.startActivity(CreateTaskActivity.getJoinIntent(taskKeys, (parameters as Parameters.InstanceKey).instanceKey.taskKey))
                    } else {
                        val firstInstanceData = instanceDatas.minBy { it.instanceTimeStamp }!!

                        val date = firstInstanceData.instanceTimeStamp.date

                        val timePair = firstInstanceData.InstanceTimePair

                        activity.startActivity(CreateTaskActivity.getJoinIntent(taskKeys, CreateTaskActivity.ScheduleHint(date, timePair)))
                    }
                }
                R.id.action_group_mark_done -> {
                    check(instanceDatas.all { it.Done == null })

                    val instanceKeys = instanceDatas.map { it.InstanceKey }

                    val done = DomainFactory.instance.setInstancesDone(parameters.dataId, SaveService.Source.GUI, instanceKeys, true)

                    removeFromGetter({ treeViewAdapter.selectedNodes.sortedByDescending { it.indentation } }) { treeNode ->
                        treeNode.modelNode.let {
                            if (it is NotDoneGroupNode) {
                                val nodeCollection = it.nodeCollection

                                nodeCollection.notDoneGroupCollection.remove(it, x)

                                if (!it.expanded()) {
                                    it.instanceDatas.forEach {
                                        it.Done = done

                                        recursiveExists(it)

                                        nodeCollection.dividerNode.add(it, x)
                                    }
                                } else {
                                    check(it.treeNode.allChildren.all { it.isSelected })
                                }

                                decrementSelected(x)
                            } else {
                                val instanceData = (it as NotDoneGroupNode.NotDoneInstanceNode).instanceData
                                instanceData.Done = done

                                recursiveExists(instanceData)

                                it.removeFromParent(x)

                                it.parentNodeCollection.dividerNode.add(instanceData, x)
                            }
                        }
                    }
                }
                R.id.action_group_mark_not_done -> {
                    check(instanceDatas.all { it.Done != null })

                    val instanceKeys = instanceDatas.map { it.InstanceKey }

                    DomainFactory.instance.setInstancesDone(parameters.dataId, SaveService.Source.GUI, instanceKeys, false)

                    removeFromGetter({ treeViewAdapter.selectedNodes.sortedByDescending { it.indentation } }) { treeNode ->
                        treeNode.modelNode.let {
                            val instanceData = (it as DoneInstanceNode).instanceData
                            instanceData.Done = null

                            recursiveExists(instanceData)

                            it.removeFromParent(x)

                            it.dividerNode
                                    .nodeCollection
                                    .notDoneGroupCollection
                                    .add(instanceData, x)
                        }
                    }
                }
                R.id.action_group_notify -> {
                    instanceDatas.all { it.IsRootInstance && it.Done == null && it.instanceTimeStamp <= TimeStamp.now && !it.notificationShown }

                    val instanceKeys = instanceDatas.map { it.InstanceKey }

                    DomainFactory.instance.setInstancesNotNotified(parameters.dataId, SaveService.Source.GUI, instanceKeys)

                    instanceDatas.forEach { it.notificationShown = true }
                }
                else -> throw UnsupportedOperationException()
            }
        }

        private fun recursiveDelete(treeNode: TreeNode, root: Boolean, x: TreeViewAdapter.Placeholder, undoAll: UndoAll) {
            check(root == treeNode.isSelected)

            treeNode.modelNode.let {
                val instanceData = when (it) {
                    is NotDoneGroupNode -> {
                        check(root)

                        if (it.singleInstance()) {
                            it.singleInstanceData
                        } else {
                            treeNode.allChildren
                                    .first()
                                    .let { recursiveDelete(it, false, x, undoAll) }

                            return
                        }
                    }
                    is NotDoneGroupNode.NotDoneInstanceNode -> it.instanceData
                    is DoneInstanceNode -> it.instanceData
                    is DividerNode -> {
                        check(!root)

                        treeNode.allChildren.forEach { recursiveDelete(it, false, x, undoAll) }

                        return
                    }
                    else -> throw IllegalArgumentException()
                }

                if (instanceData.Exists) {
                    if (instanceData.TaskCurrent) {
                        check(instanceData.TaskCurrent)
                        checkNotNull(instanceData.IsRootTask)

                        check(!undoAll.undos.containsKey(instanceData.InstanceKey))

                        // mark gray
                        undoAll.undos[instanceData.InstanceKey] = Undo(instanceData.TaskCurrent, instanceData.IsRootTask!!)

                        instanceData.TaskCurrent = false
                        instanceData.IsRootTask = null

                        treeNode.deselect(x)
                    } else {
                        check(!root)
                    }

                    treeNode.allChildren.forEach { recursiveDelete(it, false, x, undoAll) }
                } else {
                    // remove from tree
                    instanceData.instanceDataParent.remove(instanceData, undoAll)

                    when (it) {
                        is NotDoneGroupNode -> it.removeFromParent(x)
                        is NotDoneGroupNode.NotDoneInstanceNode -> it.removeFromParent(x)
                        is DoneInstanceNode -> it.removeFromParent(x)
                        else -> throw IllegalArgumentException()
                    }
                }
            }
        }

        override fun onFirstAdded(x: TreeViewAdapter.Placeholder) {
            (activity as AppCompatActivity).startSupportActionMode(this)

            actionMode!!.menuInflater.inflate(R.menu.menu_edit_groups_top, actionMode!!.menu)

            listener.onCreateGroupActionMode(actionMode!!, treeViewAdapter)

            super.onFirstAdded(x)
        }

        override fun onLastRemoved(x: TreeViewAdapter.Placeholder) = listener.onDestroyGroupActionMode()

        override fun getItemVisibilities(): List<Pair<Int, Boolean>> {
            checkNotNull(actionMode)

            val instanceDatas = nodesToInstanceDatas(treeViewAdapter.selectedNodes, true)
            check(instanceDatas.isNotEmpty())

            val itemVisibilities = mutableListOf(
                    R.id.action_group_mark_done to instanceDatas.all { it.Done == null },
                    R.id.action_group_mark_not_done to instanceDatas.all { it.Done != null },
                    R.id.action_group_edit_instance to instanceDatas.all { it.IsRootInstance && it.Done == null },
                    R.id.action_group_notify to instanceDatas.all { it.IsRootInstance && it.Done == null && it.instanceTimeStamp <= TimeStamp.now && !it.notificationShown }
            )

            if (instanceDatas.size == 1) {
                val instanceData = instanceDatas.single()

                itemVisibilities.addAll(listOf(
                        R.id.action_group_show_task to instanceData.TaskCurrent,
                        R.id.action_group_edit_task to instanceData.TaskCurrent,
                        R.id.action_group_join to false,
                        R.id.action_group_delete_task to instanceData.TaskCurrent,
                        R.id.action_group_add_task to instanceData.TaskCurrent
                ))
            } else {
                check(instanceDatas.size > 1)

                itemVisibilities.addAll(listOf(
                        R.id.action_group_show_task to false,
                        R.id.action_group_edit_task to false,
                        R.id.action_group_add_task to false
                ))

                if (instanceDatas.all { it.TaskCurrent }) {
                    val projectIdCount = instanceDatas.asSequence()
                            .map { it.InstanceKey.taskKey.remoteProjectId }
                            .distinct()
                            .count()

                    check(projectIdCount > 0)

                    itemVisibilities.addAll(listOf(
                            R.id.action_group_join to (projectIdCount == 1),
                            R.id.action_group_delete_task to !containsLoop(instanceDatas)
                    ))
                } else {
                    itemVisibilities.addAll(listOf(
                            R.id.action_group_join to false,
                            R.id.action_group_delete_task to false
                    ))
                }
            }

            return itemVisibilities
        }

        private fun containsLoop(instanceDatas: Collection<InstanceData>): Boolean {
            check(instanceDatas.size > 1)

            for (instanceData in instanceDatas) {
                val parents = ArrayList<InstanceData>()
                addParents(parents, instanceData)

                parents.forEach {
                    if (instanceDatas.contains(it))
                        return true
                }
            }

            return false
        }

        private fun addParents(parents: MutableList<InstanceData>, instanceData: InstanceData) {
            if (instanceData.instanceDataParent !is InstanceData)
                return

            val parent = instanceData.instanceDataParent as InstanceData

            parents.add(parent)
            addParents(parents, parent)
        }

        override fun getTitleCount() = nodesToInstanceDatas(treeViewAdapter.selectedNodes, true).size

        override fun onDestroyActionMode(mode: ActionMode) {
            super.onDestroyActionMode(mode)

            forceSaveStateListener?.invoke()
        }
    }

    private var floatingActionButton: FloatingActionButton? = null

    val shareData: String?
        get() {
            val instanceDatas = parameters.dataWrapper
                    .instanceDatas
                    .values
                    .sorted()

            val lines = mutableListOf<String>()

            for (instanceData in instanceDatas)
                printTree(lines, 1, instanceData)

            return lines.joinToString("\n")
        }

    private val compositeDisposable = CompositeDisposable()

    private val receiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {
            setGroupMenuItemVisibility()
            updateFabVisibility()
        }
    }

    private fun getShareData(instanceDatas: Collection<InstanceData>): String {
        check(instanceDatas.isNotEmpty())

        val tree = LinkedHashMap<InstanceKey, InstanceData>()

        instanceDatas.filterNot { inTree(tree, it) }.forEach { tree[it.InstanceKey] = it }

        val lines = mutableListOf<String>()

        for (instanceData in tree.values)
            printTree(lines, 0, instanceData)

        return lines.joinToString("\n")
    }

    private fun inTree(shareTree: Map<InstanceKey, InstanceData>, instanceData: InstanceData): Boolean = if (shareTree.containsKey(instanceData.InstanceKey)) true else shareTree.values.any { inTree(it.children, instanceData) }

    private fun printTree(lines: MutableList<String>, indentation: Int, instanceData: InstanceData) {
        lines.add("-".repeat(indentation) + instanceData.name)

        instanceData.children
                .values
                .sorted()
                .forEach { printTree(lines, indentation + 1, it) }
    }

    init {
        check(context is GroupListListener)

        inflate(context, R.layout.fragment_group_list, this)

        groupListRecycler.layoutManager = LinearLayoutManager(context)
    }

    public override fun onRestoreInstanceState(state: Parcelable) {
        if (state is Bundle) {
            state.apply {
                classLoader = State::class.java.classLoader
                if (containsKey(EXPANSION_STATE_KEY))
                    this@GroupListFragment.state = getParcelable(EXPANSION_STATE_KEY)!!

                groupListRecycler.layoutManager!!.onRestoreInstanceState(state.getParcelable(LAYOUT_MANAGER_STATE))
            }

            super.onRestoreInstanceState(state.getParcelable(SUPER_STATE_KEY))
        } else {
            super.onRestoreInstanceState(state)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        val observable = activity.started.switchMap {
            if (it) {
                parametersRelay
            } else {
                Observable.never<Parameters>()
            }
        }

        compositeDisposable += observable.subscribe { initialize() }

        observable.reduce { one, two ->
            DataDiff.diffData(one.dataWrapper, two.dataWrapper)
            two
        }
                .subscribe()
                .addTo(compositeDisposable)

        activity.startTicks(receiver)
    }

    override fun onDetachedFromWindow() {
        compositeDisposable.clear()

        activity.unregisterReceiver(receiver)

        super.onDetachedFromWindow()
    }

    fun setAll(timeRange: MainActivity.TimeRange, position: Int, dataId: Int, dataWrapper: DataWrapper) {
        check(position >= 0)

        val differentPage = (parametersRelay.value as? Parameters.All)?.let { it.timeRange != timeRange || it.position != position }
                ?: false

        parametersRelay.accept(Parameters.All(dataId, dataWrapper, position, timeRange, differentPage))
    }

    fun setTimeStamp(timeStamp: TimeStamp, dataId: Int, dataWrapper: DataWrapper) = parametersRelay.accept(Parameters.TimeStamp(dataId, dataWrapper, timeStamp))

    fun setInstanceKey(instanceKey: InstanceKey, dataId: Int, dataWrapper: DataWrapper) = parametersRelay.accept(Parameters.InstanceKey(dataId, dataWrapper, instanceKey))

    fun setInstanceKeys(dataId: Int, dataWrapper: DataWrapper) = parametersRelay.accept(Parameters.InstanceKeys(dataId, dataWrapper))

    fun setTaskKey(taskKey: TaskKey, dataId: Int, dataWrapper: DataWrapper) = parametersRelay.accept(Parameters.TaskKey(dataId, dataWrapper, taskKey))

    private fun useGroups() = parameters is Parameters.All

    public override fun onSaveInstanceState(): Bundle {
        return Bundle().apply {
            putParcelable(SUPER_STATE_KEY, super.onSaveInstanceState())

            if (this@GroupListFragment::treeViewAdapter.isInitialized)
                putParcelable(EXPANSION_STATE_KEY, (treeViewAdapter.treeModelAdapter as GroupAdapter).state)

            putParcelable(LAYOUT_MANAGER_STATE, groupListRecycler.layoutManager!!.onSaveInstanceState())
        }
    }

    private fun initialize() {
        if (this::treeViewAdapter.isInitialized && (parameters as? Parameters.All)?.differentPage != true) {
            state = (treeViewAdapter.treeModelAdapter as GroupAdapter).state

            treeViewAdapter.updateDisplayedNodes(true) {
                (treeViewAdapter.treeModelAdapter as GroupAdapter).initialize(parameters.dataId, parameters.dataWrapper.customTimeDatas, useGroups(), parameters.dataWrapper.instanceDatas.values, state, parameters.dataWrapper.taskDatas, parameters.dataWrapper.note)
                selectionCallback.setSelected(treeViewAdapter.selectedNodes.size, TreeViewAdapter.Placeholder)
            }
        } else {
            val groupAdapter = GroupAdapter(this)
            groupAdapter.initialize(parameters.dataId, parameters.dataWrapper.customTimeDatas, useGroups(), parameters.dataWrapper.instanceDatas.values, state, parameters.dataWrapper.taskDatas, parameters.dataWrapper.note)
            treeViewAdapter = groupAdapter.treeViewAdapter
            groupListRecycler.adapter = treeViewAdapter

            treeViewAdapter.updates
                    .subscribe {
                        setGroupMenuItemVisibility()
                        updateFabVisibility()
                    }
                    .addTo(compositeDisposable)

            dragHelper.attachToRecyclerView(groupListRecycler)

            treeViewAdapter.updateDisplayedNodes {
                selectionCallback.setSelected(treeViewAdapter.selectedNodes.size, TreeViewAdapter.Placeholder)
            }
        }

        val emptyTextId = when (val parameters = parameters) {
            is Parameters.All -> R.string.instances_empty_root
            is Parameters.InstanceKey -> if (parameters.dataWrapper.taskEditable!!) {
                R.string.empty_child
            } else {
                R.string.empty_disabled
            }
            else -> null
        }

        val hide = mutableListOf<View>(groupListProgress)
        val show = mutableListOf<View>()

        if (parameters.dataWrapper.instanceDatas.isEmpty() && parameters.dataWrapper.note.isNullOrEmpty() && parameters.dataWrapper.taskDatas.isNullOrEmpty()) {
            hide.add(groupListRecycler)

            if (emptyTextId != null) {
                show.add(emptyText)
                emptyText.setText(emptyTextId)
            } else {
                hide.add(emptyText)
            }
        } else {
            show.add(groupListRecycler)
            hide.add(emptyText)
        }

        animateVisibility(show, hide)

        setGroupMenuItemVisibility()
        updateFabVisibility()
    }

    private fun setGroupMenuItemVisibility() {
        listener.apply {
            val position = (parametersRelay.value as? Parameters.All)?.position

            if (this@GroupListFragment::treeViewAdapter.isInitialized)
                setGroupMenuItemVisibility(
                        position,
                        treeViewAdapter.displayedNodes.any { it.modelNode.isSelectable },
                        canAddHour())
            else
                setGroupMenuItemVisibility(position, selectAllVisible = false, addHourVisible = false)
        }
    }

    private fun canAddHour() = parameters.dataWrapper
            .instanceDatas
            .values
            .run {
                val now = ExactTimeStamp.now
                all { it.instanceTimeStamp.toExactTimeStamp() < now }
            }

    fun addHour(@Suppress("UNUSED_PARAMETER") x: TreeViewAdapter.Placeholder) {
        check(canAddHour())

        parameters.dataWrapper
                .instanceDatas
                .run {
                    val instanceDateTime = DomainFactory.instance.setInstancesAddHourActivity(parameters.dataId, SaveService.Source.GUI, keys)
                    val instanceTimeStamp = instanceDateTime.timeStamp
                    val displayText = instanceDateTime.getDisplayText()

                    values.forEach {
                        it.instanceTimeStamp = instanceTimeStamp
                        if (it.IsRootInstance)
                            it.displayText = displayText
                    }

                    setGroupMenuItemVisibility()
                }
    }

    fun selectAll(x: TreeViewAdapter.Placeholder) = treeViewAdapter.selectAll(x)

    override fun setFab(floatingActionButton: FloatingActionButton) {
        this.floatingActionButton = floatingActionButton

        floatingActionButton.setOnClickListener {
            check(showFab())

            when (val parameters = parameters) {
                is Parameters.All -> {
                    val actionMode = selectionCallback.actionMode

                    if (actionMode != null) {
                        nodesToInstanceDatas(treeViewAdapter.selectedNodes, true).let {
                            (it.firstOrNull { it.InstanceTimePair.customTimeKey != null }
                                    ?: it.first()).let {
                                activity.startActivity(CreateTaskActivity.getCreateIntent(activity, CreateTaskActivity.ScheduleHint(it.instanceTimeStamp.date, it.InstanceTimePair)))
                            }
                        }

                        actionMode.finish()
                    } else {
                        activity.startActivity(CreateTaskActivity.getCreateIntent(activity, CreateTaskActivity.ScheduleHint(rangePositionToDate(parameters.timeRange, parameters.position))))
                    }
                }
                is Parameters.TimeStamp -> activity.startActivity(CreateTaskActivity.getCreateIntent(activity, CreateTaskActivity.ScheduleHint(parameters.timeStamp.date, parameters.timeStamp.hourMinute)))
                is Parameters.InstanceKey -> activity.startActivity(CreateTaskActivity.getCreateIntent(parameters.instanceKey.taskKey))
                else -> throw IllegalStateException()
            }
        }

        updateFabVisibility()
    }

    private fun showFab() = when (val parameters = parameters) {
        is Parameters.All -> {
            if (selectionCallback.hasActionMode) {
                nodesToInstanceDatas(treeViewAdapter.selectedNodes, true).filter { it.IsRootInstance }
                        .map { it.instanceTimeStamp }
                        .distinct()
                        .singleOrNull()
                        ?.takeIf { it > TimeStamp.now } != null
            } else {
                true
            }
        }
        is Parameters.TimeStamp -> (parameters.timeStamp > TimeStamp.now) && !selectionCallback.hasActionMode
        is Parameters.InstanceKey -> parameters.dataWrapper.taskEditable!! && !selectionCallback.hasActionMode
        else -> false
    }

    private fun updateFabVisibility() {
        floatingActionButton?.apply {
            if (parametersRelay.hasValue() && showFab()) {
                show()
            } else {
                hide()
            }
        }
    }

    override fun clearFab() {
        floatingActionButton = null
    }

    class GroupAdapter(val groupListFragment: GroupListFragment) : TreeModelAdapter, NodeCollectionParent {

        companion object {

            const val TYPE_GROUP = 0
        }

        val treeViewAdapter = TreeViewAdapter(this, R.layout.row_group_list_fab_padding)

        lateinit var treeNodeCollection: TreeNodeCollection
            private set

        private lateinit var nodeCollection: NodeCollection

        val state: State
            get() {
                val expandedGroups = nodeCollection.expandedGroups

                val expandedInstances = mutableMapOf<InstanceKey, Boolean>()
                nodeCollection.addExpandedInstances(expandedInstances)

                val doneExpanded = nodeCollection.doneExpanded
                val unscheduledExpanded = nodeCollection.unscheduledExpanded
                val expandedTaskKeys = nodeCollection.expandedTaskKeys

                val selectedNodes = treeViewAdapter.selectedNodes
                val selectedInstances = nodesToInstanceDatas(selectedNodes, false).map { it.InstanceKey }
                val selectedGroups = selectedNodes.map { it.modelNode }
                        .filterIsInstance<NotDoneGroupNode>().filterNot { it.singleInstance() }
                        .map { it.exactTimeStamp.long }

                return State(doneExpanded, expandedGroups, expandedInstances, unscheduledExpanded, expandedTaskKeys, selectedInstances, selectedGroups)
            }

        var dataId = -1
            private set

        lateinit var customTimeDatas: List<CustomTimeData>
            private set

        fun initialize(dataId: Int, customTimeDatas: List<CustomTimeData>, useGroups: Boolean, instanceDatas: Collection<InstanceData>, state: GroupListFragment.State, taskDatas: List<TaskData>, note: String?) {
            this.dataId = dataId
            this.customTimeDatas = customTimeDatas

            treeNodeCollection = TreeNodeCollection(treeViewAdapter) {
                Preferences.logLineDate("logging ordinals")
                it.forEach { Preferences.logLineHour(it) }
            }

            nodeCollection = NodeCollection(0, this, useGroups, treeNodeCollection, note)

            treeNodeCollection.nodes = nodeCollection.initialize(instanceDatas, state.expandedGroups, state.expandedInstances, state.doneExpanded, state.selectedInstances, state.selectedGroups, taskDatas, state.unscheduledExpanded, state.expandedTaskKeys)
            treeViewAdapter.setTreeNodeCollection(treeNodeCollection)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = NodeHolder(LayoutInflater.from(parent.context).inflate(R.layout.row_list, parent, false))

        override val hasActionMode get() = groupListFragment.selectionCallback.hasActionMode

        override fun incrementSelected(x: TreeViewAdapter.Placeholder) = groupListFragment.selectionCallback.incrementSelected(x)

        override fun decrementSelected(x: TreeViewAdapter.Placeholder) = groupListFragment.selectionCallback.decrementSelected(x)

        override val groupAdapter = this
    }

    @Parcelize
    data class State(
            val doneExpanded: Boolean = false,
            val expandedGroups: List<TimeStamp> = listOf(),
            val expandedInstances: Map<InstanceKey, Boolean> = mapOf(),
            val unscheduledExpanded: Boolean = false,
            val expandedTaskKeys: List<TaskKey> = listOf(),
            val selectedInstances: List<InstanceKey> = listOf(),
            val selectedGroups: List<Long> = listOf()) : Parcelable

    interface GroupListListener : SnackbarListener {

        fun onCreateGroupActionMode(actionMode: ActionMode, treeViewAdapter: TreeViewAdapter)

        fun onDestroyGroupActionMode()

        fun setGroupMenuItemVisibility(position: Int?, selectAllVisible: Boolean, addHourVisible: Boolean)

        fun getBottomBar(): MyBottomBar

        fun initBottomBar()
    }

    data class DataWrapper(
            val customTimeDatas: List<CustomTimeData>,
            val taskEditable: Boolean?,
            val taskDatas: List<TaskData>,
            val note: String?,
            val instanceDatas: MutableMap<InstanceKey, InstanceData>) : InstanceDataParent {

        override fun remove(instanceData: InstanceData, undoAll: UndoAll) {
            val instanceKey = instanceData.InstanceKey
            check(instanceDatas.containsKey(instanceKey))

            instanceDatas.remove(instanceKey)

            undoAll.removedRoots.add(instanceData)
        }
    }

    data class InstanceData(
            var Done: ExactTimeStamp?,
            val InstanceKey: InstanceKey,
            var displayText: String?,
            val name: String,
            var instanceTimeStamp: TimeStamp,
            var TaskCurrent: Boolean,
            val IsRootInstance: Boolean,
            var IsRootTask: Boolean?,
            var Exists: Boolean,
            val InstanceTimePair: TimePair,
            val note: String?,
            val children: MutableMap<InstanceKey, InstanceData>,
            val hierarchyData: HierarchyData?,
            var ordinal: Double,
            var notificationShown: Boolean) : InstanceDataParent, Comparable<InstanceData> {

        lateinit var instanceDataParent: InstanceDataParent

        init {
            check(name.isNotEmpty())
        }

        override fun remove(instanceData: InstanceData, undoAll: UndoAll) {
            val instanceKey = instanceData.InstanceKey
            check(children.containsKey(instanceKey))

            children.remove(instanceKey)

            undoAll.removedChildren.put(InstanceKey, instanceData)
        }

        override fun compareTo(other: InstanceData): Int {
            val timeStampComparison = instanceTimeStamp.compareTo(other.instanceTimeStamp)
            if (timeStampComparison != 0)
                return timeStampComparison

            return if (hierarchyData != null) {
                checkNotNull(other.hierarchyData)

                hierarchyData.ordinal.compareTo(other.hierarchyData.ordinal)
            } else {
                check(other.hierarchyData == null)

                ordinal.compareTo(other.ordinal)
            }
        }
    }

    data class CustomTimeData(val Name: String, val HourMinutes: TreeMap<DayOfWeek, HourMinute>) {

        init {
            check(Name.isNotEmpty())
            check(HourMinutes.size == 7)
        }
    }

    interface InstanceDataParent {

        fun remove(instanceData: InstanceData, undoAll: UndoAll)
    }

    data class TaskData(val taskKey: TaskKey, val Name: String, val children: List<TaskData>, val mStartExactTimeStamp: ExactTimeStamp, val mNote: String?) {

        init {
            check(Name.isNotEmpty())
        }
    }

    sealed class Parameters(val dataId: Int, val dataWrapper: DataWrapper) {

        class All(dataId: Int, dataWrapper: DataWrapper, val position: Int, val timeRange: MainActivity.TimeRange, val differentPage: Boolean) : Parameters(dataId, dataWrapper)

        class TimeStamp(dataId: Int, dataWrapper: DataWrapper, val timeStamp: com.krystianwsul.checkme.utils.time.TimeStamp) : Parameters(dataId, dataWrapper)

        class InstanceKey(dataId: Int, dataWrapper: DataWrapper, val instanceKey: com.krystianwsul.checkme.utils.InstanceKey) : Parameters(dataId, dataWrapper)

        class InstanceKeys(dataId: Int, dataWrapper: DataWrapper) : Parameters(dataId, dataWrapper)

        class TaskKey(dataId: Int, dataWrapper: DataWrapper, val taskKey: com.krystianwsul.checkme.utils.TaskKey) : Parameters(dataId, dataWrapper)
    }

    class Undo(val taskCurrent: Boolean, val isRootTask: Boolean)

    class UndoAll {

        val undos = mutableMapOf<InstanceKey, Undo>()

        val removedChildren = HashMultimap.create<InstanceKey, InstanceData>()!!
        val removedRoots = mutableListOf<InstanceData>()
    }
}