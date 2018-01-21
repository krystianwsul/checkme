package com.krystianwsul.checkme;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.krystianwsul.checkme.gui.instances.tree.GroupListFragment;
import com.krystianwsul.checkme.utils.InstanceKey;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class DataDiff {
    @NonNull
    private static List<String> sDiff = new ArrayList<>();

    public static void diffData(@NonNull GroupListFragment.DataWrapper oldData, @NonNull GroupListFragment.DataWrapper newData) {
        sDiff = new ArrayList<>();

        diffMap("", oldData.getInstanceDatas(), newData.getInstanceDatas());
    }

    private static void diffMap(@NonNull String indent, @NonNull Map<InstanceKey, GroupListFragment.InstanceData> oldInstanceDatas, @NonNull Map<InstanceKey, GroupListFragment.InstanceData> newInstanceDatas) {
        HashSet<InstanceKey> instanceKeys = new HashSet<>();
        instanceKeys.addAll(oldInstanceDatas.keySet());
        instanceKeys.addAll(newInstanceDatas.keySet());

        for (InstanceKey instanceKey : instanceKeys) {
            if (!oldInstanceDatas.keySet().contains(instanceKey)) {
                sDiff.add(indent + newInstanceDatas.get(instanceKey).getName() + " missing from oldData");
                continue;
            }

            GroupListFragment.InstanceData oldInstanceData = oldInstanceDatas.get(instanceKey);
            Assert.assertTrue(oldInstanceData != null);

            if (!newInstanceDatas.keySet().contains(instanceKey)) {
                sDiff.add(indent + oldInstanceData.getName() + " missing from newData");
                continue;
            }

            GroupListFragment.InstanceData newInstanceData = newInstanceDatas.get(instanceKey);
            Assert.assertTrue(newInstanceData != null);

            if (!oldInstanceData.equals(newInstanceData)) {
                sDiff.add(indent + "difference in " + oldInstanceData.getName() + ":");

                diffInstance(indent + "\t", oldInstanceData, newInstanceData);
            }
        }
    }

    private static void diffInstance(@NonNull String indent, @NonNull GroupListFragment.InstanceData oldInstanceData, @NonNull GroupListFragment.InstanceData newInstanceData) {
        Assert.assertTrue(!TextUtils.isEmpty(indent));
        Assert.assertTrue(oldInstanceData.getInstanceKey().equals(newInstanceData.getInstanceKey()));

        if (((oldInstanceData.getDone() == null) != (newInstanceData.getDone() == null)) || ((oldInstanceData.getDone() != null) && !oldInstanceData.getDone().equals(newInstanceData.getDone())))
            sDiff.add(indent + "oldInstanceData.done == " + oldInstanceData.getDone() + ", newInstanceData.done == " + newInstanceData.getDone());

        if ((TextUtils.isEmpty(oldInstanceData.getDisplayText()) != TextUtils.isEmpty(newInstanceData.getDisplayText())) || (!TextUtils.isEmpty(oldInstanceData.getDisplayText()) && !oldInstanceData.getDisplayText().equals(newInstanceData.getDisplayText())))
            sDiff.add(indent + "oldInstanceData.displayText == " + oldInstanceData.getDisplayText() + ", newInstanceData.displayText == " + newInstanceData.getDisplayText());

        if (!oldInstanceData.getName().equals(newInstanceData.getName()))
            sDiff.add(indent + "oldInstanceData.name == " + oldInstanceData.getName() + ", newInstanceData.name == " + newInstanceData.getName());

        if (!oldInstanceData.getInstanceTimeStamp().equals(newInstanceData.getInstanceTimeStamp()))
            sDiff.add(indent + "oldInstanceData.InstanceTimeStamp == " + oldInstanceData.getInstanceTimeStamp() + ", newInstanceData.InstanceTimeStamp == " + newInstanceData.getInstanceTimeStamp());

        if (oldInstanceData.getTaskCurrent() != newInstanceData.getTaskCurrent())
            sDiff.add(indent + "oldInstanceData.taskCurrent == " + oldInstanceData.getTaskCurrent() + ", newInstanceData.taskCurrent == " + newInstanceData.getTaskCurrent());

        if (oldInstanceData.getIsRootInstance() != newInstanceData.getIsRootInstance())
            sDiff.add(indent + "oldInstanceData.isRootInstance == " + oldInstanceData.getIsRootInstance() + ", newInstanceData.isRootInstance == " + newInstanceData.getIsRootInstance());

        if (((oldInstanceData.getIsRootTask() == null) != (newInstanceData.getIsRootTask() == null)) || ((oldInstanceData.getIsRootTask() != null) && !oldInstanceData.getIsRootTask().equals(newInstanceData.getIsRootTask())))
            sDiff.add(indent + "oldInstanceData.IsRootTask == " + oldInstanceData.getIsRootTask() + ", newInstanceData.IsRootTask == " + newInstanceData.getIsRootTask());

        if (oldInstanceData.getExists() != newInstanceData.getExists())
            sDiff.add(indent + "oldInstanceData.Exists == " + oldInstanceData.getExists() + ", newInstanceData.exists == " + newInstanceData.getExists());

        if (!oldInstanceData.getInstanceTimePair().equals(newInstanceData.getInstanceTimePair()))
            sDiff.add(indent + "oldInstanceData.instanceTimePair == " + oldInstanceData.getInstanceTimePair() + ", newInstanceData.instanceTimePair == " + newInstanceData.getInstanceTimePair());

        if (!oldInstanceData.getChildren().equals(newInstanceData.getChildren())) {
            sDiff.add(indent + "oldInstanceData.children != newInstanceData.children");
            diffMap(indent + "\t", oldInstanceData.getChildren(), newInstanceData.getChildren());
        }
    }

    @Nullable
    public static String getDiff() {
        return TextUtils.join("\n", sDiff);
    }
}
