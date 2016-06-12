package com.krystianwsul.checkme.gui.instances.tree;

import android.view.View;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.krystianwsul.checkme.gui.SelectionCallback;
import com.krystianwsul.checkme.gui.instances.GroupListFragment;
import com.krystianwsul.checkme.loaders.GroupListLoader;

import junit.framework.Assert;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DividerTreeNode implements GroupListFragment.Node, GroupListFragment.NodeContainer {
    private final DividerModelNode mDividerModelNode;

    private List<DoneTreeNode> mDoneTreeNodes;

    private boolean mDoneExpanded;

    private final WeakReference<TreeNodeCollection> mTreeNodeCollectionReference;

    public DividerTreeNode(DividerModelNode dividerModelNode, boolean expanded, WeakReference<TreeNodeCollection> treeNodeCollectionReference) {
        Assert.assertTrue(dividerModelNode != null);
        Assert.assertTrue(treeNodeCollectionReference != null);

        mDividerModelNode = dividerModelNode;
        mDoneExpanded = expanded;
        mTreeNodeCollectionReference = treeNodeCollectionReference;
    }

    public void setInstanceDatas(ArrayList<GroupListLoader.InstanceData> instanceDatas) {
        Assert.assertTrue(instanceDatas != null);

        mDoneTreeNodes = Stream.of(instanceDatas)
                .map(instanceData -> mDividerModelNode.newDoneTreeNode(instanceData, this))
                .collect(Collectors.toList());

        Collections.sort(mDoneTreeNodes);
    }

    @Override
    public void onBindViewHolder(GroupListFragment.GroupAdapter.AbstractHolder abstractHolder) {
        mDividerModelNode.onBindViewHolder(abstractHolder);
    }

    @Override
    public int getItemViewType() {
        return mDividerModelNode.getItemViewType();
    }

    public int getTotalDoneCount() {
        return mDoneTreeNodes.size();
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean isEmpty() {
        return mDoneTreeNodes.isEmpty();
    }

    @Override
    public int displayedSize() {
        if (mDoneTreeNodes.isEmpty() || mDividerModelNode.hasActionMode()) {
            return 0;
        } else {
            if (mDoneExpanded) {
                return 1 + mDoneTreeNodes.size();
            } else {
                return 1;
            }
        }
    }

    @Override
    public GroupListFragment.Node getNode(int position) {
        Assert.assertTrue(position >= 0);
        Assert.assertTrue(!mDoneTreeNodes.isEmpty());

        if (position == 0) {
            return this;
        } else {
            Assert.assertTrue(mDoneExpanded);
            Assert.assertTrue(position <= mDoneTreeNodes.size());

            GroupListFragment.Node node = mDoneTreeNodes.get(position - 1);
            Assert.assertTrue(node != null);

            return node;
        }
    }

    @Override
    public int getPosition(GroupListFragment.Node node) {
        if (node == this)
            return 0;

        if (!(node instanceof DoneTreeNode))
            return -1;

        DoneTreeNode doneTreeNode = (DoneTreeNode) node;
        if (mDoneTreeNodes.contains(doneTreeNode)) {
            Assert.assertTrue(mDoneExpanded);
            return mDoneTreeNodes.indexOf(doneTreeNode) + 1;
        }

        return -1;
    }

    @SuppressWarnings("RedundantIfStatement")
    public boolean visible() {
        if (mDoneTreeNodes.isEmpty())
            return false;

        SelectionCallback selectionCallback = getSelectionCallback();
        Assert.assertTrue(selectionCallback != null);

        if (selectionCallback.hasActionMode())
            return false;

        return true;
    }

    @SuppressWarnings("SimplifiableIfStatement")
    @Override
    public boolean expanded() {
        return mDoneExpanded;
    }

    public void add(GroupListLoader.InstanceData instanceData, int oldInstancePosition) {
        Assert.assertTrue(instanceData != null);

        TreeNodeCollection treeNodeCollection = getTreeNodeCollection();
        Assert.assertTrue(treeNodeCollection != null);

        TreeViewAdapter treeViewAdapter = getTreeViewAdapter();
        Assert.assertTrue(treeViewAdapter != null);

        if (mDoneExpanded) {
            Assert.assertTrue(!isEmpty());

            int oldDividerPosition = treeNodeCollection.getPosition(this);
            boolean bottomNotDone = (oldInstancePosition == oldDividerPosition);

            DoneTreeNode doneTreeNode = mDividerModelNode.newDoneTreeNode(instanceData, this);
            Assert.assertTrue(doneTreeNode != null);

            mDoneTreeNodes.add(doneTreeNode);

            Collections.sort(mDoneTreeNodes);

            int newInstancePosition = treeNodeCollection.getPosition(doneTreeNode);
            treeViewAdapter.notifyItemInserted(newInstancePosition);

            if (bottomNotDone && treeNodeCollection.mNotDoneGroupTreeCollection.displayedSize() > 0) {
                int newDividerPosition = treeNodeCollection.getPosition(this);
                treeViewAdapter.notifyItemChanged(newDividerPosition - 1);
            }
        } else {
            DoneTreeNode doneTreeNode = mDividerModelNode.newDoneTreeNode(instanceData, this);
            mDoneTreeNodes.add(doneTreeNode);

            Collections.sort(mDoneTreeNodes);

            if (mDoneTreeNodes.size() == 1) {
                Assert.assertTrue(!mDoneExpanded);
                int newDividerPosition = treeNodeCollection.getPosition(this);
                treeViewAdapter.notifyItemInserted(newDividerPosition);

                if (treeNodeCollection.mNotDoneGroupTreeCollection.displayedSize() != 0) {
                    treeViewAdapter.notifyItemChanged(newDividerPosition - 1);
                }
            } else {
                if (mDoneExpanded) {
                    int newInstancePosition = treeNodeCollection.getPosition(doneTreeNode);
                    treeViewAdapter.notifyItemInserted(newInstancePosition);
                }
            }
        }
    }

    public void remove(DoneTreeNode doneTreeNode) {
        Assert.assertTrue(doneTreeNode != null);

        TreeNodeCollection treeNodeCollection = getTreeNodeCollection();
        Assert.assertTrue(treeNodeCollection != null);

        TreeViewAdapter treeViewAdapter = getTreeViewAdapter();
        Assert.assertTrue(treeViewAdapter != null);

        Assert.assertTrue(mDoneTreeNodes.contains(doneTreeNode));

        Assert.assertTrue(displayedSize() > 1);

        if (treeNodeCollection.mNotDoneGroupTreeCollection.displayedSize() == 0) {
            int oldDoneTreePosition = treeNodeCollection.getPosition(doneTreeNode);

            mDoneTreeNodes.remove(doneTreeNode);

            if (mDoneTreeNodes.isEmpty()) {
                mDoneExpanded = false;

                int dividerPosition = treeNodeCollection.getPosition(this);
                Assert.assertTrue(dividerPosition == oldDoneTreePosition - 1);

                treeViewAdapter.notifyItemRangeRemoved(dividerPosition, 2);
            } else {
                treeViewAdapter.notifyItemRemoved(oldDoneTreePosition);
            }
        } else {
            int oldDoneTreePosition = treeNodeCollection.getPosition(doneTreeNode);
            int oldDividerPosition = treeNodeCollection.getPosition(this);

            mDoneTreeNodes.remove(doneTreeNode);

            if (mDoneTreeNodes.isEmpty()) {
                mDoneExpanded = false;

                int dividerPosition = treeNodeCollection.getPosition(this);
                Assert.assertTrue(dividerPosition == oldDoneTreePosition - 1);

                treeViewAdapter.notifyItemRangeRemoved(dividerPosition, 2);

                treeViewAdapter.notifyItemChanged(treeNodeCollection.mNotDoneGroupTreeCollection.displayedSize() - 1);
            } else {
                treeViewAdapter.notifyItemRemoved(oldDoneTreePosition);

                treeViewAdapter.notifyItemChanged(oldDividerPosition - 1);
            }
        }
    }

    private TreeNodeCollection getTreeNodeCollection() {
        TreeNodeCollection treeNodeCollection = mTreeNodeCollectionReference.get();
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

        SelectionCallback selectionCallback = treeViewAdapter.getSelectionCallback();
        Assert.assertTrue(selectionCallback != null);

        return selectionCallback;
    }

    public View.OnClickListener getExpandListener() {
        return v -> {
            Assert.assertTrue(!isEmpty());

            TreeNodeCollection treeNodeCollection = getTreeNodeCollection();
            Assert.assertTrue(treeNodeCollection != null);

            TreeViewAdapter treeViewAdapter = treeNodeCollection.getTreeViewAdapter();
            Assert.assertTrue(treeViewAdapter != null);

            int position = treeNodeCollection.getPosition(this);

            int displayedSize = displayedSize();
            if (mDoneExpanded) { // hiding
                mDoneExpanded = false;
                treeViewAdapter.notifyItemRangeRemoved(position + 1, displayedSize - 1);
            } else { // showing
                mDoneExpanded = true;
                treeViewAdapter.notifyItemRangeInserted(position + 1, displayedSize - 1);
            }

            if (treeNodeCollection.mNotDoneGroupTreeCollection.displayedSize() == 0) {
                treeViewAdapter.notifyItemChanged(position);
            } else {
                treeViewAdapter.notifyItemRangeChanged(position - 1, 2);
            }
        };
    }

    @Override
    public void update() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeFromParent() {
        throw new UnsupportedOperationException();
    }

    public void onCreateActionMode() {
        TreeNodeCollection treeNodeCollection = getTreeNodeCollection();
        Assert.assertTrue(treeNodeCollection != null);

        TreeViewAdapter treeViewAdapter = treeNodeCollection.getTreeViewAdapter();
        Assert.assertTrue(treeViewAdapter != null);

        int oldPosition = treeNodeCollection.getPosition(this);
        Assert.assertTrue(oldPosition > 0);

        if (getTotalDoneCount() > 0) {
            if (expanded())
                treeViewAdapter.notifyItemRangeRemoved(oldPosition, getTotalDoneCount() + 1);
            else
                treeViewAdapter.notifyItemRemoved(oldPosition);
        }

        Assert.assertTrue(treeNodeCollection.mNotDoneGroupTreeCollection.displayedSize() > 0);
        treeViewAdapter.notifyItemChanged(oldPosition - 1);
    }

    public void onDestroyActionMode() {
        TreeNodeCollection treeNodeCollection = getTreeNodeCollection();
        Assert.assertTrue(treeNodeCollection != null);

        TreeViewAdapter treeViewAdapter = treeNodeCollection.getTreeViewAdapter();
        Assert.assertTrue(treeViewAdapter != null);

        int position = treeNodeCollection.getPosition(this);
        Assert.assertTrue(position > 0);

        if (getTotalDoneCount() > 0) {
            if (expanded())
                treeViewAdapter.notifyItemRangeInserted(position, getTotalDoneCount() + 1);
            else
                treeViewAdapter.notifyItemInserted(position);
        }

        Assert.assertTrue(treeNodeCollection.mNotDoneGroupTreeCollection.displayedSize() > 0);
        treeViewAdapter.notifyItemChanged(position - 1);
    }
}
