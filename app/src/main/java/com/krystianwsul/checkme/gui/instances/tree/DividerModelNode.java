package com.krystianwsul.checkme.gui.instances.tree;

import com.krystianwsul.checkme.gui.instances.GroupListFragment;
import com.krystianwsul.checkme.loaders.GroupListLoader;

public interface DividerModelNode {
    boolean hasActionMode();
    DoneTreeNode newDoneTreeNode(GroupListLoader.InstanceData instanceData, DividerTreeNode dividerTreeNode);
    void onBindViewHolder(GroupListFragment.GroupAdapter.AbstractHolder abstractHolder);
    int getItemViewType();
}
