package com.krystianwsul.checkme.gui.instances.tree;

import com.krystianwsul.checkme.gui.instances.GroupListFragment;

import junit.framework.Assert;

import java.lang.ref.WeakReference;

public class NotDoneInstanceTreeNode implements GroupListFragment.Node {
    private final NotDoneInstanceModelNode mNotDoneInstanceModelNode;

    public WeakReference<NotDoneGroupTreeNode> mNotDoneGroupTreeNodeReference;

    public NotDoneInstanceTreeNode(NotDoneInstanceModelNode notDoneInstanceModelNode) {
        Assert.assertTrue(notDoneInstanceModelNode != null);

        mNotDoneInstanceModelNode = notDoneInstanceModelNode;
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
}
