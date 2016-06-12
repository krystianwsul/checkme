package com.krystianwsul.checkme.gui.instances.tree;

import android.view.View;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.krystianwsul.checkme.gui.SelectionCallback;
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

    private final ArrayList<NotDoneInstanceTreeNode> mNotDoneInstanceTreeNodes = new ArrayList<>();

    private boolean mNotDoneGroupNodeExpanded;

    public ExactTimeStamp mExactTimeStamp;

    private boolean mSelected = false;

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

    public void remove(NotDoneInstanceTreeNode notDoneInstanceTreeNode) {
        Assert.assertTrue(notDoneInstanceTreeNode != null);

        TreeNodeCollection treeNodeCollection = getTreeNodeCollection();
        Assert.assertTrue(treeNodeCollection != null);

        TreeViewAdapter treeViewAdapter = treeNodeCollection.getTreeViewAdapter();
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

    public Stream<GroupListFragment.Node> getSelectedNodes() {
        if (mNotDoneInstanceTreeNodes.size() == 1) {
            ArrayList<GroupListFragment.Node> selectedNodes = new ArrayList<>();
            if (mSelected)
                selectedNodes.add(this);
            return Stream.of(selectedNodes);
        } else {
            Assert.assertTrue(!mSelected);

            return Stream.of(Stream.of(mNotDoneInstanceTreeNodes)
                    .filter(NotDoneInstanceTreeNode::isSelected)
                    .collect(Collectors.toList()));
        }
    }

    public void unselect() {
        TreeNodeCollection treeNodeCollection = getTreeNodeCollection();
        Assert.assertTrue(treeNodeCollection != null);

        TreeViewAdapter treeViewAdapter = treeNodeCollection.getTreeViewAdapter();
        Assert.assertTrue(treeViewAdapter != null);

        if (mNotDoneInstanceTreeNodes.size() == 1) {
            NotDoneInstanceTreeNode notDoneInstanceTreeNode = mNotDoneInstanceTreeNodes.get(0);
            Assert.assertTrue(notDoneInstanceTreeNode != null);

            if (mSelected) {
                mSelected = false;
                treeViewAdapter.notifyItemChanged(treeNodeCollection.getPosition(this));
            }
        } else {
            Assert.assertTrue(!mSelected);

            List<NotDoneInstanceTreeNode> selected = getSelectedNodes()
                    .map(node -> (NotDoneInstanceTreeNode) node)
                    .collect(Collectors.toList());

            if (!selected.isEmpty()) {
                Assert.assertTrue(mNotDoneGroupNodeExpanded);

                Stream.of(selected)
                        .forEach(NotDoneInstanceTreeNode::unselect);

                treeViewAdapter.notifyItemChanged(treeNodeCollection.getPosition(this));
            }
        }
    }

    public void updateCheckBoxes() {
        TreeNodeCollection treeNodeCollection = getTreeNodeCollection();
        Assert.assertTrue(treeNodeCollection != null);

        TreeViewAdapter treeViewAdapter = treeNodeCollection.getTreeViewAdapter();
        Assert.assertTrue(treeViewAdapter != null);

        if (mNotDoneInstanceTreeNodes.size() == 1) {
            treeViewAdapter.notifyItemChanged(treeNodeCollection.getPosition(this));
        } else {
            treeViewAdapter.notifyItemRangeChanged(treeNodeCollection.getPosition(this) + 1, displayedSize() - 1);
        }
    }

    public void sort() {
        Collections.sort(mNotDoneInstanceTreeNodes);
    }

    public void setInstanceDatas(List<GroupListLoader.InstanceData> instanceDatas, ArrayList<InstanceKey> selectedNodes) {
        Assert.assertTrue(instanceDatas != null);
        Assert.assertTrue(!instanceDatas.isEmpty());
        Assert.assertTrue(instanceDatas.size() > 1 || !mNotDoneGroupNodeExpanded);

        if (instanceDatas.size() == 1) {
            GroupListLoader.InstanceData instanceData = instanceDatas.get(0);

            mExactTimeStamp = instanceData.InstanceTimeStamp.toExactTimeStamp();
            if (selectedNodes != null && selectedNodes.contains(instanceData.InstanceKey))
                mSelected = true;

            addInstanceData(instanceData, null);
        } else {
            mExactTimeStamp = instanceDatas.get(0).InstanceTimeStamp.toExactTimeStamp();

            for (GroupListLoader.InstanceData instanceData : instanceDatas) {
                Assert.assertTrue(mExactTimeStamp.equals(instanceData.InstanceTimeStamp.toExactTimeStamp()));
                addInstanceData(instanceData, selectedNodes);
            }

            sort();
        }
    }

    public NotDoneInstanceTreeNode addInstanceData(GroupListLoader.InstanceData instanceData, ArrayList<InstanceKey> selectedNodes) {
        Assert.assertTrue(instanceData != null);

        NotDoneInstanceTreeNode notDoneInstanceTreeNode = mNotDoneGroupModelNode.newNotDoneInstanceTreeNode(instanceData, selectedNodes);
        Assert.assertTrue(notDoneInstanceTreeNode != null);

        mNotDoneInstanceTreeNodes.add(notDoneInstanceTreeNode);
        return notDoneInstanceTreeNode;
    }

    private NotDoneGroupTreeCollection getNotDoneGroupTreeCollection() {
        NotDoneGroupTreeCollection notDoneGroupTreeCollection = mNotDoneGroupTreeCollectionReference.get();
        Assert.assertTrue(notDoneGroupTreeCollection != null);

        return notDoneGroupTreeCollection;
    }

    private TreeViewAdapter getTreeViewAdapter() {
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

    private SelectionCallback getSelectionCallback() {
        TreeViewAdapter treeViewAdapter = getTreeViewAdapter();
        Assert.assertTrue(treeViewAdapter != null);

        return treeViewAdapter.getSelectionCallback();
    }

    public View.OnLongClickListener getOnLongClickListener() {
        return v -> {
            onLongClick();
            return true;
        };
    }

    public View.OnClickListener getOnClickListener() {
        SelectionCallback selectionCallback = getSelectionCallback();
        Assert.assertTrue(selectionCallback != null);

        return v -> {
            if (selectionCallback.hasActionMode()) {
                onLongClick();
            } else {
                mNotDoneGroupModelNode.onClick();
            }
        };
    }

    private void onLongClick() {
        if (!(mNotDoneInstanceTreeNodes.size() == 1))
            return;

        TreeNodeCollection treeNodeCollection = getTreeNodeCollection();
        Assert.assertTrue(treeNodeCollection != null);

        TreeViewAdapter treeViewAdapter = treeNodeCollection.getTreeViewAdapter();
        Assert.assertTrue(treeViewAdapter != null);

        SelectionCallback selectionCallback = treeViewAdapter.getSelectionCallback();
        Assert.assertTrue(selectionCallback != null);

        NotDoneInstanceTreeNode notDoneInstanceTreeNode = mNotDoneInstanceTreeNodes.get(0);
        Assert.assertTrue(notDoneInstanceTreeNode != null);

        mSelected = !mSelected;

        if (mSelected) {
            selectionCallback.incrementSelected();
        } else {
            selectionCallback.decrementSelected();
        }
        treeViewAdapter.notifyItemChanged(treeNodeCollection.getPosition(this));
    }

    public boolean isSelected() {
        return mSelected;
    }

    public View.OnClickListener getExpandListener() {
        return v -> {
            TreeNodeCollection treeNodeCollection = getTreeNodeCollection();
            Assert.assertTrue(treeNodeCollection != null);

            TreeViewAdapter treeViewAdapter = treeNodeCollection.getTreeViewAdapter();
            Assert.assertTrue(treeViewAdapter != null);

            SelectionCallback selectionCallback = treeViewAdapter.getSelectionCallback();
            Assert.assertTrue(selectionCallback != null);

            Assert.assertTrue(!(selectionCallback.hasActionMode() && getSelectedNodes().count() > 0));

            int position = treeNodeCollection.getPosition(this);

            if (expanded()) { // hiding
                Assert.assertTrue(getSelectedNodes().count() == 0);

                int displayedSize = displayedSize();
                mNotDoneGroupNodeExpanded = false;
                treeViewAdapter.notifyItemRangeRemoved(position + 1, displayedSize - 1);
            } else { // showing
                mNotDoneGroupNodeExpanded = true;
                treeViewAdapter.notifyItemRangeInserted(position + 1, displayedSize() - 1);
            }

            if ((position) > 0 && (treeNodeCollection.getNode(position - 1) instanceof NotDoneGroupTreeNode)) {
                treeViewAdapter.notifyItemRangeChanged(position - 1, 2);
            } else {
                treeViewAdapter.notifyItemChanged(position);
            }
        };
    }

    public boolean getSeparatorVisibility() {
        if (expanded())
            return false;

        NotDoneGroupTreeCollection notDoneGroupTreeCollection = getNotDoneGroupTreeCollection();
        Assert.assertTrue(notDoneGroupTreeCollection != null);

        TreeNodeCollection treeNodeCollection = notDoneGroupTreeCollection.getTreeNodeCollection();
        Assert.assertTrue(treeNodeCollection != null);

        int position = treeNodeCollection.getPosition(this);
        boolean last = (position == notDoneGroupTreeCollection.displayedSize() - 1);
        if (!last) {
            NotDoneGroupTreeNode nextNode = (NotDoneGroupTreeNode) treeNodeCollection.getNode(position + 1);
            return (nextNode.expanded());
        } else {
            return (treeNodeCollection.mDividerTreeNode.visible() && treeNodeCollection.mDividerTreeNode.expanded());
        }
    }

    @Override
    public void update() {
        TreeNodeCollection treeNodeCollection = getTreeNodeCollection();
        Assert.assertTrue(treeNodeCollection != null);

        TreeViewAdapter treeViewAdapter = treeNodeCollection.getTreeViewAdapter();
        Assert.assertTrue(treeViewAdapter != null);

        treeViewAdapter.notifyItemChanged(treeNodeCollection.getPosition(this));
    }

    @Override
    public void removeFromParent() {
        NotDoneGroupTreeCollection notDoneGroupTreeCollection = getNotDoneGroupTreeCollection();
        Assert.assertTrue(notDoneGroupTreeCollection != null);

        NotDoneGroupModelCollection notDoneGroupModelCollection = notDoneGroupTreeCollection.getNotDoneGroupModelCollection();
        Assert.assertTrue(notDoneGroupModelCollection != null);

        notDoneGroupModelCollection.remove(this);
    }

    public NotDoneGroupModelNode getNotDoneGroupModelNode() {
        return mNotDoneGroupModelNode;
    }
}
