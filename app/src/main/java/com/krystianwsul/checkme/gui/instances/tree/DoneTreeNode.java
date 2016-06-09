package com.krystianwsul.checkme.gui.instances.tree;

import com.krystianwsul.checkme.gui.instances.GroupListFragment;

public class DoneTreeNode implements GroupListFragment.Node {
    public final DoneModelNode mDoneModelNode;

    public DoneTreeNode(DoneModelNode doneModelNode) {
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
}
