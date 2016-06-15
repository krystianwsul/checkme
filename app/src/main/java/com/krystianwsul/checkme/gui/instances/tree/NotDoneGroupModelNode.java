package com.krystianwsul.checkme.gui.instances.tree;

import com.krystianwsul.checkme.gui.instances.GroupListFragment;
import com.krystianwsul.checkme.utils.time.ExactTimeStamp;

public interface NotDoneGroupModelNode extends Comparable<NotDoneGroupModelNode> {
    void onClick();
    void onBindViewHolder(GroupListFragment.GroupAdapter.AbstractHolder abstractHolder);
    int getItemViewType();
    ExactTimeStamp getExactTimeStamp();
}
