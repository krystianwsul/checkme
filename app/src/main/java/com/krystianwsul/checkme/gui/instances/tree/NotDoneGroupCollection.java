package com.krystianwsul.checkme.gui.instances.tree;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.krystianwsul.checkme.utils.InstanceKey;
import com.krystianwsul.checkme.utils.time.ExactTimeStamp;
import com.krystianwsul.checkme.utils.time.TimeStamp;
import com.krystianwsul.treeadapter.NodeContainer;
import com.krystianwsul.treeadapter.TreeNode;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class NotDoneGroupCollection {
    @NonNull
    private final NodeCollection mNodeCollection;

    @NonNull
    private final NodeContainer mNodeContainer;

    private final ArrayList<NotDoneGroupNode> mNotDoneGroupNodes = new ArrayList<>();

    private final float mDensity;
    private final int mIndentation;

    private final boolean mSelectable;

    NotDoneGroupCollection(float density, int indentation, @NonNull NodeCollection nodeCollection, @NonNull NodeContainer nodeContainer, boolean selectable) {
        mDensity = density;
        mIndentation = indentation;
        mNodeCollection = nodeCollection;
        mNodeContainer = nodeContainer;
        mSelectable = selectable;
    }

    @NonNull
    List<TreeNode> initialize(@NonNull List<GroupListFragment.InstanceData> notDoneInstanceDatas, @Nullable List<TimeStamp> expandedGroups, @Nullable HashMap<InstanceKey, Boolean> expandedInstances, @Nullable ArrayList<InstanceKey> selectedNodes) {
        ArrayList<TreeNode> notDoneGroupTreeNodes = new ArrayList<>();

        NodeCollection nodeCollection = getNodeCollection();

        if (nodeCollection.mUseGroups) {
            HashMap<TimeStamp, ArrayList<GroupListFragment.InstanceData>> instanceDataHash = new HashMap<>();
            for (GroupListFragment.InstanceData instanceData : notDoneInstanceDatas) {
                if (!instanceDataHash.containsKey(instanceData.InstanceTimeStamp))
                    instanceDataHash.put(instanceData.InstanceTimeStamp, new ArrayList<>());
                instanceDataHash.get(instanceData.InstanceTimeStamp).add(instanceData);
            }

            for (Map.Entry<TimeStamp, ArrayList<GroupListFragment.InstanceData>> entry : instanceDataHash.entrySet()) {
                TreeNode notDoneGroupTreeNode = newNotDoneGroupNode(this, entry.getValue(), expandedGroups, expandedInstances, selectedNodes);

                notDoneGroupTreeNodes.add(notDoneGroupTreeNode);
            }
        } else {
            for (GroupListFragment.InstanceData instanceData : notDoneInstanceDatas) {
                ArrayList<GroupListFragment.InstanceData> dummyInstanceDatas = new ArrayList<>();
                dummyInstanceDatas.add(instanceData);

                TreeNode notDoneGroupTreeNode = newNotDoneGroupNode(this, dummyInstanceDatas, expandedGroups, expandedInstances, selectedNodes);

                notDoneGroupTreeNodes.add(notDoneGroupTreeNode);
            }
        }

        return notDoneGroupTreeNodes;
    }

    public void remove(@NonNull NotDoneGroupNode notDoneGroupNode) {
        Assert.assertTrue(mNotDoneGroupNodes.contains(notDoneGroupNode));
        mNotDoneGroupNodes.remove(notDoneGroupNode);

        NodeContainer nodeContainer = getNodeContainer();

        TreeNode notDoneGroupTreeNode = notDoneGroupNode.getTreeNode();

        nodeContainer.remove(notDoneGroupTreeNode);
    }

    public void add(@NonNull GroupListFragment.InstanceData instanceData) {
        NodeCollection nodeCollection = getNodeCollection();

        NodeContainer nodeContainer = nodeCollection.getNodeContainer();

        ExactTimeStamp exactTimeStamp = instanceData.InstanceTimeStamp.toExactTimeStamp();

        List<NotDoneGroupNode> timeStampNotDoneGroupNodes = Stream.of(mNotDoneGroupNodes)
                .filter(notDoneGroupNode -> notDoneGroupNode.mExactTimeStamp.equals(exactTimeStamp))
                .collect(Collectors.toList());

        if (timeStampNotDoneGroupNodes.isEmpty() || !nodeCollection.mUseGroups) {
            ArrayList<GroupListFragment.InstanceData> instanceDatas = new ArrayList<>();
            instanceDatas.add(instanceData);

            TreeNode notDoneGroupTreeNode = newNotDoneGroupNode(this, instanceDatas, null, null, null);

            nodeContainer.add(notDoneGroupTreeNode);
        } else {
            Assert.assertTrue(timeStampNotDoneGroupNodes.size() == 1);

            NotDoneGroupNode notDoneGroupNode = timeStampNotDoneGroupNodes.get(0);
            Assert.assertTrue(notDoneGroupNode != null);

            notDoneGroupNode.addInstanceData(instanceData);
        }
    }

    @NonNull
    private TreeNode newNotDoneGroupNode(@NonNull NotDoneGroupCollection notDoneGroupCollection, @NonNull List<GroupListFragment.InstanceData> instanceDatas, @Nullable List<TimeStamp> expandedGroups, @Nullable HashMap<InstanceKey, Boolean> expandedInstances, @Nullable ArrayList<InstanceKey> selectedNodes) {
        Assert.assertTrue(!instanceDatas.isEmpty());

        NotDoneGroupNode notDoneGroupNode = new NotDoneGroupNode(mDensity, mIndentation, notDoneGroupCollection, instanceDatas, mSelectable);

        TreeNode notDoneGroupTreeNode = notDoneGroupNode.initialize(expandedGroups, expandedInstances, selectedNodes, mNodeContainer);
        Assert.assertTrue(notDoneGroupTreeNode != null);

        mNotDoneGroupNodes.add(notDoneGroupNode);

        return notDoneGroupTreeNode;
    }

    @NonNull
    NodeCollection getNodeCollection() {
        return mNodeCollection;
    }

    @NonNull
    private NodeContainer getNodeContainer() {
        return mNodeContainer;
    }

    @NonNull
    List<TimeStamp> getExpandedGroups() {
        return Stream.of(mNotDoneGroupNodes)
                .filter(notDoneGroupNode -> !notDoneGroupNode.singleInstance() && notDoneGroupNode.expanded())
                .map(notDoneGroupNode -> notDoneGroupNode.mExactTimeStamp.toTimeStamp())
                .collect(Collectors.toList());
    }

    void addExpandedInstances(HashMap<InstanceKey, Boolean> expandedInstances) {
        for (NotDoneGroupNode notDoneGroupNode : mNotDoneGroupNodes)
            notDoneGroupNode.addExpandedInstances(expandedInstances);
    }
}
