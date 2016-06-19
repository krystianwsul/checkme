package com.krystianwsul.checkme.gui.instances.tree;

import com.krystianwsul.checkme.gui.instances.GroupListFragment;

public interface ModelNode extends Comparable<ModelNode> {
    void onBindViewHolder(GroupListFragment.GroupAdapter.AbstractHolder abstractHolder);

    int getItemViewType();

    boolean selectable();

    void onClick();

    boolean visibleWhenEmpty();

    boolean visibleDuringActionMode();
}
