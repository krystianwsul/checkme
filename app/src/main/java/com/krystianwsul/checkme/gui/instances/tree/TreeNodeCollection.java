package com.krystianwsul.checkme.gui.instances.tree;

import com.krystianwsul.checkme.gui.instances.GroupListFragment;
import com.krystianwsul.checkme.utils.time.TimeStamp;

import junit.framework.Assert;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class TreeNodeCollection {
    NotDoneGroupTreeCollection mNotDoneGroupTreeCollection;

    DividerTreeNode mDividerTreeNode;

    private final ModelNodeCollection mModelNodeCollection;

    private final WeakReference<TreeViewAdapter> mTreeViewAdapterReference;

    public TreeNodeCollection(ModelNodeCollection modelNodeCollection, WeakReference<TreeViewAdapter> treeViewAdapterReference) {
        Assert.assertTrue(modelNodeCollection != null);
        Assert.assertTrue(treeViewAdapterReference != null);

        mModelNodeCollection = modelNodeCollection;
        mTreeViewAdapterReference = treeViewAdapterReference;
    }

    public Node getNode(int position) {
        Assert.assertTrue(position >= 0);

        if (position < mNotDoneGroupTreeCollection.displayedSize())
            return mNotDoneGroupTreeCollection.getNode(position);

        Assert.assertTrue(!mDividerTreeNode.isEmpty());

        int newPosition = position - mNotDoneGroupTreeCollection.displayedSize();
        Assert.assertTrue(newPosition < mDividerTreeNode.displayedSize());
        return mDividerTreeNode.getNode(newPosition);
    }

    public int getPosition(Node node) {
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
        Node node = getNode(position);
        Assert.assertTrue(node != null);

        return node.getItemViewType();
    }

    public void setNodes(NotDoneGroupTreeCollection notDoneGroupTreeCollection, DividerTreeNode dividerTreeNode) {
        Assert.assertTrue(notDoneGroupTreeCollection != null);
        Assert.assertTrue(dividerTreeNode != null);

        mNotDoneGroupTreeCollection = notDoneGroupTreeCollection;
        mDividerTreeNode = dividerTreeNode;
    }

    TreeViewAdapter getTreeViewAdapter() {
        TreeViewAdapter treeViewAdapter = mTreeViewAdapterReference.get();
        Assert.assertTrue(treeViewAdapter != null);

        return treeViewAdapter;
    }

    public int displayedSize() {
        return mNotDoneGroupTreeCollection.displayedSize() + mDividerTreeNode.displayedSize();
    }

    public List<Node> getSelectedNodes() {
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

    public void unselect() {
        mNotDoneGroupTreeCollection.unselect();
    }
}
