package com.krystianwsul.checkme.gui.instances.tree;

import com.krystianwsul.checkme.gui.instances.GroupListFragment;

public interface DoneModelNode extends Comparable<DoneModelNode> {
    GroupListFragment.GroupAdapter.NodeCollection.DoneInstanceNode getDoneInstanceNode();
    void onBindViewHolder(GroupListFragment.GroupAdapter.AbstractHolder abstractHolder);
    int getItemViewType();
}
