package com.krystianwsul.checkme.gui.instances.tree;

import android.support.annotation.NonNull;

import com.krystianwsul.checkme.gui.instances.GroupListFragment;

import junit.framework.Assert;

public class DoneTreeNode extends ChildTreeNode implements GroupListFragment.Node, Comparable<DoneTreeNode> {
    private final ChildModelNode mChildModelNode;

    public DoneTreeNode(ChildModelNode childModelNode) {
        Assert.assertTrue(childModelNode != null);
        mChildModelNode = childModelNode;
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
    public int compareTo(@NonNull DoneTreeNode another) {
        return mChildModelNode.compareTo(another.mChildModelNode);
    }

    @Override
    public void update() {
        throw new UnsupportedOperationException();
    }
}
