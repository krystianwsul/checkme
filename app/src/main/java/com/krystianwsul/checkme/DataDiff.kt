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
                sDiff.add(indent + newInstanceDatas.getValue(instanceKey).name + " missing from oldData")
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
        check(oldInstanceData.instanceKey == newInstanceData.instanceKey)

        if (oldInstanceData.done != newInstanceData.done)
            sDiff.add(indent + "oldInstanceData.done == " + oldInstanceData.done + ", newInstanceData.done == " + newInstanceData.done)

        if (oldInstanceData.displayText != newInstanceData.displayText)
            sDiff.add(indent + "oldInstanceData.displayText == " + oldInstanceData.displayText + ", newInstanceData.displayText == " + newInstanceData.displayText)

        if (oldInstanceData.name != newInstanceData.name)
            sDiff.add(indent + "oldInstanceData.name == " + oldInstanceData.name + ", newInstanceData.name == " + newInstanceData.name)

        if (oldInstanceData.instanceTimeStamp != newInstanceData.instanceTimeStamp)
            sDiff.add(indent + "oldInstanceData.instanceTimeStamp == " + oldInstanceData.instanceTimeStamp + ", newInstanceData.instanceTimeStamp == " + newInstanceData.instanceTimeStamp)

        if (oldInstanceData.taskCurrent != newInstanceData.taskCurrent)
            sDiff.add(indent + "oldInstanceData.taskCurrent == " + oldInstanceData.taskCurrent + ", newInstanceData.taskCurrent == " + newInstanceData.taskCurrent)

        if (oldInstanceData.isRootInstance != newInstanceData.isRootInstance)
            sDiff.add(indent + "oldInstanceData.isRootInstance == " + oldInstanceData.isRootInstance + ", newInstanceData.isRootInstance == " + newInstanceData.isRootInstance)

        if (oldInstanceData.isRootTask == null != (newInstanceData.isRootTask == null) || oldInstanceData.isRootTask != null && oldInstanceData.isRootTask != newInstanceData.isRootTask)
            sDiff.add(indent + "oldInstanceData.isRootTask == " + oldInstanceData.isRootTask + ", newInstanceData.isRootTask == " + newInstanceData.isRootTask)

        if (oldInstanceData.exists != newInstanceData.exists)
            sDiff.add(indent + "oldInstanceData.exists == " + oldInstanceData.exists + ", newInstanceData.exists == " + newInstanceData.exists)

        if (oldInstanceData.instanceTimePair != newInstanceData.instanceTimePair)
            sDiff.add(indent + "oldInstanceData.instanceTimePair == " + oldInstanceData.instanceTimePair + ", newInstanceData.instanceTimePair == " + newInstanceData.instanceTimePair)

        if (oldInstanceData.note != newInstanceData.note)
            sDiff.add(indent + "oldInstanceData.note == " + oldInstanceData.note + ", newInstanceData.note == " + newInstanceData.note)

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
