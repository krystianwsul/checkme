package com.krystianwsul.checkme.gui.instances.tree;

import com.krystianwsul.checkme.gui.instances.GroupListFragment;
import com.krystianwsul.checkme.loaders.GroupListLoader;

public interface DividerModelNode {
    boolean hasActionMode();
    void onBindViewHolder(GroupListFragment.GroupAdapter.AbstractHolder abstractHolder);
    int getItemViewType();
    void add(GroupListLoader.InstanceData instanceData, int oldInstancePosition);
}
