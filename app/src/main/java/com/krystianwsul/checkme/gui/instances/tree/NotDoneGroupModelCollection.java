package com.krystianwsul.checkme.gui.instances.tree;

import com.krystianwsul.checkme.gui.instances.GroupListFragment;

import java.util.Comparator;

public interface NotDoneGroupModelCollection {
    Comparator<NotDoneGroupTreeNode> getComparator();
    GroupListFragment.GroupAdapter.NodeCollection.NotDoneGroupCollection getNotDoneGroupCollection();
}
