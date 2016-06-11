package com.krystianwsul.checkme.gui.instances.tree;

import com.krystianwsul.checkme.gui.instances.GroupListFragment;

public interface NotDoneGroupModelNode extends GroupListFragment.Node {
    GroupListFragment.GroupAdapter.NodeCollection.NotDoneGroupNode getNotDoneGroupNode();
    void onClick();
}
