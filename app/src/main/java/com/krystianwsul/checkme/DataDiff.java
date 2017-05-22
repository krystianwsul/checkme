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

        diffMap("", oldData.InstanceDatas, newData.InstanceDatas);
    }

    private static void diffMap(@NonNull String indent, @NonNull Map<InstanceKey, GroupListFragment.InstanceData> oldInstanceDatas, @NonNull Map<InstanceKey, GroupListFragment.InstanceData> newInstanceDatas) {
        Assert.assertTrue(!TextUtils.isEmpty(indent));

        HashSet<InstanceKey> instanceKeys = new HashSet<>();
        instanceKeys.addAll(oldInstanceDatas.keySet());
        instanceKeys.addAll(newInstanceDatas.keySet());

        for (InstanceKey instanceKey : instanceKeys) {
            if (!oldInstanceDatas.keySet().contains(instanceKey)) {
                sDiff.add(indent + newInstanceDatas.get(instanceKey).Name + " missing from oldData");
                continue;
            }

            GroupListFragment.InstanceData oldInstanceData = oldInstanceDatas.get(instanceKey);
            Assert.assertTrue(oldInstanceData != null);

            if (!newInstanceDatas.keySet().contains(instanceKey)) {
                sDiff.add(indent + oldInstanceData.Name + " missing from newData");
                continue;
            }

            GroupListFragment.InstanceData newInstanceData = newInstanceDatas.get(instanceKey);
            Assert.assertTrue(newInstanceData != null);

            if (!oldInstanceData.equals(newInstanceData)) {
                sDiff.add(indent + "difference in " + oldInstanceData.Name + ":");

                diffInstance(indent + "\t", oldInstanceData, newInstanceData);
            }
        }
    }

    private static void diffInstance(@NonNull String indent, @NonNull GroupListFragment.InstanceData oldInstanceData, @NonNull GroupListFragment.InstanceData newInstanceData) {
        Assert.assertTrue(!TextUtils.isEmpty(indent));
        Assert.assertTrue(oldInstanceData.InstanceKey.equals(newInstanceData.InstanceKey));

        if (((oldInstanceData.Done == null) != (newInstanceData.Done == null)) || ((oldInstanceData.Done != null) && !oldInstanceData.Done.equals(newInstanceData.Done)))
            sDiff.add(indent + "oldInstanceData.Done == " + oldInstanceData.Done + ", newInstanceData.Done == " + newInstanceData.Done);

        if ((TextUtils.isEmpty(oldInstanceData.DisplayText) != TextUtils.isEmpty(newInstanceData.DisplayText)) || (!TextUtils.isEmpty(oldInstanceData.DisplayText) && !oldInstanceData.DisplayText.equals(newInstanceData.DisplayText)))
            sDiff.add(indent + "oldInstanceData.DisplayText == " + oldInstanceData.DisplayText + ", newInstanceData.DisplayText == " + newInstanceData.DisplayText);

        if (!oldInstanceData.Name.equals(newInstanceData.Name))
            sDiff.add(indent + "oldInstanceData.Name == " + oldInstanceData.Name + ", newInstanceData.Name == " + newInstanceData.Name);

        if (!oldInstanceData.InstanceTimeStamp.equals(newInstanceData.InstanceTimeStamp))
            sDiff.add(indent + "oldInstanceData.InstanceTimeStamp == " + oldInstanceData.InstanceTimeStamp + ", newInstanceData.InstanceTimeStamp == " + newInstanceData.InstanceTimeStamp);

        if (oldInstanceData.TaskCurrent != newInstanceData.TaskCurrent)
            sDiff.add(indent + "oldInstanceData.TaskCurrent == " + oldInstanceData.TaskCurrent + ", newInstanceData.TaskCurrent == " + newInstanceData.TaskCurrent);

        if (oldInstanceData.IsRootInstance != newInstanceData.IsRootInstance)
            sDiff.add(indent + "oldInstanceData.IsRootInstance == " + oldInstanceData.IsRootInstance + ", newInstanceData.IsRootInstance == " + newInstanceData.IsRootInstance);

        if (((oldInstanceData.IsRootTask == null) != (newInstanceData.IsRootTask == null)) || ((oldInstanceData.IsRootTask != null) && !oldInstanceData.IsRootTask.equals(newInstanceData.IsRootTask)))
            sDiff.add(indent + "oldInstanceData.IsRootTask == " + oldInstanceData.IsRootTask + ", newInstanceData.IsRootTask == " + newInstanceData.IsRootTask);

        if (oldInstanceData.Exists != newInstanceData.Exists)
            sDiff.add(indent + "oldInstanceData.Exists == " + oldInstanceData.Exists + ", newInstanceData.mExists == " + newInstanceData.Exists);

        if (!oldInstanceData.InstanceTimePair.equals(newInstanceData.InstanceTimePair))
            sDiff.add(indent + "oldInstanceData.InstanceTimePair == " + oldInstanceData.InstanceTimePair + ", newInstanceData.InstanceTimePair == " + newInstanceData.InstanceTimePair);

        if (!oldInstanceData.Children.equals(newInstanceData.Children)) {
            sDiff.add(indent + "oldInstanceData.Children != newInstanceData.Children");
            diffMap(indent + "\t", oldInstanceData.Children, newInstanceData.Children);
        }
    }

    @Nullable
    public static String getDiff() {
        return TextUtils.join("\n", sDiff);
    }
}
