package com.krystianwsul.checkme.gui.instances.tree;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.krystianwsul.checkme.gui.instances.GroupListFragment;
import com.krystianwsul.checkme.loaders.GroupListLoader;
import com.krystianwsul.checkme.utils.InstanceKey;
import com.krystianwsul.checkme.utils.time.ExactTimeStamp;

import junit.framework.Assert;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NotDoneGroupTreeNode implements GroupListFragment.Node, GroupListFragment.NodeContainer {
    private final NotDoneGroupModelNode mNotDoneGroupModelNode;

    public final ArrayList<NotDoneInstanceTreeNode> mNotDoneInstanceTreeNodes = new ArrayList<>();

    public boolean mNotDoneGroupNodeExpanded;

    public ExactTimeStamp mExactTimeStamp;

    public NotDoneGroupTreeNode(NotDoneGroupModelNode notDoneGroupModelNode, boolean expanded) {
        Assert.assertTrue(notDoneGroupModelNode != null);

        mNotDoneGroupModelNode = notDoneGroupModelNode;
        mNotDoneGroupNodeExpanded = expanded;
    }

    @Override
    public void onBindViewHolder(GroupListFragment.GroupAdapter.AbstractHolder abstractHolder) {
        mNotDoneGroupModelNode.onBindViewHolder(abstractHolder);
    }

    @Override
    public int getItemViewType() {
        return mNotDoneGroupModelNode.getItemViewType();
    }

    @Override
    public int displayedSize() {
        if (mNotDoneGroupNodeExpanded) {
            return 1 + mNotDoneInstanceTreeNodes.size();
        } else {
            return 1;
        }
    }

    @Override
    public GroupListFragment.Node getNode(int position) {
        Assert.assertTrue(position >= 0);
        Assert.assertTrue(position < displayedSize());

        if (position == 0)
            return this;

        Assert.assertTrue(mNotDoneGroupNodeExpanded);

        GroupListFragment.Node node = mNotDoneInstanceTreeNodes.get(position - 1);
        Assert.assertTrue(node != null);

        return node;
    }

    @Override
    public int getPosition(GroupListFragment.Node node) {
        if (node == this)
            return 0;

        if (!(node instanceof NotDoneInstanceTreeNode))
            return -1;

        NotDoneInstanceTreeNode notDoneInstanceTreeNode = (NotDoneInstanceTreeNode) node;
        if (mNotDoneInstanceTreeNodes.contains(notDoneInstanceTreeNode)) {
            Assert.assertTrue(mNotDoneGroupNodeExpanded);
            return mNotDoneInstanceTreeNodes.indexOf(notDoneInstanceTreeNode) + 1;
        }

        return -1;
    }

    @Override
    public boolean expanded() {
        return mNotDoneGroupNodeExpanded;
    }

    public void remove(NotDoneInstanceTreeNode notDoneInstanceTreeNode, TreeNodeCollection treeNodeCollection, GroupListFragment.GroupAdapter groupAdapter) {
        Assert.assertTrue(notDoneInstanceTreeNode != null);
        Assert.assertTrue(treeNodeCollection != null);
        Assert.assertTrue(groupAdapter != null);

        Assert.assertTrue(mNotDoneInstanceTreeNodes.size() >= 2);

        final boolean lastInGroup = (mNotDoneInstanceTreeNodes.indexOf(notDoneInstanceTreeNode) == mNotDoneInstanceTreeNodes.size() - 1);

        int groupPosition = treeNodeCollection.getPosition(this);

        int oldInstancePosition = treeNodeCollection.getPosition(notDoneInstanceTreeNode);

        if (mNotDoneInstanceTreeNodes.size() == 2) {
            mNotDoneInstanceTreeNodes.remove(notDoneInstanceTreeNode);

            mNotDoneGroupNodeExpanded = false;

            if ((groupPosition > 0) && (treeNodeCollection.getNode(groupPosition - 1) instanceof NotDoneGroupTreeNode))
                groupAdapter.notifyItemRangeChanged(groupPosition - 1, 2);
            else
                groupAdapter.notifyItemChanged(groupPosition);

            groupAdapter.notifyItemRangeRemoved(groupPosition + 1, 2);
        } else {
            Assert.assertTrue(mNotDoneInstanceTreeNodes.size() > 2);

            mNotDoneInstanceTreeNodes.remove(notDoneInstanceTreeNode);

            groupAdapter.notifyItemChanged(groupPosition);
            groupAdapter.notifyItemRemoved(oldInstancePosition);

            if (lastInGroup)
                groupAdapter.notifyItemChanged(oldInstancePosition - 1);
        }
    }

    public Stream<NotDoneInstanceTreeNode> getSelected() {
        return Stream.of(mNotDoneInstanceTreeNodes)
                .filter(notDoneInstanceNode -> notDoneInstanceNode.getNotDoneInstanceNode().mSelected);
    }

    public Stream<GroupListFragment.Node> getSelectedNodes() {
        if (mNotDoneInstanceTreeNodes.size() == 1) {
            ArrayList<GroupListFragment.Node> selectedNodes = new ArrayList<>();
            if (mNotDoneInstanceTreeNodes.get(0).getNotDoneInstanceNode().mSelected)
                selectedNodes.add(this);
            return Stream.of(selectedNodes);
        } else {
            return Stream.of(Stream.of(mNotDoneInstanceTreeNodes)
                    .filter(notDoneInstanceTreeNode -> notDoneInstanceTreeNode.getNotDoneInstanceNode().mSelected)
                    .collect(Collectors.toList()));
        }
    }

    public void unselect(TreeNodeCollection treeNodeCollection, GroupListFragment.GroupAdapter groupAdapter) {
        Assert.assertTrue(treeNodeCollection != null);
        Assert.assertTrue(groupAdapter != null);

        if (singleInstance()) {
            GroupListFragment.GroupAdapter.NodeCollection.NotDoneGroupNode.NotDoneInstanceNode notDoneInstanceNode = mNotDoneInstanceTreeNodes.get(0).getNotDoneInstanceNode();
            Assert.assertTrue(notDoneInstanceNode != null);

            if (notDoneInstanceNode.mSelected) {
                notDoneInstanceNode.mSelected = false;
                groupAdapter.notifyItemChanged(treeNodeCollection.getPosition(this));
            }
        } else {
            List<NotDoneInstanceTreeNode> selected = getSelected().collect(Collectors.toList());
            if (!selected.isEmpty()) {
                Assert.assertTrue(mNotDoneGroupNodeExpanded);

                for (NotDoneInstanceTreeNode notDoneInstanceTreeNode : selected) {
                    notDoneInstanceTreeNode.getNotDoneInstanceNode().mSelected = false;
                    groupAdapter.notifyItemChanged(treeNodeCollection.getPosition(notDoneInstanceTreeNode));
                }

                groupAdapter.notifyItemChanged(treeNodeCollection.getPosition(this));
            }
        }
    }

    public boolean singleInstance() {
        Assert.assertTrue(!mNotDoneInstanceTreeNodes.isEmpty());
        return (mNotDoneInstanceTreeNodes.size() == 1);
    }

    public void updateCheckBoxes(TreeNodeCollection treeNodeCollection, GroupListFragment.GroupAdapter groupAdapter) {
        Assert.assertTrue(treeNodeCollection != null);
        Assert.assertTrue(groupAdapter != null);

        if (singleInstance()) {
            groupAdapter.notifyItemChanged(treeNodeCollection.getPosition(this));
        } else {
            groupAdapter.notifyItemRangeChanged(treeNodeCollection.getPosition(this) + 1, displayedSize() - 1);
        }
    }

    public void sort() {
        Collections.sort(mNotDoneInstanceTreeNodes, mNotDoneGroupModelNode.getComparator());
    }

    public GroupListLoader.InstanceData getSingleInstanceData() {
        Assert.assertTrue(mNotDoneInstanceTreeNodes.size() == 1);
        return mNotDoneInstanceTreeNodes.get(0).getNotDoneInstanceNode().mInstanceData;
    }

    public void setInstanceDatas(ArrayList<GroupListLoader.InstanceData> instanceDatas, ArrayList<InstanceKey> selectedNodes) {
        Assert.assertTrue(instanceDatas != null);
        Assert.assertTrue(!instanceDatas.isEmpty());
        Assert.assertTrue(instanceDatas.size() > 1 || !mNotDoneGroupNodeExpanded);

        mExactTimeStamp = instanceDatas.get(0).InstanceTimeStamp.toExactTimeStamp();
        for (GroupListLoader.InstanceData instanceData : instanceDatas) {
            Assert.assertTrue(mExactTimeStamp.equals(instanceData.InstanceTimeStamp.toExactTimeStamp()));
            addInstanceData(instanceData, selectedNodes);
        }
        sort();
    }

    public NotDoneInstanceTreeNode addInstanceData(GroupListLoader.InstanceData instanceData, ArrayList<InstanceKey> selectedNodes) {
        Assert.assertTrue(instanceData != null);

        GroupListFragment.GroupAdapter.NodeCollection.NotDoneGroupNode.NotDoneInstanceNode notDoneInstanceNode = new GroupListFragment.GroupAdapter.NodeCollection.NotDoneGroupNode.NotDoneInstanceNode(instanceData, new WeakReference<>(mNotDoneGroupModelNode.getNotDoneGroupNode()), selectedNodes);
        NotDoneInstanceTreeNode notDoneInstanceTreeNode = new NotDoneInstanceTreeNode(notDoneInstanceNode.getNotDoneInstanceModelNode());
        notDoneInstanceNode.setNotDoneInstanceTreeNodeReference(new WeakReference<>(notDoneInstanceTreeNode));
        notDoneInstanceTreeNode.setNotDoneGroupTreeNodeReference(new WeakReference<>(this));

        mNotDoneInstanceTreeNodes.add(notDoneInstanceTreeNode);
        return notDoneInstanceTreeNode;
    }
}
