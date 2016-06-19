package com.krystianwsul.checkme.gui.instances.tree;

import com.krystianwsul.checkme.gui.instances.GroupListFragment;

public abstract class TreeNode {
    public abstract void onBindViewHolder(GroupListFragment.GroupAdapter.AbstractHolder abstractHolder);

    public abstract int getItemViewType();

    public abstract void update();

    public abstract TreeNodeCollection getTreeNodeCollection();

    public abstract boolean expanded();

    public abstract int displayedSize();

    public abstract int getPosition(TreeNode treeNode);
}
