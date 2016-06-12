package com.krystianwsul.checkme.gui.instances.tree;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.krystianwsul.checkme.gui.instances.GroupListFragment;
import com.krystianwsul.checkme.loaders.GroupListLoader;
import com.krystianwsul.checkme.utils.InstanceKey;
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

    public void unselect() {
        Stream.of(mNotDoneGroupTreeNodes)
                .forEach(NotDoneGroupTreeNode::unselect);
    }

    public List<GroupListFragment.Node> getSelectedNodes() {
        return Stream.of(mNotDoneGroupTreeNodes)
                .flatMap(NotDoneGroupTreeNode::getSelectedNodes)
                .collect(Collectors.toList());
    }

    public int remove(NotDoneGroupTreeNode notDoneGroupTreeNode) {
        Assert.assertTrue(notDoneGroupTreeNode != null);
        Assert.assertTrue(mNotDoneGroupTreeNodes.contains(notDoneGroupTreeNode));

        TreeNodeCollection treeNodeCollection = getTreeNodeCollection();
        Assert.assertTrue(treeNodeCollection != null);

        TreeViewAdapter treeViewAdapter = treeNodeCollection.getTreeViewAdapter();
        Assert.assertTrue(treeViewAdapter != null);

        int oldPosition = treeNodeCollection.getPosition(notDoneGroupTreeNode);

        mNotDoneGroupTreeNodes.remove(notDoneGroupTreeNode);

        treeViewAdapter.notifyItemRemoved(oldPosition);

        return oldPosition;
    }

    public int displayedSize() {
        int displayedSize = 0;
        for (NotDoneGroupTreeNode notDoneGroupTreeNode : mNotDoneGroupTreeNodes)
            displayedSize += notDoneGroupTreeNode.displayedSize();
        return displayedSize;
    }

    public void updateCheckBoxes() {
        Stream.of(mNotDoneGroupTreeNodes)
                .forEach(NotDoneGroupTreeNode::updateCheckBoxes);
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
                .map(notDoneGroupTreeNode -> notDoneGroupTreeNode.getNotDoneGroupModelNode().getExactTimeStamp().toTimeStamp())
                .collect(Collectors.toCollection(ArrayList::new));
    }

    public void setInstanceDatas(Collection<GroupListLoader.InstanceData> instanceDatas, ArrayList<TimeStamp> expandedGroups, ArrayList<InstanceKey> selectedNodes) {
        Assert.assertTrue(instanceDatas != null);

        TreeNodeCollection treeNodeCollection = getTreeNodeCollection();
        Assert.assertTrue(treeNodeCollection != null);

        TreeViewAdapter treeViewAdapter = treeNodeCollection.getTreeViewAdapter();
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

                NotDoneGroupTreeNode notDoneGroupTreeNode = mNotDoneGroupModelCollection.newNotDoneGroupNode(new WeakReference<>(mNotDoneGroupModelCollection.getNotDoneGroupCollection()), entry.getValue(), expanded, selectedNodes);
                Assert.assertTrue(notDoneGroupTreeNode != null);

                mNotDoneGroupTreeNodes.add(notDoneGroupTreeNode);
            }
        } else {
            for (GroupListLoader.InstanceData instanceData : instanceDatas) {
                ArrayList<GroupListLoader.InstanceData> dummyInstanceDatas = new ArrayList<>();
                dummyInstanceDatas.add(instanceData);

                NotDoneGroupTreeNode notDoneGroupTreeNode = mNotDoneGroupModelCollection.newNotDoneGroupNode(new WeakReference<>(mNotDoneGroupModelCollection.getNotDoneGroupCollection()), dummyInstanceDatas, false, selectedNodes);
                Assert.assertTrue(notDoneGroupTreeNode != null);

                mNotDoneGroupTreeNodes.add(notDoneGroupTreeNode);
            }
        }

        sort();
    }

    private void sort() {
        Collections.sort(mNotDoneGroupTreeNodes);
    }

    public void addNotDoneGroupTreeNode(NotDoneGroupTreeNode notDoneGroupTreeNode) {
        Assert.assertTrue(notDoneGroupTreeNode != null);

        mNotDoneGroupTreeNodes.add(notDoneGroupTreeNode);

        sort();

        TreeNodeCollection treeNodeCollection = getTreeNodeCollection();
        Assert.assertTrue(treeNodeCollection != null);

        TreeViewAdapter treeViewAdapter = treeNodeCollection.getTreeViewAdapter();
        Assert.assertTrue(treeViewAdapter != null);

        treeViewAdapter.notifyItemInserted(treeNodeCollection.getPosition(notDoneGroupTreeNode));
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

    public void onCreateActionMode() {
        updateCheckBoxes();
    }

    public void onDestroyActionMode() {
        updateCheckBoxes();
    }

    public NotDoneGroupModelCollection getNotDoneGroupModelCollection() {
        return mNotDoneGroupModelCollection;
    }
}
