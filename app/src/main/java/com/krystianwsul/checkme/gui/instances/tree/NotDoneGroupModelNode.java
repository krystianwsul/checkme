package com.krystianwsul.checkme.gui.instances.tree;

import com.krystianwsul.checkme.gui.instances.GroupListFragment;

import java.util.Comparator;

public interface NotDoneGroupModelNode extends GroupListFragment.Node {
    GroupListFragment.GroupAdapter.NodeCollection.NotDoneGroupNode getNotDoneGroupNode();
    Comparator<NotDoneInstanceTreeNode> getComparator();
}
