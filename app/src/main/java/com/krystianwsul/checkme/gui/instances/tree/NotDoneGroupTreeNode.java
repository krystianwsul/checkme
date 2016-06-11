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

    private final WeakReference<NotDoneGroupTreeCollection> mNotDoneGroupTreeCollectionReference;

    public final ArrayList<NotDoneInstanceTreeNode> mNotDoneInstanceTreeNodes = new ArrayList<>();

    public boolean mNotDoneGroupNodeExpanded;

    public ExactTimeStamp mExactTimeStamp;

    public NotDoneGroupTreeNode(NotDoneGroupModelNode notDoneGroupModelNode, boolean expanded, WeakReference<NotDoneGroupTreeCollection> notDoneGroupTreeCollectionReference) {
        Assert.assertTrue(notDoneGroupModelNode != null);
        Assert.assertTrue(notDoneGroupTreeCollectionReference != null);

        mNotDoneGroupModelNode = notDoneGroupModelNode;
        mNotDoneGroupNodeExpanded = expanded;
        mNotDoneGroupTreeCollectionReference = notDoneGroupTreeCollectionReference;
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

    public void remove(NotDoneInstanceTreeNode notDoneInstanceTreeNode, TreeNodeCollection treeNodeCollection, TreeViewAdapter treeViewAdapter) {
        Assert.assertTrue(notDoneInstanceTreeNode != null);
        Assert.assertTrue(treeNodeCollection != null);
        Assert.assertTrue(treeViewAdapter != null);

        Assert.assertTrue(mNotDoneInstanceTreeNodes.size() >= 2);

        final boolean lastInGroup = (mNotDoneInstanceTreeNodes.indexOf(notDoneInstanceTreeNode) == mNotDoneInstanceTreeNodes.size() - 1);

        int groupPosition = treeNodeCollection.getPosition(this);

        int oldInstancePosition = treeNodeCollection.getPosition(notDoneInstanceTreeNode);

        if (mNotDoneInstanceTreeNodes.size() == 2) {
            mNotDoneInstanceTreeNodes.remove(notDoneInstanceTreeNode);

            mNotDoneGroupNodeExpanded = false;

            if ((groupPosition > 0) && (treeNodeCollection.getNode(groupPosition - 1) instanceof NotDoneGroupTreeNode))
                treeViewAdapter.notifyItemRangeChanged(groupPosition - 1, 2);
            else
                treeViewAdapter.notifyItemChanged(groupPosition);

            treeViewAdapter.notifyItemRangeRemoved(groupPosition + 1, 2);
        } else {
            Assert.assertTrue(mNotDoneInstanceTreeNodes.size() > 2);

            mNotDoneInstanceTreeNodes.remove(notDoneInstanceTreeNode);

            treeViewAdapter.notifyItemChanged(groupPosition);
            treeViewAdapter.notifyItemRemoved(oldInstancePosition);

            if (lastInGroup)
                treeViewAdapter.notifyItemChanged(oldInstancePosition - 1);
        }
    }

    public Stream<NotDoneInstanceTreeNode> getSelected() {
        return Stream.of(mNotDoneInstanceTreeNodes)
                .filter(notDoneInstanceTreeNode -> notDoneInstanceTreeNode.mSelected);
    }

    public Stream<GroupListFragment.Node> getSelectedNodes() {
        if (mNotDoneInstanceTreeNodes.size() == 1) {
            ArrayList<GroupListFragment.Node> selectedNodes = new ArrayList<>();
            if (mNotDoneInstanceTreeNodes.get(0).mSelected)
                selectedNodes.add(this);
            return Stream.of(selectedNodes);
        } else {
            return Stream.of(Stream.of(mNotDoneInstanceTreeNodes)
                    .filter(notDoneInstanceTreeNode -> notDoneInstanceTreeNode.mSelected)
                    .collect(Collectors.toList()));
        }
    }

    public void unselect(TreeNodeCollection treeNodeCollection, TreeViewAdapter treeViewAdapter) {
        Assert.assertTrue(treeNodeCollection != null);
        Assert.assertTrue(treeViewAdapter != null);

        if (singleInstance()) {
            NotDoneInstanceTreeNode notDoneInstanceTreeNode = mNotDoneInstanceTreeNodes.get(0);
            Assert.assertTrue(notDoneInstanceTreeNode != null);

            if (notDoneInstanceTreeNode.mSelected) {
                notDoneInstanceTreeNode.mSelected = false;
                treeViewAdapter.notifyItemChanged(treeNodeCollection.getPosition(this));
            }
        } else {
            List<NotDoneInstanceTreeNode> selected = getSelected().collect(Collectors.toList());
            if (!selected.isEmpty()) {
                Assert.assertTrue(mNotDoneGroupNodeExpanded);

                for (NotDoneInstanceTreeNode notDoneInstanceTreeNode : selected) {
                    notDoneInstanceTreeNode.mSelected = false;
                    treeViewAdapter.notifyItemChanged(treeNodeCollection.getPosition(notDoneInstanceTreeNode));
                }

                treeViewAdapter.notifyItemChanged(treeNodeCollection.getPosition(this));
            }
        }
    }

    public boolean singleInstance() {
        Assert.assertTrue(!mNotDoneInstanceTreeNodes.isEmpty());
        return (mNotDoneInstanceTreeNodes.size() == 1);
    }

    public void updateCheckBoxes(TreeNodeCollection treeNodeCollection, TreeViewAdapter treeViewAdapter) {
        Assert.assertTrue(treeNodeCollection != null);
        Assert.assertTrue(treeViewAdapter != null);

        if (singleInstance()) {
            treeViewAdapter.notifyItemChanged(treeNodeCollection.getPosition(this));
        } else {
            treeViewAdapter.notifyItemRangeChanged(treeNodeCollection.getPosition(this) + 1, displayedSize() - 1);
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

        GroupListFragment.GroupAdapter.NodeCollection.NotDoneGroupNode.NotDoneInstanceNode notDoneInstanceNode = new GroupListFragment.GroupAdapter.NodeCollection.NotDoneGroupNode.NotDoneInstanceNode(instanceData, new WeakReference<>(mNotDoneGroupModelNode.getNotDoneGroupNode()));
        NotDoneInstanceTreeNode notDoneInstanceTreeNode = new NotDoneInstanceTreeNode(notDoneInstanceNode.getNotDoneInstanceModelNode(), selectedNodes);
        notDoneInstanceNode.setNotDoneInstanceTreeNodeReference(new WeakReference<>(notDoneInstanceTreeNode));
        notDoneInstanceTreeNode.setNotDoneGroupTreeNodeReference(new WeakReference<>(this));

        mNotDoneInstanceTreeNodes.add(notDoneInstanceTreeNode);
        return notDoneInstanceTreeNode;
    }

    private NotDoneGroupTreeCollection getNotDoneGroupTreeCollection() {
        NotDoneGroupTreeCollection notDoneGroupTreeCollection = mNotDoneGroupTreeCollectionReference.get();
        Assert.assertTrue(notDoneGroupTreeCollection != null);

        return notDoneGroupTreeCollection;
    }

    public TreeViewAdapter getTreeViewAdapter() {
        NotDoneGroupTreeCollection notDoneGroupTreeCollection = getNotDoneGroupTreeCollection();
        Assert.assertTrue(notDoneGroupTreeCollection != null);

        TreeViewAdapter treeViewAdapter = notDoneGroupTreeCollection.getTreeViewAdapter();
        Assert.assertTrue(treeViewAdapter != null);

        return treeViewAdapter;
    }

    public TreeNodeCollection getTreeNodeCollection() {
        NotDoneGroupTreeCollection notDoneGroupTreeCollection = getNotDoneGroupTreeCollection();
        Assert.assertTrue(notDoneGroupTreeCollection != null);

        TreeNodeCollection treeNodeCollection = notDoneGroupTreeCollection.getTreeNodeCollection();
        Assert.assertTrue(treeNodeCollection != null);

        return treeNodeCollection;
    }
}
