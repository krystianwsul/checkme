package com.krystianwsul.checkme.gui.instances.tree;

import com.krystianwsul.checkme.gui.instances.GroupListFragment;

public interface NotDoneInstanceModelNode extends GroupListFragment.Node, Comparable<NotDoneInstanceModelNode> {
    GroupListFragment.GroupAdapter.NodeCollection.NotDoneGroupNode.NotDoneInstanceNode getNotDoneInstanceNode();
    void onClick();
}
