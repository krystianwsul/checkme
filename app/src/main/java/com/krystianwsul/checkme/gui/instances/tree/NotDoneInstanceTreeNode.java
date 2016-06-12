package com.krystianwsul.checkme.gui.instances.tree;

import android.support.annotation.NonNull;
import android.view.View;

import com.krystianwsul.checkme.gui.SelectionCallback;
import com.krystianwsul.checkme.gui.instances.GroupListFragment;
import com.krystianwsul.checkme.utils.InstanceKey;

import junit.framework.Assert;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

public class NotDoneInstanceTreeNode implements GroupListFragment.Node, Comparable<NotDoneInstanceTreeNode> {
    private final NotDoneInstanceModelNode mNotDoneInstanceModelNode;

    public WeakReference<NotDoneGroupTreeNode> mNotDoneGroupTreeNodeReference;

    private boolean mSelected = false;

    public NotDoneInstanceTreeNode(NotDoneInstanceModelNode notDoneInstanceModelNode, ArrayList<InstanceKey> selectedNodes) {
        Assert.assertTrue(notDoneInstanceModelNode != null);

        mNotDoneInstanceModelNode = notDoneInstanceModelNode;

        if (selectedNodes != null && selectedNodes.contains(mNotDoneInstanceModelNode.getNotDoneInstanceNode().mInstanceData.InstanceKey)) {
            mSelected = true;
        }
    }

    public void setNotDoneGroupTreeNodeReference(WeakReference<NotDoneGroupTreeNode> notDoneGroupTreeNodeReference) {
        Assert.assertTrue(notDoneGroupTreeNodeReference != null);

        mNotDoneGroupTreeNodeReference = notDoneGroupTreeNodeReference;
    }

    @Override
    public void onBindViewHolder(GroupListFragment.GroupAdapter.AbstractHolder abstractHolder) {
        mNotDoneInstanceModelNode.onBindViewHolder(abstractHolder);
    }

    @Override
    public int getItemViewType() {
        return mNotDoneInstanceModelNode.getItemViewType();
    }

    public GroupListFragment.GroupAdapter.NodeCollection.NotDoneGroupNode.NotDoneInstanceNode getNotDoneInstanceNode() {
        return mNotDoneInstanceModelNode.getNotDoneInstanceNode();
    }

    private NotDoneGroupTreeNode getNotDoneGroupTreeNode() {
        NotDoneGroupTreeNode notDoneGroupTreeNode = mNotDoneGroupTreeNodeReference.get();
        Assert.assertTrue(notDoneGroupTreeNode != null);

        return notDoneGroupTreeNode;
    }

    private TreeNodeCollection getTreeNodeCollection() {
        NotDoneGroupTreeNode notDoneGroupTreeNode = getNotDoneGroupTreeNode();
        Assert.assertTrue(notDoneGroupTreeNode != null);

        TreeNodeCollection treeNodeCollection = notDoneGroupTreeNode.getTreeNodeCollection();
        Assert.assertTrue(treeNodeCollection != null);

        return treeNodeCollection;
    }

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

        return treeViewAdapter.getSelectionCallback();
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
                mNotDoneInstanceModelNode.onClick();
        };
    }

    private void onLongClick() {
        NotDoneGroupTreeNode notDoneGroupTreeNode = getNotDoneGroupTreeNode();
        Assert.assertTrue(notDoneGroupTreeNode != null);
        Assert.assertTrue(notDoneGroupTreeNode.expanded());

        TreeNodeCollection treeNodeCollection = notDoneGroupTreeNode.getTreeNodeCollection();
        Assert.assertTrue(treeNodeCollection != null);

        TreeViewAdapter treeViewAdapter = treeNodeCollection.getTreeViewAdapter();
        Assert.assertTrue(treeViewAdapter != null);

        SelectionCallback selectionCallback = treeViewAdapter.getSelectionCallback();
        Assert.assertTrue(selectionCallback != null);

        mSelected = !mSelected;

        if (mSelected) {
            selectionCallback.incrementSelected();

            if (notDoneGroupTreeNode.getSelectedNodes().count() == 1) // first in group
                treeViewAdapter.notifyItemChanged(treeNodeCollection.getPosition(notDoneGroupTreeNode));
        } else {
            selectionCallback.decrementSelected();

            if (notDoneGroupTreeNode.getSelectedNodes().count() == 0) // last in group
                treeViewAdapter.notifyItemChanged(treeNodeCollection.getPosition(notDoneGroupTreeNode));
        }

        treeViewAdapter.notifyItemChanged(treeNodeCollection.getPosition(this));
    }

    @Override
    public int compareTo(@NonNull NotDoneInstanceTreeNode another) {
        return mNotDoneInstanceModelNode.compareTo(another.mNotDoneInstanceModelNode);
    }

    public boolean isSelected() {
        return mSelected;
    }

    public void unselect() {
        TreeNodeCollection treeNodeCollection = getTreeNodeCollection();
        Assert.assertTrue(treeNodeCollection != null);

        TreeViewAdapter treeViewAdapter = treeNodeCollection.getTreeViewAdapter();
        Assert.assertTrue(treeViewAdapter != null);

        if (mSelected) {
            mSelected = false;
            treeViewAdapter.notifyItemChanged(treeNodeCollection.getPosition(this));
        }
    }

    public boolean getSeparatorVisibility() {
        NotDoneGroupTreeNode notDoneGroupTreeNode = getNotDoneGroupTreeNode();
        Assert.assertTrue(notDoneGroupTreeNode != null);

        Assert.assertTrue(notDoneGroupTreeNode.expanded());

        TreeNodeCollection treeNodeCollection = notDoneGroupTreeNode.getTreeNodeCollection();
        Assert.assertTrue(treeNodeCollection != null);

        boolean lastInGroup = (notDoneGroupTreeNode.mNotDoneInstanceTreeNodes.indexOf(this) == notDoneGroupTreeNode.mNotDoneInstanceTreeNodes.size() - 1);

        boolean lastInAdapter = (treeNodeCollection.getPosition(this) == treeNodeCollection.displayedSize() - 1);

        return (lastInGroup && !lastInAdapter);
    }

    @Override
    public void update() {
        TreeNodeCollection treeNodeCollection = getTreeNodeCollection();
        Assert.assertTrue(treeNodeCollection != null);

        TreeViewAdapter treeViewAdapter = treeNodeCollection.getTreeViewAdapter();
        Assert.assertTrue(treeViewAdapter != null);

        treeViewAdapter.notifyItemChanged(treeNodeCollection.getPosition(this));
    }

    @Override
    public void removeFromParent() {
        NotDoneGroupTreeNode notDoneGroupTreeNode = getNotDoneGroupTreeNode();
        Assert.assertTrue(notDoneGroupTreeNode != null);

        notDoneGroupTreeNode.remove(this);
    }
}
