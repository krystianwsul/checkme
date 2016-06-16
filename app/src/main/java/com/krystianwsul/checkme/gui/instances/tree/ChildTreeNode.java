package com.krystianwsul.checkme.gui.instances.tree;

import android.support.annotation.NonNull;
import android.view.View;

import com.krystianwsul.checkme.gui.SelectionCallback;
import com.krystianwsul.checkme.gui.instances.GroupListFragment;
import com.krystianwsul.checkme.utils.InstanceKey;

import junit.framework.Assert;

import java.util.ArrayList;

public abstract class ChildTreeNode implements GroupListFragment.Node, Comparable<ChildTreeNode> {
    protected final ChildModelNode mChildModelNode;

    private boolean mSelected = false;

    public ChildTreeNode(ChildModelNode childModelNode, ArrayList<InstanceKey> selectedNodes) {
        Assert.assertTrue(childModelNode != null);

        mChildModelNode = childModelNode;

        if (selectedNodes != null && mChildModelNode.isSelected(selectedNodes)) {
            Assert.assertTrue(mChildModelNode.selectable());
            mSelected = true;
        }
    }

    @Override
    public void onBindViewHolder(GroupListFragment.GroupAdapter.AbstractHolder abstractHolder) {
        mChildModelNode.onBindViewHolder(abstractHolder);
    }

    @Override
    public int getItemViewType() {
        return mChildModelNode.getItemViewType();
    }

    @Override
    public int compareTo(@NonNull ChildTreeNode another) {
        return mChildModelNode.compareTo(another.mChildModelNode);
    }

    public View.OnLongClickListener getOnLongClickListener() {
        return v -> {
            onLongClick();
            return true;
        };
    }

    public View.OnClickListener getOnClickListener() {
        SelectionCallback selectionCallback = getSelectionCallback();
        Assert.assertTrue(selectionCallback != null);

        return v -> {
            if (selectionCallback.hasActionMode())
                onLongClick();
            else
                mChildModelNode.onClick();
        };
    }

    private void onLongClick() {
        if (!mChildModelNode.selectable())
            return;

        TreeNodeCollection treeNodeCollection = getTreeNodeCollection();
        Assert.assertTrue(treeNodeCollection != null);

        TreeViewAdapter treeViewAdapter = treeNodeCollection.getTreeViewAdapter();
        Assert.assertTrue(treeViewAdapter != null);

        SelectionCallback selectionCallback = treeViewAdapter.getSelectionCallback();
        Assert.assertTrue(selectionCallback != null);

        GroupListFragment.Node parent = getParent();
        Assert.assertTrue(parent != null);

        mSelected = !mSelected;

        if (mSelected) {
            selectionCallback.incrementSelected();

            if (parent.getSelectedChildren().size() == 1) // first in group
                treeViewAdapter.notifyItemChanged(treeNodeCollection.getPosition(parent));
        } else {
            selectionCallback.decrementSelected();

            if (parent.getSelectedChildren().size() == 0) // last in group
                treeViewAdapter.notifyItemChanged(treeNodeCollection.getPosition(parent));
        }

        treeViewAdapter.notifyItemChanged(treeNodeCollection.getPosition(this));
    }

    protected abstract TreeNodeCollection getTreeNodeCollection();

    protected abstract GroupListFragment.Node getParent();

    private TreeViewAdapter getTreeViewAdapter() {
        TreeNodeCollection treeNodeCollection = getTreeNodeCollection();
        Assert.assertTrue(treeNodeCollection != null);

        TreeViewAdapter treeViewAdapter = treeNodeCollection.getTreeViewAdapter();
        Assert.assertTrue(treeViewAdapter != null);

        return treeViewAdapter;
    }

    private SelectionCallback getSelectionCallback() {
        TreeViewAdapter treeViewAdapter = getTreeViewAdapter();
        Assert.assertTrue(treeViewAdapter != null);

        SelectionCallback selectionCallback = treeViewAdapter.getSelectionCallback();
        Assert.assertTrue(selectionCallback != null);

        return selectionCallback;
    }

    public ChildModelNode getChildModelNode() {
        return mChildModelNode;
    }

    public boolean isSelected() {
        Assert.assertTrue(!mSelected || mChildModelNode.selectable());

        return mSelected;
    }

    public void unselect() {
        TreeNodeCollection treeNodeCollection = getTreeNodeCollection();
        Assert.assertTrue(treeNodeCollection != null);

        TreeViewAdapter treeViewAdapter = treeNodeCollection.getTreeViewAdapter();
        Assert.assertTrue(treeViewAdapter != null);

        if (mSelected) {
            Assert.assertTrue(mChildModelNode.selectable());

            mSelected = false;
            treeViewAdapter.notifyItemChanged(treeNodeCollection.getPosition(this));
        }
    }
}
