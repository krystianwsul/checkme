package com.krystianwsul.checkme.gui.instances.tree;

import com.krystianwsul.checkme.gui.instances.GroupListFragment;

import java.util.List;

public interface Node {
    void onBindViewHolder(GroupListFragment.GroupAdapter.AbstractHolder abstractHolder);

    int getItemViewType();

    void update();

    List<Node> getSelectedChildren();
}
