package com.krystianwsul.checkme.gui.instances.tree;

import com.krystianwsul.checkme.gui.instances.GroupListFragment;
import com.krystianwsul.checkme.loaders.GroupListLoader;
import com.krystianwsul.checkme.utils.time.ExactTimeStamp;

public interface NotDoneGroupModelNode extends Comparable<NotDoneGroupModelNode> {
    void onClick();
    void onBindViewHolder(GroupListFragment.GroupAdapter.AbstractHolder abstractHolder);
    int getItemViewType();
    void remove(NotDoneInstanceTreeNode notDoneInstanceTreeNode);
    boolean singleInstance();
    GroupListLoader.InstanceData getSingleInstanceData();
    ExactTimeStamp getExactTimeStamp();
    void addInstanceData(GroupListLoader.InstanceData instanceData);
}
