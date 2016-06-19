package com.krystianwsul.checkme.gui.instances.tree;

import com.krystianwsul.checkme.gui.instances.GroupListFragment;

import junit.framework.Assert;

public abstract class TreeNode {
    protected final ModelNode mModelNode;

    public TreeNode(ModelNode modelNode) {
        Assert.assertTrue(modelNode != null);

        mModelNode = modelNode;
    }

    public void onBindViewHolder(GroupListFragment.GroupAdapter.AbstractHolder abstractHolder) {
        mModelNode.onBindViewHolder(abstractHolder);
    }

    public int getItemViewType() {
        return mModelNode.getItemViewType();
    }

    public abstract void update();

    public abstract TreeNodeCollection getTreeNodeCollection();

    public abstract boolean expanded();

    public abstract int displayedSize();

    public abstract int getPosition(TreeNode treeNode);
}
