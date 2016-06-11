package com.krystianwsul.checkme.gui.instances.tree;

import android.support.v4.util.Pair;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.krystianwsul.checkme.gui.instances.GroupListFragment;
import com.krystianwsul.checkme.loaders.GroupListLoader;
import com.krystianwsul.checkme.utils.InstanceKey;
import com.krystianwsul.checkme.utils.time.ExactTimeStamp;
import com.krystianwsul.checkme.utils.time.TimeStamp;

import junit.framework.Assert;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NotDoneGroupTreeCollection {
    private final ArrayList<NotDoneGroupTreeNode> mNotDoneGroupTreeNodes = new ArrayList<>();

    private final NotDoneGroupModelCollection mNotDoneGroupModelCollection;

    private final WeakReference<TreeNodeCollection> mTreeNodeCollectionReference;

    public NotDoneGroupTreeCollection(NotDoneGroupModelCollection notDoneGroupModelCollection, WeakReference<TreeNodeCollection> treeNodeCollectionReference) {
        Assert.assertTrue(notDoneGroupModelCollection != null);
        Assert.assertTrue(treeNodeCollectionReference != null);

        mNotDoneGroupModelCollection = notDoneGroupModelCollection;
        mTreeNodeCollectionReference = treeNodeCollectionReference;
    }

    public void unselect(TreeNodeCollection treeNodeCollection, TreeViewAdapter treeViewAdapter) {
        Stream.of(mNotDoneGroupTreeNodes)
                .forEach(notDoneGroupTreeNode -> notDoneGroupTreeNode.unselect(treeNodeCollection, treeViewAdapter));
    }

    public List<NotDoneInstanceTreeNode> getSelected() {
        return Stream.of(mNotDoneGroupTreeNodes)
                .flatMap(NotDoneGroupTreeNode::getSelected)
                .collect(Collectors.toList());
    }

    public List<GroupListFragment.Node> getSelectedNodes() {
        return Stream.of(mNotDoneGroupTreeNodes)
                .flatMap(NotDoneGroupTreeNode::getSelectedNodes)
                .collect(Collectors.toList());
    }

    public void remove(NotDoneGroupTreeNode notDoneGroupTreeNode) {
        Assert.assertTrue(notDoneGroupTreeNode != null);
        Assert.assertTrue(mNotDoneGroupTreeNodes.contains(notDoneGroupTreeNode));

        mNotDoneGroupTreeNodes.remove(notDoneGroupTreeNode);
    }

    public int displayedSize() {
        int displayedSize = 0;
        for (NotDoneGroupTreeNode notDoneGroupTreeNode : mNotDoneGroupTreeNodes)
            displayedSize += notDoneGroupTreeNode.displayedSize();
        return displayedSize;
    }

    public void updateCheckBoxes(TreeNodeCollection treeNodeCollection, TreeViewAdapter treeViewAdapter) {
        Stream.of(mNotDoneGroupTreeNodes)
                .forEach(notDoneGroupTreeNode -> notDoneGroupTreeNode.updateCheckBoxes(treeNodeCollection, treeViewAdapter));
    }

    public GroupListFragment.Node getNode(int position) {
        Assert.assertTrue(position >= 0);
        Assert.assertTrue(position < displayedSize());

        for (NotDoneGroupTreeNode notDoneGroupTreeNode : mNotDoneGroupTreeNodes) {
            if (position < notDoneGroupTreeNode.displayedSize())
                return notDoneGroupTreeNode.getNode(position);

            position = position - notDoneGroupTreeNode.displayedSize();
        }

        throw new IndexOutOfBoundsException();
    }

    public int getPosition(GroupListFragment.Node node) {
        int offset = 0;
        for (NotDoneGroupTreeNode notDoneGroupTreeNode : mNotDoneGroupTreeNodes) {
            int position = notDoneGroupTreeNode.getPosition(node);
            if (position >= 0)
                return offset + position;
            offset += notDoneGroupTreeNode.displayedSize();
        }

        return -1;
    }

    public ArrayList<TimeStamp> getExpandedGroups() {
        return Stream.of(mNotDoneGroupTreeNodes)
                .filter(NotDoneGroupTreeNode::expanded)
                .map(notDoneGroupTreeNode -> notDoneGroupTreeNode.mExactTimeStamp.toTimeStamp())
                .collect(Collectors.toCollection(ArrayList::new));
    }

    public void setInstanceDatas(Collection<GroupListLoader.InstanceData> instanceDatas, ArrayList<TimeStamp> expandedGroups, ArrayList<InstanceKey> selectedNodes, TreeNodeCollection treeNodeCollection, TreeViewAdapter treeViewAdapter) {
        Assert.assertTrue(instanceDatas != null);
        Assert.assertTrue(treeNodeCollection != null);
        Assert.assertTrue(treeViewAdapter != null);

        if (treeViewAdapter.getGroupAdapter().mUseGroups) {
            HashMap<TimeStamp, ArrayList<GroupListLoader.InstanceData>> instanceDataHash = new HashMap<>();
            for (GroupListLoader.InstanceData instanceData : instanceDatas) {
                if (!instanceDataHash.containsKey(instanceData.InstanceTimeStamp))
                    instanceDataHash.put(instanceData.InstanceTimeStamp, new ArrayList<>());
                instanceDataHash.get(instanceData.InstanceTimeStamp).add(instanceData);
            }

            for (Map.Entry<TimeStamp, ArrayList<GroupListLoader.InstanceData>> entry : instanceDataHash.entrySet()) {
                boolean expanded = false;
                if (entry.getValue().size() > 1 && expandedGroups != null && expandedGroups.contains(entry.getKey()))
                    expanded = true;

                GroupListFragment.GroupAdapter.NodeCollection.NotDoneGroupNode notDoneGroupNode = GroupListFragment.GroupAdapter.NodeCollection.NotDoneGroupNode.newNotDoneGroupNode(new WeakReference<>(mNotDoneGroupModelCollection.getNotDoneGroupCollection()));
                NotDoneGroupTreeNode notDoneGroupTreeNode = new NotDoneGroupTreeNode(notDoneGroupNode.getNotDoneGroupModelNode(), expanded, new WeakReference<>(this));
                notDoneGroupNode.setNotDoneGroupTreeNodeReference(new WeakReference<>(notDoneGroupTreeNode));
                notDoneGroupTreeNode.setInstanceDatas(entry.getValue(), selectedNodes);
                mNotDoneGroupTreeNodes.add(notDoneGroupTreeNode);
            }
        } else {
            for (GroupListLoader.InstanceData instanceData : instanceDatas) {
                ArrayList<GroupListLoader.InstanceData> dummyInstanceDatas = new ArrayList<>();
                dummyInstanceDatas.add(instanceData);

                GroupListFragment.GroupAdapter.NodeCollection.NotDoneGroupNode notDoneGroupNode = GroupListFragment.GroupAdapter.NodeCollection.NotDoneGroupNode.newNotDoneGroupNode(new WeakReference<>(mNotDoneGroupModelCollection.getNotDoneGroupCollection()));
                NotDoneGroupTreeNode notDoneGroupTreeNode = new NotDoneGroupTreeNode(notDoneGroupNode.getNotDoneGroupModelNode(), false, new WeakReference<>(this));
                notDoneGroupNode.setNotDoneGroupTreeNodeReference(new WeakReference<>(notDoneGroupTreeNode));
                notDoneGroupTreeNode.setInstanceDatas(dummyInstanceDatas, selectedNodes);
                mNotDoneGroupTreeNodes.add(notDoneGroupTreeNode);
            }
        }

        sort();
    }

    private void sort() {
        Collections.sort(mNotDoneGroupTreeNodes, mNotDoneGroupModelCollection.getComparator());
    }

    public Pair<Boolean, Pair<NotDoneGroupTreeNode, NotDoneInstanceTreeNode>> add(GroupListLoader.InstanceData instanceData, TreeNodeCollection treeNodeCollection, TreeViewAdapter treeViewAdapter) {
        Assert.assertTrue(instanceData != null);
        Assert.assertTrue(instanceData.Done == null);
        Assert.assertTrue(treeNodeCollection != null);
        Assert.assertTrue(treeViewAdapter != null);

        Pair<Boolean, Pair<NotDoneGroupTreeNode, NotDoneInstanceTreeNode>> pair = addInstanceHelper(instanceData, treeNodeCollection, treeViewAdapter);
        sort();

        return pair;
    }

    private Pair<Boolean, Pair<NotDoneGroupTreeNode, NotDoneInstanceTreeNode>> addInstanceHelper(GroupListLoader.InstanceData instanceData, TreeNodeCollection treeNodeCollection, TreeViewAdapter treeViewAdapter) {
        Assert.assertTrue(instanceData != null);
        Assert.assertTrue(instanceData.Done == null);
        Assert.assertTrue(treeNodeCollection != null);
        Assert.assertTrue(treeViewAdapter != null);

        ExactTimeStamp exactTimeStamp = instanceData.InstanceTimeStamp.toExactTimeStamp();

        List<NotDoneGroupTreeNode> timeStampNotDoneGroupTreeNodes = Stream.of(mNotDoneGroupTreeNodes)
                .filter(notDoneGroupTreeNode -> notDoneGroupTreeNode.mExactTimeStamp.equals(exactTimeStamp))
                .collect(Collectors.toList());

        if (timeStampNotDoneGroupTreeNodes.isEmpty()) {
            ArrayList<GroupListLoader.InstanceData> instanceDatas = new ArrayList<>();
            instanceDatas.add(instanceData);

            GroupListFragment.GroupAdapter.NodeCollection.NotDoneGroupNode notDoneGroupNode = GroupListFragment.GroupAdapter.NodeCollection.NotDoneGroupNode.newNotDoneGroupNode(new WeakReference<>(mNotDoneGroupModelCollection.getNotDoneGroupCollection()));
            NotDoneGroupTreeNode notDoneGroupTreeNode = new NotDoneGroupTreeNode(notDoneGroupNode.getNotDoneGroupModelNode(), false, new WeakReference<>(this));
            notDoneGroupNode.setNotDoneGroupTreeNodeReference(new WeakReference<>(notDoneGroupTreeNode));
            notDoneGroupTreeNode.setInstanceDatas(instanceDatas, null);
            NotDoneInstanceTreeNode notDoneInstanceTreeNode = notDoneGroupTreeNode.mNotDoneInstanceTreeNodes.get(0);

            mNotDoneGroupTreeNodes.add(notDoneGroupTreeNode);
            return new Pair<>(true, new Pair<>(notDoneGroupTreeNode, notDoneInstanceTreeNode));
        } else {
            Assert.assertTrue(timeStampNotDoneGroupTreeNodes.size() == 1);

            NotDoneGroupTreeNode notDoneGroupTreeNode = timeStampNotDoneGroupTreeNodes.get(0);
            NotDoneInstanceTreeNode notDoneInstanceTreeNode = notDoneGroupTreeNode.addInstanceData(instanceData, null);

            notDoneGroupTreeNode.sort();
            return new Pair<>(false, new Pair<>(notDoneGroupTreeNode, notDoneInstanceTreeNode));
        }
    }

    public TreeNodeCollection getTreeNodeCollection() {
        TreeNodeCollection treeNodeCollection = mTreeNodeCollectionReference.get();
        Assert.assertTrue(treeNodeCollection != null);

        return treeNodeCollection;
    }

    public TreeViewAdapter getTreeViewAdapter() {
        TreeNodeCollection treeNodeCollection = getTreeNodeCollection();
        Assert.assertTrue(treeNodeCollection != null);

        TreeViewAdapter treeViewAdapter = treeNodeCollection.getTreeViewAdapter();
        Assert.assertTrue(treeViewAdapter != null);

        return treeViewAdapter;
    }
}
