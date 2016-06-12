package com.krystianwsul.checkme.gui.instances.tree;

import com.krystianwsul.checkme.gui.instances.GroupListFragment;

public interface NotDoneInstanceModelNode extends Comparable<NotDoneInstanceModelNode> {
    GroupListFragment.GroupAdapter.NodeCollection.NotDoneGroupNode.NotDoneInstanceNode getNotDoneInstanceNode();
    void onClick();
    void onBindViewHolder(GroupListFragment.GroupAdapter.AbstractHolder abstractHolder);
    int getItemViewType();
}
