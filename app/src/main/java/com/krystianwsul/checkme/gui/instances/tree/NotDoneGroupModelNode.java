package com.krystianwsul.checkme.gui.instances.tree;

import com.krystianwsul.checkme.gui.instances.GroupListFragment;
import com.krystianwsul.checkme.loaders.GroupListLoader;
import com.krystianwsul.checkme.utils.InstanceKey;
import com.krystianwsul.checkme.utils.time.ExactTimeStamp;

import java.util.ArrayList;

public interface NotDoneGroupModelNode extends Comparable<NotDoneGroupModelNode> {
    GroupListFragment.GroupAdapter.NodeCollection.NotDoneGroupNode getNotDoneGroupNode();
    void onClick();
    void onBindViewHolder(GroupListFragment.GroupAdapter.AbstractHolder abstractHolder);
    int getItemViewType();
    NotDoneInstanceTreeNode newNotDoneInstanceTreeNode(GroupListLoader.InstanceData instanceData, ArrayList<InstanceKey> selectedNodes);
    void remove(NotDoneInstanceTreeNode notDoneInstanceTreeNode);
    boolean singleInstance();
    GroupListLoader.InstanceData getSingleInstanceData();
    ExactTimeStamp getExactTimeStamp();
}
