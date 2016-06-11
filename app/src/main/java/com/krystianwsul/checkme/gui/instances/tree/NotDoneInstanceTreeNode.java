package com.krystianwsul.checkme.gui.instances.tree;

import com.krystianwsul.checkme.gui.instances.GroupListFragment;
import com.krystianwsul.checkme.utils.InstanceKey;

import junit.framework.Assert;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

public class NotDoneInstanceTreeNode implements GroupListFragment.Node {
    private final NotDoneInstanceModelNode mNotDoneInstanceModelNode;

    public WeakReference<NotDoneGroupTreeNode> mNotDoneGroupTreeNodeReference;

    public boolean mSelected = false;

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
}
