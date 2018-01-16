package com.krystianwsul.checkme.gui.instances.tree;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.krystianwsul.checkme.utils.InstanceKey;
import com.krystianwsul.checkme.utils.TaskKey;
import com.krystianwsul.checkme.utils.time.TimeStamp;
import com.krystianwsul.treeadapter.NodeContainer;
import com.krystianwsul.treeadapter.TreeNode;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

public class NodeCollection {
    @NonNull
    private final NodeCollectionParent mNodeCollectionParent;

    @NonNull
    private final NodeContainer mNodeContainer;

    private NotDoneGroupCollection mNotDoneGroupCollection;
    private DividerNode mDividerNode;
    private UnscheduledNode mUnscheduledNode;

    final boolean mUseGroups;

    private final float mDensity;
    private final int mIndentation;

    private final String mNote;

    NodeCollection(float density, int indentation, @NonNull NodeCollectionParent nodeCollectionParent, boolean useGroups, @NonNull NodeContainer nodeContainer, @Nullable String note) {
        mDensity = density;
        mIndentation = indentation;
        mNodeCollectionParent = nodeCollectionParent;
        mUseGroups = useGroups;
        mNodeContainer = nodeContainer;
        mNote = note;
    }

    @NonNull
    List<TreeNode> initialize(@NonNull Collection<GroupListFragment.InstanceData> instanceDatas, @Nullable List<TimeStamp> expandedGroups, @Nullable HashMap<InstanceKey, Boolean> expandedInstances, boolean doneExpanded, @Nullable ArrayList<InstanceKey> selectedNodes, boolean selectable, @Nullable List<GroupListFragment.TaskData> taskDatas, boolean unscheduledExpanded, @Nullable List<TaskKey> expandedTaskKeys) {
        ArrayList<GroupListFragment.InstanceData> notDoneInstanceDatas = new ArrayList<>();
        ArrayList<GroupListFragment.InstanceData> doneInstanceDatas = new ArrayList<>();
        for (GroupListFragment.InstanceData instanceData : instanceDatas) {
            if (instanceData.getDone() == null)
                notDoneInstanceDatas.add(instanceData);
            else
                doneInstanceDatas.add(instanceData);
        }

        List<TreeNode> rootTreeNodes = new ArrayList<>();

        if (!TextUtils.isEmpty(mNote)) {
            Assert.assertTrue(mIndentation == 0);

            rootTreeNodes.add(new NoteNode(mDensity, mNote, getGroupAdapter()).initialize(mNodeContainer));
        }

        mNotDoneGroupCollection = new NotDoneGroupCollection(mDensity, mIndentation, this, mNodeContainer, selectable);

        rootTreeNodes.addAll(mNotDoneGroupCollection.initialize(notDoneInstanceDatas, expandedGroups, expandedInstances, selectedNodes));

        Assert.assertTrue((mIndentation == 0) || (taskDatas == null));
        if (taskDatas != null && !taskDatas.isEmpty()) {
            mUnscheduledNode = new UnscheduledNode(mDensity, this);

            TreeNode unscheduledTreeNode = mUnscheduledNode.initialize(unscheduledExpanded, mNodeContainer, taskDatas, expandedTaskKeys);

            rootTreeNodes.add(unscheduledTreeNode);
        }

        mDividerNode = new DividerNode(mDensity, mIndentation, this);

        doneExpanded = doneExpanded && !doneInstanceDatas.isEmpty();

        TreeNode dividerTreeNode = mDividerNode.initialize(doneExpanded, mNodeContainer, doneInstanceDatas, expandedInstances);

        rootTreeNodes.add(dividerTreeNode);

        return rootTreeNodes;
    }

    @NonNull
    private NodeCollectionParent getNodeCollectionParent() {
        return mNodeCollectionParent;
    }

    @NonNull
    NodeContainer getNodeContainer() {
        return mNodeContainer;
    }

    @NonNull
    GroupListFragment.GroupAdapter getGroupAdapter() {
        return getNodeCollectionParent().getGroupAdapter();
    }

    @NonNull
    NotDoneGroupCollection getNotDoneGroupCollection() {
        Assert.assertTrue(mNotDoneGroupCollection != null);

        return mNotDoneGroupCollection;
    }

    @NonNull
    List<TimeStamp> getExpandedGroups() {
        return mNotDoneGroupCollection.getExpandedGroups();
    }

    @NonNull
    DividerNode getDividerNode() {
        Assert.assertTrue(mDividerNode != null);

        return mDividerNode;
    }

    void addExpandedInstances(@NonNull HashMap<InstanceKey, Boolean> expandedInstances) {
        mNotDoneGroupCollection.addExpandedInstances(expandedInstances);
        mDividerNode.addExpandedInstances(expandedInstances);
    }

    boolean getUnscheduledExpanded() {
        return (mUnscheduledNode != null && mUnscheduledNode.expanded());
    }

    @Nullable
    List<TaskKey> getExpandedTaskKeys() {
        if (mUnscheduledNode == null)
            return null;
        else
            return mUnscheduledNode.getExpandedTaskKeys();
    }

    boolean getDoneExpanded() {
        return mDividerNode.expanded();
    }
}
