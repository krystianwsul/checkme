package com.krystianwsul.checkme.gui.instances.tree;

import com.krystianwsul.checkme.gui.instances.GroupListFragment;

public interface DoneModelNode extends GroupListFragment.Node, Comparable<DoneModelNode> {
    GroupListFragment.GroupAdapter.NodeCollection.DoneInstanceNode getDoneInstanceNode();
}
