package com.krystianwsul.checkme.gui.instances.tree;

import com.krystianwsul.checkme.gui.instances.GroupListFragment;

import junit.framework.Assert;

public class NotDoneInstanceTreeNode implements GroupListFragment.Node {
    public final NotDoneInstanceModelNode mNotDoneInstanceModelNode;

    public NotDoneInstanceTreeNode(NotDoneInstanceModelNode notDoneInstanceModelNode) {
        Assert.assertTrue(notDoneInstanceModelNode != null);

        mNotDoneInstanceModelNode = notDoneInstanceModelNode;
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
}
