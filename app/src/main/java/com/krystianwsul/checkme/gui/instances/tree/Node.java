package com.krystianwsul.checkme.gui.instances.tree;

import com.krystianwsul.checkme.gui.instances.GroupListFragment;

public interface Node {
    void onBindViewHolder(GroupListFragment.GroupAdapter.AbstractHolder abstractHolder);
    int getItemViewType();
    void update();
    TreeNodeCollection getTreeNodeCollection();
    boolean expanded();
    int displayedSize();
    int getPosition(Node node);
}
