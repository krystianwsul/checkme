package com.krystianwsul.checkme.gui.instances.tree;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.krystianwsul.checkme.gui.instances.GroupListFragment;
import com.krystianwsul.checkme.loaders.GroupListLoader;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DividerTreeNode implements GroupListFragment.Node, GroupListFragment.NodeContainer {
    public final DividerModelNode mDividerModelNode;

    private List<DoneTreeNode> mDoneTreeNodes;

    public boolean mDoneExpanded;

    public DividerTreeNode(DividerModelNode dividerModelNode, boolean expanded) {
        Assert.assertTrue(dividerModelNode != null);

        mDividerModelNode = dividerModelNode;

        mDoneExpanded = expanded;
    }

    public void setInstanceDatas(ArrayList<GroupListLoader.InstanceData> instanceDatas) {
        Assert.assertTrue(instanceDatas != null);

        mDoneTreeNodes = Stream.of(instanceDatas)
                .map(instanceData -> mDividerModelNode.newDoneTreeNode(instanceData, this))
                .collect(Collectors.toList());

        Collections.sort(mDoneTreeNodes, mDividerModelNode.getComparator());
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

    @Override
    public boolean expanded() {
        if (!mDoneExpanded)
            return false;

        return !mDividerModelNode.hasActionMode();
    }

    public void add(GroupListLoader.InstanceData instanceData, int oldInstancePosition, GroupListFragment.GroupAdapter.NodeCollection nodeCollection, GroupListFragment.GroupAdapter groupAdapter) {
        Assert.assertTrue(instanceData != null);
        Assert.assertTrue(nodeCollection != null);
        Assert.assertTrue(groupAdapter != null);

        if (mDoneExpanded) {
            Assert.assertTrue(!isEmpty());

            int oldDividerPosition = nodeCollection.getPosition(this);
            boolean bottomNotDone = (oldInstancePosition == oldDividerPosition);

            DoneTreeNode doneTreeNode = mDividerModelNode.newDoneTreeNode(instanceData, this);
            Assert.assertTrue(doneTreeNode != null);

            mDoneTreeNodes.add(doneTreeNode);

            Collections.sort(mDoneTreeNodes, mDividerModelNode.getComparator());

            int newInstancePosition = nodeCollection.getPosition(doneTreeNode.mDoneModelNode.getDoneInstanceNode());
            groupAdapter.notifyItemInserted(newInstancePosition);

            if (bottomNotDone && nodeCollection.mNotDoneGroupCollection.displayedSize() > 0) {
                int newDividerPosition = nodeCollection.getPosition(this);
                groupAdapter.notifyItemChanged(newDividerPosition - 1);
            }
        } else {
            DoneTreeNode doneTreeNode = mDividerModelNode.newDoneTreeNode(instanceData, this);
            mDoneTreeNodes.add(doneTreeNode);

            Collections.sort(mDoneTreeNodes, mDividerModelNode.getComparator());

            if (mDoneTreeNodes.size() == 1) {
                Assert.assertTrue(!mDoneExpanded);
                int newDividerPosition = nodeCollection.getPosition(this);
                groupAdapter.notifyItemInserted(newDividerPosition);

                if (nodeCollection.mNotDoneGroupCollection.displayedSize() != 0) {
                    groupAdapter.notifyItemChanged(newDividerPosition - 1);
                }
            } else {
                if (mDoneExpanded) {
                    int newInstancePosition = nodeCollection.getPosition(doneTreeNode.mDoneModelNode.getDoneInstanceNode());
                    groupAdapter.notifyItemInserted(newInstancePosition);
                }
            }
        }
    }

    public void remove(DoneTreeNode doneTreeNode, GroupListFragment.GroupAdapter.NodeCollection nodeCollection, GroupListFragment.GroupAdapter groupAdapter) {
        Assert.assertTrue(doneTreeNode != null);
        Assert.assertTrue(nodeCollection != null);
        Assert.assertTrue(groupAdapter != null);

        Assert.assertTrue(mDoneTreeNodes.contains(doneTreeNode));

        Assert.assertTrue(displayedSize() > 1);

        if (nodeCollection.mNotDoneGroupCollection.displayedSize() == 0) {
            int oldDoneTreePosition = nodeCollection.getPosition(doneTreeNode);

            mDoneTreeNodes.remove(doneTreeNode);

            if (mDoneTreeNodes.isEmpty()) {
                mDoneExpanded = false;

                int dividerPosition = nodeCollection.getPosition(this);
                Assert.assertTrue(dividerPosition == oldDoneTreePosition - 1);

                groupAdapter.notifyItemRangeRemoved(dividerPosition, 2);
            } else {
                groupAdapter.notifyItemRemoved(oldDoneTreePosition);
            }
        } else {
            int oldDoneTreePosition = nodeCollection.getPosition(doneTreeNode);
            int oldDividerPosition = nodeCollection.getPosition(this);

            mDoneTreeNodes.remove(doneTreeNode);

            if (mDoneTreeNodes.isEmpty()) {
                mDoneExpanded = false;

                int dividerPosition = nodeCollection.getPosition(this);
                Assert.assertTrue(dividerPosition == oldDoneTreePosition - 1);

                groupAdapter.notifyItemRangeRemoved(dividerPosition, 2);

                groupAdapter.notifyItemChanged(nodeCollection.mNotDoneGroupCollection.displayedSize() - 1);
            } else {
                groupAdapter.notifyItemRemoved(oldDoneTreePosition);

                groupAdapter.notifyItemChanged(oldDividerPosition - 1);
            }
        }
    }
}
