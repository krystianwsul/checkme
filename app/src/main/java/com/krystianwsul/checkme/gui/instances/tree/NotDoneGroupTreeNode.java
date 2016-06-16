package com.krystianwsul.checkme.gui.instances.tree;

import android.support.annotation.NonNull;
import android.view.View;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.krystianwsul.checkme.gui.SelectionCallback;
import com.krystianwsul.checkme.gui.instances.GroupListFragment;

import junit.framework.Assert;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NotDoneGroupTreeNode implements Node, NodeContainer, Comparable<NotDoneGroupTreeNode> {
    private final NotDoneGroupModelNode mNotDoneGroupModelNode;

    private final WeakReference<NotDoneGroupTreeCollection> mNotDoneGroupTreeCollectionReference;

    private List<ChildTreeNode> mChildTreeNodes;

    private boolean mNotDoneGroupNodeExpanded;

    private boolean mSelected = false;

    public NotDoneGroupTreeNode(NotDoneGroupModelNode notDoneGroupModelNode, boolean expanded, WeakReference<NotDoneGroupTreeCollection> notDoneGroupTreeCollectionReference, boolean selected) {
        Assert.assertTrue(notDoneGroupModelNode != null);
        Assert.assertTrue(notDoneGroupTreeCollectionReference != null);

        mNotDoneGroupModelNode = notDoneGroupModelNode;
        mNotDoneGroupNodeExpanded = expanded;
        mNotDoneGroupTreeCollectionReference = notDoneGroupTreeCollectionReference;
        mSelected = selected;
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
            return 1 + mChildTreeNodes.size();
        } else {
            return 1;
        }
    }

    @Override
    public Node getNode(int position) {
        Assert.assertTrue(position >= 0);
        Assert.assertTrue(position < displayedSize());

        if (position == 0)
            return this;

        Assert.assertTrue(mNotDoneGroupNodeExpanded);

        Node node = mChildTreeNodes.get(position - 1);
        Assert.assertTrue(node != null);

        return node;
    }

    @Override
    public int getPosition(Node node) {
        if (node == this)
            return 0;

        if (!(node instanceof ChildTreeNode))
            return -1;

        ChildTreeNode childTreeNode = (ChildTreeNode) node;
        if (mChildTreeNodes.contains(childTreeNode)) {
            Assert.assertTrue(mNotDoneGroupNodeExpanded);
            return mChildTreeNodes.indexOf(childTreeNode) + 1;
        }

        return -1;
    }

    @Override
    public boolean expanded() {
        return mNotDoneGroupNodeExpanded;
    }

    public void remove(ChildTreeNode childTreeNode) {
        Assert.assertTrue(childTreeNode != null);

        TreeNodeCollection treeNodeCollection = getTreeNodeCollection();
        Assert.assertTrue(treeNodeCollection != null);

        TreeViewAdapter treeViewAdapter = treeNodeCollection.getTreeViewAdapter();
        Assert.assertTrue(treeViewAdapter != null);

        Assert.assertTrue(mChildTreeNodes.size() > 0);

        final boolean lastInGroup = (mChildTreeNodes.indexOf(childTreeNode) == mChildTreeNodes.size() - 1);

        int groupPosition = treeNodeCollection.getPosition(this);

        int oldInstancePosition = treeNodeCollection.getPosition(childTreeNode);

        if (mChildTreeNodes.size() == 1) {
            mChildTreeNodes.remove(childTreeNode);

            mNotDoneGroupNodeExpanded = false;

            if ((groupPosition > 0) && (treeNodeCollection.getNode(groupPosition - 1) instanceof NotDoneGroupTreeNode))
                treeViewAdapter.notifyItemRangeChanged(groupPosition - 1, 2);
            else
                treeViewAdapter.notifyItemChanged(groupPosition);

            treeViewAdapter.notifyItemRangeRemoved(groupPosition + 1, 2);
        } else {
            Assert.assertTrue(mChildTreeNodes.size() > 1);

            mChildTreeNodes.remove(childTreeNode);

            treeViewAdapter.notifyItemChanged(groupPosition);
            treeViewAdapter.notifyItemRemoved(oldInstancePosition);

            if (lastInGroup)
                treeViewAdapter.notifyItemChanged(oldInstancePosition - 1);
        }
    }

    public Stream<Node> getSelectedNodes() {
        if (mChildTreeNodes.isEmpty()) {
            ArrayList<Node> selectedNodes = new ArrayList<>();
            if (mSelected)
                selectedNodes.add(this);
            return Stream.of(selectedNodes);
        } else {
            Assert.assertTrue(!mSelected);

            return Stream.of(Stream.of(mChildTreeNodes)
                    .filter(ChildTreeNode::isSelected)
                    .collect(Collectors.toList()));
        }
    }

    public void unselect() {
        TreeNodeCollection treeNodeCollection = getTreeNodeCollection();
        Assert.assertTrue(treeNodeCollection != null);

        TreeViewAdapter treeViewAdapter = treeNodeCollection.getTreeViewAdapter();
        Assert.assertTrue(treeViewAdapter != null);

        if (mChildTreeNodes.isEmpty()) {
            if (mSelected) {
                mSelected = false;
                treeViewAdapter.notifyItemChanged(treeNodeCollection.getPosition(this));
            }
        } else {
            Assert.assertTrue(!mSelected);

            List<ChildTreeNode> selected = getSelectedNodes()
                    .map(node -> (ChildTreeNode) node)
                    .collect(Collectors.toList());

            if (!selected.isEmpty()) {
                Assert.assertTrue(mNotDoneGroupNodeExpanded);

                Stream.of(selected)
                        .forEach(ChildTreeNode::unselect);

                treeViewAdapter.notifyItemChanged(treeNodeCollection.getPosition(this));
            }
        }
    }

    public void updateCheckBoxes() {
        TreeNodeCollection treeNodeCollection = getTreeNodeCollection();
        Assert.assertTrue(treeNodeCollection != null);

        TreeViewAdapter treeViewAdapter = treeNodeCollection.getTreeViewAdapter();
        Assert.assertTrue(treeViewAdapter != null);

        if (mChildTreeNodes.isEmpty()) {
            treeViewAdapter.notifyItemChanged(treeNodeCollection.getPosition(this));
        } else {
            treeViewAdapter.notifyItemRangeChanged(treeNodeCollection.getPosition(this) + 1, displayedSize() - 1);
        }
    }

    public void sort() {
        Collections.sort(mChildTreeNodes);
    }

    public void setNotDoneInstanceTreeNodes(List<ChildTreeNode> childTreeNodes) {
        Assert.assertTrue(childTreeNodes != null);

        mChildTreeNodes = childTreeNodes;

        sort();
    }

    public void addNotDoneInstanceNode(ChildTreeNode childTreeNode) {
        Assert.assertTrue(childTreeNode != null);

        TreeNodeCollection treeNodeCollection = getTreeNodeCollection();
        Assert.assertTrue(treeNodeCollection != null);

        TreeViewAdapter treeViewAdapter = treeNodeCollection.getTreeViewAdapter();
        Assert.assertTrue(treeViewAdapter != null);

        mChildTreeNodes.add(childTreeNode);

        sort();

        if (expanded()) {
            int newGroupPosition = treeNodeCollection.getPosition(this);
            int newInstancePosition = treeNodeCollection.getPosition(childTreeNode);

            boolean last = (newGroupPosition + displayedSize() - 1 == newInstancePosition);

            treeViewAdapter.notifyItemChanged(newGroupPosition);
            treeViewAdapter.notifyItemInserted(newInstancePosition);

            if (last)
                treeViewAdapter.notifyItemChanged(newInstancePosition - 1);
        } else {
            int newGroupPosition = treeNodeCollection.getPosition(this);
            treeViewAdapter.notifyItemChanged(newGroupPosition);
        }
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

    @Override
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
        if (!mChildTreeNodes.isEmpty())
            return;

        TreeNodeCollection treeNodeCollection = getTreeNodeCollection();
        Assert.assertTrue(treeNodeCollection != null);

        TreeViewAdapter treeViewAdapter = treeNodeCollection.getTreeViewAdapter();
        Assert.assertTrue(treeViewAdapter != null);

        SelectionCallback selectionCallback = treeViewAdapter.getSelectionCallback();
        Assert.assertTrue(selectionCallback != null);

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

    public NotDoneGroupModelNode getNotDoneGroupModelNode() {
        return mNotDoneGroupModelNode;
    }

    @Override
    public int compareTo(@NonNull NotDoneGroupTreeNode another) {
        return mNotDoneGroupModelNode.compareTo(another.mNotDoneGroupModelNode);
    }

    @Override
    public List<Node> getSelectedChildren() {
        Assert.assertTrue(!mChildTreeNodes.isEmpty());
        Assert.assertTrue(!mSelected);

        return Stream.of(mChildTreeNodes)
                .filter(ChildTreeNode::isSelected)
                .collect(Collectors.toList());
    }
}
