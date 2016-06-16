package com.krystianwsul.checkme.gui.instances.tree;

import com.krystianwsul.checkme.gui.instances.GroupListFragment;
import com.krystianwsul.checkme.utils.InstanceKey;

import java.util.List;

public interface ChildModelNode extends Comparable<ChildModelNode> {
    void onBindViewHolder(GroupListFragment.GroupAdapter.AbstractHolder abstractHolder);
    int getItemViewType();

    boolean isSelected(List<InstanceKey> instanceKeys);

    boolean selectable();

    void onClick();
}
