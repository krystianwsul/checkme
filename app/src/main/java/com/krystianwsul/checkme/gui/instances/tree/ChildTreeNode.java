package com.krystianwsul.checkme.gui.instances.tree;

import android.support.annotation.NonNull;
import android.view.View;

import com.krystianwsul.checkme.gui.SelectionCallback;
import com.krystianwsul.checkme.gui.instances.GroupListFragment;

import junit.framework.Assert;

import java.lang.ref.WeakReference;
import java.util.List;

public class ChildTreeNode extends TreeNode implements Comparable<ChildTreeNode> {
    private final WeakReference<NodeContainer> mParentReference;
    private final ChildModelNode mChildModelNode;

    private List<ChildTreeNode> mChildTreeNodes;

    private boolean mExpanded;

    private boolean mSelected = false;

    public ChildTreeNode(ChildModelNode childModelNode, WeakReference<NodeContainer> parentReference, boolean expanded, boolean selected) {
        Assert.assertTrue(childModelNode != null);
        Assert.assertTrue(parentReference != null);

        mChildModelNode = childModelNode;
        mParentReference = parentReference;
        mExpanded = expanded;
        mSelected = selected;

        Assert.assertTrue(!mSelected || mChildModelNode.selectable());
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

        NodeContainer parent = getParent();
        Assert.assertTrue(parent != null);

        mSelected = !mSelected;

        if (mSelected) {
            selectionCallback.incrementSelected();

            if (parent.getSelectedChildren().size() == 1) // first in group
                parent.update();
        } else {
            selectionCallback.decrementSelected();

            if (parent.getSelectedChildren().size() == 0) // last in group
                parent.update();
        }

        treeViewAdapter.notifyItemChanged(treeNodeCollection.getPosition(this));
    }

    public TreeNodeCollection getTreeNodeCollection() {
        NodeContainer parent = getParent();
        Assert.assertTrue(parent != null);

        TreeNodeCollection treeNodeCollection = parent.getTreeNodeCollection();
        Assert.assertTrue(treeNodeCollection != null);

        return treeNodeCollection;
    }

    private NodeContainer getParent() {
        NodeContainer parent = mParentReference.get();
        Assert.assertTrue(parent != null);

        return parent;
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

    @Override
    public void update() {
        TreeNodeCollection treeNodeCollection = getTreeNodeCollection();
        Assert.assertTrue(treeNodeCollection != null);

        TreeViewAdapter treeViewAdapter = treeNodeCollection.getTreeViewAdapter();
        Assert.assertTrue(treeViewAdapter != null);

        treeViewAdapter.notifyItemChanged(treeNodeCollection.getPosition(this));
    }

    public boolean getSeparatorVisibility() {
        NodeContainer parent = getParent();
        Assert.assertTrue(parent != null);

        Assert.assertTrue(parent.expanded());

        TreeNodeCollection treeNodeCollection = getTreeNodeCollection();
        Assert.assertTrue(treeNodeCollection != null);

        boolean lastInGroup = (parent.getPosition(this) == parent.displayedSize() - 1);

        boolean lastInAdapter = (treeNodeCollection.getPosition(this) == treeNodeCollection.displayedSize() - 1);

        return (lastInGroup && !lastInAdapter);
    }

    @Override
    public boolean expanded() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int displayedSize() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getPosition(TreeNode treeNode) {
        throw new UnsupportedOperationException();
    }
}
