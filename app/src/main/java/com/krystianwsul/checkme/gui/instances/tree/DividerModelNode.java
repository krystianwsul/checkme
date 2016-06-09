package com.krystianwsul.checkme.gui.instances.tree;

import com.krystianwsul.checkme.gui.instances.GroupListFragment;
import com.krystianwsul.checkme.loaders.GroupListLoader;

import java.util.Comparator;

public interface DividerModelNode extends GroupListFragment.Node {
    boolean hasActionMode();
    DoneTreeNode newDoneTreeNode(GroupListLoader.InstanceData instanceData, DividerTreeNode dividerTreeNode);
    Comparator<DoneTreeNode> getComparator();
}
