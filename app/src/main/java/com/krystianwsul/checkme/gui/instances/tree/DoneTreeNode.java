package com.krystianwsul.checkme.gui.instances.tree;

import android.support.annotation.NonNull;

import com.krystianwsul.checkme.gui.instances.GroupListFragment;

import junit.framework.Assert;

public class DoneTreeNode implements GroupListFragment.Node, Comparable<DoneTreeNode> {
    private final DoneModelNode mDoneModelNode;

    public DoneTreeNode(DoneModelNode doneModelNode) {
        Assert.assertTrue(doneModelNode != null);
        mDoneModelNode = doneModelNode;
    }

    @Override
    public void onBindViewHolder(GroupListFragment.GroupAdapter.AbstractHolder abstractHolder) {
        mDoneModelNode.onBindViewHolder(abstractHolder);
    }

    @Override
    public int getItemViewType() {
        return mDoneModelNode.getItemViewType();
    }

    @Override
    public int compareTo(@NonNull DoneTreeNode another) {
        return mDoneModelNode.compareTo(another.mDoneModelNode);
    }

    @Override
    public void update() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeFromParent() {
        throw new UnsupportedOperationException();
    }
}
