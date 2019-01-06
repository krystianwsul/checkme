package com.krystianwsul.checkme

import android.util.Log
import com.krystianwsul.checkme.gui.instances.tree.GroupListFragment
import com.krystianwsul.checkme.utils.InstanceKey
import java.util.*

object DataDiff {

    private var sDiff: MutableList<String> = ArrayList()

    val diff get() = sDiff.joinToString("\n")

    fun diffData(oldData: GroupListFragment.DataWrapper, newData: GroupListFragment.DataWrapper) {
        sDiff = ArrayList()

        diffMap("", oldData.instanceDatas, newData.instanceDatas)

        Log.e("asdf", "diff:\n$diff")
    }

    private fun diffMap(indent: String, oldInstanceDatas: Map<InstanceKey, GroupListFragment.InstanceData>, newInstanceDatas: Map<InstanceKey, GroupListFragment.InstanceData>) {
        val instanceKeys = HashSet<InstanceKey>()
        instanceKeys.addAll(oldInstanceDatas.keys)
        instanceKeys.addAll(newInstanceDatas.keys)

        for (instanceKey in instanceKeys) {
            if (!oldInstanceDatas.keys.contains(instanceKey)) {
                sDiff.add(indent + newInstanceDatas[instanceKey]!!.name + " missing from oldData")
                continue
            }

            val oldInstanceData = oldInstanceDatas[instanceKey]
            checkNotNull(oldInstanceData)

            if (!newInstanceDatas.keys.contains(instanceKey)) {
                sDiff.add(indent + oldInstanceData.name + " missing from newData")
                continue
            }

            val newInstanceData = newInstanceDatas[instanceKey]
            checkNotNull(newInstanceData)

            if (oldInstanceData != newInstanceData) {
                sDiff.add(indent + "difference in " + oldInstanceData.name + ":")

                diffInstance(indent + "\t", oldInstanceData, newInstanceData)
            }
        }
    }

    private fun diffInstance(indent: String, oldInstanceData: GroupListFragment.InstanceData, newInstanceData: GroupListFragment.InstanceData) {
        check(indent.isNotEmpty())
        check(oldInstanceData.InstanceKey == newInstanceData.InstanceKey)

        if (oldInstanceData.Done != newInstanceData.Done)
            sDiff.add(indent + "oldInstanceData.done == " + oldInstanceData.Done + ", newInstanceData.done == " + newInstanceData.Done)

        if (oldInstanceData.DisplayText != newInstanceData.DisplayText)
            sDiff.add(indent + "oldInstanceData.displayText == " + oldInstanceData.DisplayText + ", newInstanceData.displayText == " + newInstanceData.DisplayText)

        if (oldInstanceData.name != newInstanceData.name)
            sDiff.add(indent + "oldInstanceData.name == " + oldInstanceData.name + ", newInstanceData.name == " + newInstanceData.name)

        if (oldInstanceData.InstanceTimeStamp != newInstanceData.InstanceTimeStamp)
            sDiff.add(indent + "oldInstanceData.InstanceTimeStamp == " + oldInstanceData.InstanceTimeStamp + ", newInstanceData.InstanceTimeStamp == " + newInstanceData.InstanceTimeStamp)

        if (oldInstanceData.TaskCurrent != newInstanceData.TaskCurrent)
            sDiff.add(indent + "oldInstanceData.taskCurrent == " + oldInstanceData.TaskCurrent + ", newInstanceData.taskCurrent == " + newInstanceData.TaskCurrent)

        if (oldInstanceData.IsRootInstance != newInstanceData.IsRootInstance)
            sDiff.add(indent + "oldInstanceData.isRootInstance == " + oldInstanceData.IsRootInstance + ", newInstanceData.isRootInstance == " + newInstanceData.IsRootInstance)

        if (oldInstanceData.IsRootTask == null != (newInstanceData.IsRootTask == null) || oldInstanceData.IsRootTask != null && oldInstanceData.IsRootTask != newInstanceData.IsRootTask)
            sDiff.add(indent + "oldInstanceData.IsRootTask == " + oldInstanceData.IsRootTask + ", newInstanceData.IsRootTask == " + newInstanceData.IsRootTask)

        if (oldInstanceData.Exists != newInstanceData.Exists)
            sDiff.add(indent + "oldInstanceData.Exists == " + oldInstanceData.Exists + ", newInstanceData.exists == " + newInstanceData.Exists)

        if (oldInstanceData.InstanceTimePair != newInstanceData.InstanceTimePair)
            sDiff.add(indent + "oldInstanceData.instanceTimePair == " + oldInstanceData.InstanceTimePair + ", newInstanceData.instanceTimePair == " + newInstanceData.InstanceTimePair)

        if (oldInstanceData.mNote != newInstanceData.mNote)
            sDiff.add(indent + "oldInstanceData.note == " + oldInstanceData.mNote + ", newInstanceData.note == " + newInstanceData.mNote)

        if (oldInstanceData.children != newInstanceData.children) {
            sDiff.add(indent + "oldInstanceData.children != newInstanceData.children")
            diffMap(indent + "\t", oldInstanceData.children, newInstanceData.children)
        }

        if (oldInstanceData.hierarchyData != newInstanceData.hierarchyData)
            sDiff.add(indent + "oldInstanceData.hierarchyData == " + oldInstanceData.hierarchyData + ", newInstanceData.hierarchyData == " + newInstanceData.hierarchyData)

        if (oldInstanceData.ordinal != newInstanceData.ordinal)
            sDiff.add(indent + "oldInstanceData.ordinal == " + oldInstanceData.ordinal + ", newInstanceData.ordinal == " + newInstanceData.ordinal)
    }
}
