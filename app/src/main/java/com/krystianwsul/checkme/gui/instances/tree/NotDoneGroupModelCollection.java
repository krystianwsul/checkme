package com.krystianwsul.checkme.gui.instances.tree;

import com.krystianwsul.checkme.gui.instances.GroupListFragment;
import com.krystianwsul.checkme.loaders.GroupListLoader;
import com.krystianwsul.checkme.utils.InstanceKey;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public interface NotDoneGroupModelCollection {
    GroupListFragment.GroupAdapter.NodeCollection.NotDoneGroupCollection getNotDoneGroupCollection();
    NotDoneGroupTreeNode newNotDoneGroupNode(WeakReference<GroupListFragment.GroupAdapter.NodeCollection.NotDoneGroupCollection> notDoneGroupCollectionReference, List<GroupListLoader.InstanceData> instanceDatas, boolean expanded, ArrayList<InstanceKey> selectedNodes);
    int remove(NotDoneGroupTreeNode notDoneGroupTreeNode);
    void add(GroupListLoader.InstanceData instanceData);
}
