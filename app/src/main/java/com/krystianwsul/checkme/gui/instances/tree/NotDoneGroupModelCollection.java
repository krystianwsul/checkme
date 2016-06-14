package com.krystianwsul.checkme.gui.instances.tree;

import com.krystianwsul.checkme.loaders.GroupListLoader;

public interface NotDoneGroupModelCollection {
    int remove(NotDoneGroupTreeNode notDoneGroupTreeNode);
    void add(GroupListLoader.InstanceData instanceData);
}
