package com.krystianwsul.checkme.gui.instances.tree;

import com.krystianwsul.checkme.gui.instances.GroupListFragment;

public interface NotDoneGroupModelNode {
    GroupListFragment.GroupAdapter.NodeCollection.NotDoneGroupNode getNotDoneGroupNode();
    void onClick();
    void onBindViewHolder(GroupListFragment.GroupAdapter.AbstractHolder abstractHolder);
    int getItemViewType();
}
