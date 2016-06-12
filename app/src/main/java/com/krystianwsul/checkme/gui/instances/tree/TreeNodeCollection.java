package com.krystianwsul.checkme.gui.instances.tree;

import com.krystianwsul.checkme.gui.instances.GroupListFragment;
import com.krystianwsul.checkme.loaders.GroupListLoader;
import com.krystianwsul.checkme.utils.InstanceKey;
import com.krystianwsul.checkme.utils.time.TimeStamp;

import junit.framework.Assert;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class TreeNodeCollection {
    public NotDoneGroupTreeCollection mNotDoneGroupTreeCollection;

    public DividerTreeNode mDividerTreeNode;

    private final ModelNodeCollection mModelNodeCollection;

    private final WeakReference<TreeViewAdapter> mTreeViewAdapterReference;

    public TreeNodeCollection(ModelNodeCollection modelNodeCollection, WeakReference<TreeViewAdapter> treeViewAdapterReference) {
        Assert.assertTrue(modelNodeCollection != null);
        Assert.assertTrue(treeViewAdapterReference != null);

        mModelNodeCollection = modelNodeCollection;
        mTreeViewAdapterReference = treeViewAdapterReference;
    }

    public GroupListFragment.Node getNode(int position) {
        Assert.assertTrue(position >= 0);

        if (position < mNotDoneGroupTreeCollection.displayedSize())
            return mNotDoneGroupTreeCollection.getNode(position);

        Assert.assertTrue(!mDividerTreeNode.isEmpty());

        int newPosition = position - mNotDoneGroupTreeCollection.displayedSize();
        Assert.assertTrue(newPosition < mDividerTreeNode.displayedSize());
        return mDividerTreeNode.getNode(newPosition);
    }

    public int getPosition(GroupListFragment.Node node) {
        Assert.assertTrue(node != null);

        int offset = 0;

        int position = mNotDoneGroupTreeCollection.getPosition(node);
        if (position >= 0)
            return position;

        offset = offset + mNotDoneGroupTreeCollection.displayedSize();

        position = mDividerTreeNode.getPosition(node);
        Assert.assertTrue(position >= 0);

        return offset + position;
    }

    public int getItemCount() {
        return mNotDoneGroupTreeCollection.displayedSize() + mDividerTreeNode.displayedSize();
    }

    public GroupListFragment.ExpansionState getExpansionState() {
        ArrayList<TimeStamp> expandedGroups = mNotDoneGroupTreeCollection.getExpandedGroups();
        return new GroupListFragment.ExpansionState(mDividerTreeNode.expanded(), expandedGroups);
    }

    public int getItemViewType(int position) {
        GroupListFragment.Node node = getNode(position);
        Assert.assertTrue(node != null);

        return node.getItemViewType();
    }

    public void setInstanceDatas(Collection<GroupListLoader.InstanceData> instanceDatas, GroupListFragment.ExpansionState expansionState, ArrayList<InstanceKey> selectedNodes) {
        Assert.assertTrue(instanceDatas != null);

        TreeViewAdapter treeViewAdapter = getTreeViewAdapter();
        Assert.assertTrue(treeViewAdapter != null);

        ArrayList<GroupListLoader.InstanceData> notDoneInstances = new ArrayList<>();
        ArrayList<GroupListLoader.InstanceData> doneInstances = new ArrayList<>();
        for (GroupListLoader.InstanceData instanceData : instanceDatas) {
            if (instanceData.Done == null)
                notDoneInstances.add(instanceData);
            else
                doneInstances.add(instanceData);
        }

        boolean doneExpanded = false;
        ArrayList<TimeStamp> expandedGroups = null;
        if (expansionState != null) {
            doneExpanded = expansionState.DoneExpanded;
            expandedGroups = expansionState.ExpandedGroups;
        }

        GroupListFragment.GroupAdapter.NodeCollection.NotDoneGroupCollection notDoneGroupCollection = GroupListFragment.GroupAdapter.NodeCollection.NotDoneGroupCollection.newNotDoneGroupCollection(new WeakReference<>(this));
        mNotDoneGroupTreeCollection = new NotDoneGroupTreeCollection(notDoneGroupCollection.getNotDoneGroupModelCollection(), new WeakReference<>(this));
        notDoneGroupCollection.setNotDoneGroupTreeCollectionReference(new WeakReference<>(mNotDoneGroupTreeCollection));
        mNotDoneGroupTreeCollection.setInstanceDatas(notDoneInstances, expandedGroups, selectedNodes);

        mDividerTreeNode = GroupListFragment.GroupAdapter.NodeCollection.DividerNode.newDividerTreeNode(doneInstances, doneExpanded, new WeakReference<>(this));
    }

    public GroupListFragment.GroupAdapter.NodeCollection getNodeCollection() {
        return mModelNodeCollection.getNodeCollection();
    }

    public TreeViewAdapter getTreeViewAdapter() {
        TreeViewAdapter treeViewAdapter = mTreeViewAdapterReference.get();
        Assert.assertTrue(treeViewAdapter != null);

        return treeViewAdapter;
    }

    public int displayedSize() {
        return mNotDoneGroupTreeCollection.displayedSize() + mDividerTreeNode.displayedSize();
    }

    public List<GroupListFragment.Node> getSelectedNodes() {
        return mNotDoneGroupTreeCollection.getSelectedNodes();
    }

    public void onCreateActionMode() {
        mNotDoneGroupTreeCollection.onCreateActionMode();
        mDividerTreeNode.onCreateActionMode();
    }

    public void onDestroyActionMode() {
        mNotDoneGroupTreeCollection.onDestroyActionMode();
        mDividerTreeNode.onDestroyActionMode();
    }
}
