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

public class TreeNode implements Comparable<TreeNode>, NodeContainer {
    private final ModelNode mModelNode;

    private List<TreeNode> mChildTreeNodes;

    private final WeakReference<NodeContainer> mParentReference;

    private boolean mExpanded;

    private boolean mSelected = false;

    public TreeNode(ModelNode modelNode, WeakReference<NodeContainer> parentReference, boolean expanded, boolean selected) {
        Assert.assertTrue(modelNode != null);
        Assert.assertTrue(parentReference != null);

        mModelNode = modelNode;
        mParentReference = parentReference;

        mExpanded = expanded;
        mSelected = selected;

        Assert.assertTrue(!mSelected || mModelNode.selectable());
    }

    public void onBindViewHolder(GroupListFragment.GroupAdapter.AbstractHolder abstractHolder) {
        mModelNode.onBindViewHolder(abstractHolder);
    }

    public int getItemViewType() {
        return mModelNode.getItemViewType();
    }

    public void update() {
        TreeNodeCollection treeNodeCollection = getTreeNodeCollection();
        Assert.assertTrue(treeNodeCollection != null);

        TreeViewAdapter treeViewAdapter = treeNodeCollection.getTreeViewAdapter();
        Assert.assertTrue(treeViewAdapter != null);

        treeViewAdapter.notifyItemChanged(treeNodeCollection.getPosition(this));
    }

    public ModelNode getModelNode() {
        return mModelNode;
    }

    public void setChildTreeNodes(List<TreeNode> childTreeNodes) {
        Assert.assertTrue(childTreeNodes != null);

        mChildTreeNodes = childTreeNodes;

        Collections.sort(mChildTreeNodes);
    }

    public boolean expanded() {
        Assert.assertTrue(!mExpanded || !mChildTreeNodes.isEmpty());
        return mExpanded;
    }

    @Override
    public int compareTo(@NonNull TreeNode another) {
        return mModelNode.compareTo(another.mModelNode);
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
                mModelNode.onClick();
            }
        };
    }

    private void onLongClick() {
        if (!mModelNode.selectable())
            return;

        TreeNodeCollection treeNodeCollection = getTreeNodeCollection();
        Assert.assertTrue(treeNodeCollection != null);

        TreeViewAdapter treeViewAdapter = treeNodeCollection.getTreeViewAdapter();
        Assert.assertTrue(treeViewAdapter != null);

        SelectionCallback selectionCallback = treeViewAdapter.getSelectionCallback();
        Assert.assertTrue(selectionCallback != null);

        NodeContainer parent = getParent();
        Assert.assertTrue(parent != null);

        mSelected = !mSelected;

        if (mSelected) {
            selectionCallback.incrementSelected();

            if (parent.getSelectedChildren().size() == 1) // first in group
                parent.update();
        } else {
            selectionCallback.decrementSelected();

            if (parent.getSelectedChildren().size() == 0) // last in group
                parent.update();
        }

        treeViewAdapter.notifyItemChanged(treeNodeCollection.getPosition(this));
    }

    private NodeContainer getParent() {
        NodeContainer parent = mParentReference.get();
        Assert.assertTrue(parent != null);

        return parent;
    }

    private SelectionCallback getSelectionCallback() {
        TreeViewAdapter treeViewAdapter = getTreeViewAdapter();
        Assert.assertTrue(treeViewAdapter != null);

        return treeViewAdapter.getSelectionCallback();
    }

    private TreeViewAdapter getTreeViewAdapter() {
        TreeNodeCollection treeNodeCollection = getTreeNodeCollection();
        Assert.assertTrue(treeNodeCollection != null);

        TreeViewAdapter treeViewAdapter = treeNodeCollection.getTreeViewAdapter();
        Assert.assertTrue(treeViewAdapter != null);

        return treeViewAdapter;
    }

    public int displayedSize() {
        SelectionCallback selectionCallback = getSelectionCallback();
        Assert.assertTrue(selectionCallback != null);

        if ((!mModelNode.visibleWhenEmpty() && mChildTreeNodes.isEmpty()) || (!mModelNode.visibleDuringActionMode() && selectionCallback.hasActionMode())) {
            return 0;
        } else {
            if (mExpanded) {
                return 1 + childrenDisplayedSize();
            } else {
                return 1;
            }
        }
    }

    private int childrenDisplayedSize() {
        return Stream.of(mChildTreeNodes)
                .map(TreeNode::displayedSize)
                .reduce(0, (lhs, rhs) -> lhs + rhs);
    }

    public TreeNode getNode(int position) {
        Assert.assertTrue(position >= 0);
        Assert.assertTrue(!mChildTreeNodes.isEmpty() || mModelNode.visibleWhenEmpty());
        Assert.assertTrue(!getSelectionCallback().hasActionMode() || mModelNode.visibleDuringActionMode());
        Assert.assertTrue(position < displayedSize());

        if (position == 0)
            return this;

        Assert.assertTrue(mExpanded);

        position--;

        for (TreeNode notDoneGroupTreeNode : mChildTreeNodes) {
            if (position < notDoneGroupTreeNode.displayedSize())
                return notDoneGroupTreeNode.getNode(position);

            position -= notDoneGroupTreeNode.displayedSize();
        }

        throw new IndexOutOfBoundsException();
    }

    public int getPosition(TreeNode treeNode) {
        if (treeNode == this)
            return 0;

        int offset = 1;
        for (TreeNode childTreeNode : mChildTreeNodes) {
            int position = childTreeNode.getPosition(treeNode);
            if (position >= 0)
                return offset + position;
            offset += childTreeNode.displayedSize();
        }

        return -1;
    }

    public boolean isSelected() {
        Assert.assertTrue(!mSelected || mModelNode.selectable());

        return mSelected;
    }

    public void unselect() {
        Assert.assertTrue(!mSelected || mModelNode.selectable());

        TreeNodeCollection treeNodeCollection = getTreeNodeCollection();
        Assert.assertTrue(treeNodeCollection != null);

        TreeViewAdapter treeViewAdapter = treeNodeCollection.getTreeViewAdapter();
        Assert.assertTrue(treeViewAdapter != null);

        if (mSelected) {
            Assert.assertTrue(mModelNode.selectable());

            mSelected = false;
            treeViewAdapter.notifyItemChanged(treeNodeCollection.getPosition(this));
        }

        List<TreeNode> selected = getSelectedNodes()
                .collect(Collectors.toList());

        if (!selected.isEmpty()) {
            Assert.assertTrue(mExpanded);

            Stream.of(selected)
                    .forEach(TreeNode::unselect);

            treeViewAdapter.notifyItemChanged(treeNodeCollection.getPosition(this));
        }
    }

    public Stream<TreeNode> getSelectedNodes() {
        Assert.assertTrue(!mSelected || mModelNode.selectable());

        ArrayList<TreeNode> selectedTreeNodes = new ArrayList<>();

        if (mSelected)
            selectedTreeNodes.add(this);

        selectedTreeNodes.addAll(Stream.of(mChildTreeNodes)
                .flatMap(TreeNode::getSelectedNodes)
                .collect(Collectors.toList()));

        return Stream.of(selectedTreeNodes);
    }

    public View.OnClickListener getExpandListener() {
        return v -> {
            Assert.assertTrue(!mChildTreeNodes.isEmpty());

            TreeNodeCollection treeNodeCollection = getTreeNodeCollection();
            Assert.assertTrue(treeNodeCollection != null);

            TreeViewAdapter treeViewAdapter = treeNodeCollection.getTreeViewAdapter();
            Assert.assertTrue(treeViewAdapter != null);

            SelectionCallback selectionCallback = treeViewAdapter.getSelectionCallback();
            Assert.assertTrue(selectionCallback != null);

            Assert.assertTrue(!(selectionCallback.hasActionMode() && getSelectedNodes().count() > 0));

            int position = treeNodeCollection.getPosition(this);
            Assert.assertTrue(position >= 0);

            if (mExpanded) { // hiding
                Assert.assertTrue(getSelectedNodes().count() == 0);

                int displayedSize = displayedSize();
                mExpanded = false;
                treeViewAdapter.notifyItemRangeRemoved(position + 1, displayedSize - 1);
            } else { // showing
                mExpanded = true;
                treeViewAdapter.notifyItemRangeInserted(position + 1, displayedSize() - 1);
            }

            if (position > 0) {
                treeViewAdapter.notifyItemRangeChanged(position - 1, 2);
            } else {
                treeViewAdapter.notifyItemChanged(position);
            }
        };
    }

    @Override
    public void remove(TreeNode childTreeNode) {
        Assert.assertTrue(childTreeNode != null);

        TreeNodeCollection treeNodeCollection = getTreeNodeCollection();
        Assert.assertTrue(treeNodeCollection != null);

        TreeViewAdapter treeViewAdapter = treeNodeCollection.getTreeViewAdapter();
        Assert.assertTrue(treeViewAdapter != null);

        Assert.assertTrue(mChildTreeNodes.contains(childTreeNode));

        Assert.assertTrue(!mChildTreeNodes.isEmpty());

        int childDisplayedSize = childTreeNode.displayedSize();

        boolean lastInParent = (mChildTreeNodes.indexOf(childTreeNode) == mChildTreeNodes.size() - 1);

        int oldParentPosition = treeNodeCollection.getPosition(this);
        Assert.assertTrue(oldParentPosition >= 0);

        int oldChildPosition = treeNodeCollection.getPosition(childTreeNode);
        Assert.assertTrue(oldChildPosition >= 0);

        mChildTreeNodes.remove(childTreeNode);

        if (Stream.of(mChildTreeNodes).map(TreeNode::displayedSize).reduce(0, (lhs, rhs) -> lhs + rhs) == 0) {
            mExpanded = false;

            if (oldParentPosition == 0) {
                if (mModelNode.visibleWhenEmpty()) {
                    treeViewAdapter.notifyItemChanged(oldParentPosition);
                    treeViewAdapter.notifyItemRangeRemoved(oldParentPosition + 1, childDisplayedSize);
                } else {
                    treeViewAdapter.notifyItemRangeRemoved(oldParentPosition, 1 + childDisplayedSize);
                }
            } else {
                if (mModelNode.visibleWhenEmpty()) {
                    treeViewAdapter.notifyItemRangeChanged(oldParentPosition - 1, 2);
                    treeViewAdapter.notifyItemRangeRemoved(oldParentPosition + 1, childDisplayedSize);
                } else {
                    treeViewAdapter.notifyItemChanged(oldParentPosition - 1);
                    treeViewAdapter.notifyItemRangeRemoved(oldParentPosition, 1 + childDisplayedSize);
                }
            }
        } else {
            treeViewAdapter.notifyItemChanged(oldParentPosition);
            treeViewAdapter.notifyItemRangeRemoved(oldChildPosition, childDisplayedSize);

            treeViewAdapter.notifyItemChanged(oldChildPosition - 1);

            if (oldParentPosition > 0)
                treeViewAdapter.notifyItemChanged(oldParentPosition - 1);
        }
    }

    @Override
    public void add(TreeNode childTreeNode) {
        Assert.assertTrue(childTreeNode != null);

        TreeNodeCollection treeNodeCollection = getTreeNodeCollection();
        Assert.assertTrue(treeNodeCollection != null);

        TreeViewAdapter treeViewAdapter = treeNodeCollection.getTreeViewAdapter();
        Assert.assertTrue(treeViewAdapter != null);

        if (mExpanded) {
            if (mModelNode.visibleWhenEmpty()) {
                int oldParentPosition = treeNodeCollection.getPosition(this);
                Assert.assertTrue(oldParentPosition >= 0);

                mChildTreeNodes.add(childTreeNode);

                Collections.sort(mChildTreeNodes);

                int newChildPosition = treeNodeCollection.getPosition(childTreeNode);
                Assert.assertTrue(newChildPosition >= 0);

                treeViewAdapter.notifyItemChanged(oldParentPosition);
                treeViewAdapter.notifyItemInserted(newChildPosition);

                boolean last = (oldParentPosition + displayedSize() - 1 == newChildPosition);

                if (last)
                    treeViewAdapter.notifyItemChanged(newChildPosition - 1);

                if (oldParentPosition > 0)
                    treeViewAdapter.notifyItemChanged(oldParentPosition - 1);
            } else {
                if (mChildTreeNodes.isEmpty()) {
                    mChildTreeNodes.add(childTreeNode);

                    Collections.sort(mChildTreeNodes);

                    int newParentPosition = treeNodeCollection.getPosition(this);

                    treeViewAdapter.notifyItemInserted(newParentPosition + 1);

                    if (newParentPosition > 0)
                        treeViewAdapter.notifyItemChanged(newParentPosition - 1);
                } else {
                    int oldParentPosition = treeNodeCollection.getPosition(this);
                    Assert.assertTrue(oldParentPosition >= 0);

                    mChildTreeNodes.add(childTreeNode);

                    Collections.sort(mChildTreeNodes);

                    int newChildPosition = treeNodeCollection.getPosition(childTreeNode);
                    Assert.assertTrue(newChildPosition >= 0);

                    treeViewAdapter.notifyItemChanged(oldParentPosition);
                    treeViewAdapter.notifyItemInserted(newChildPosition);

                    boolean last = (oldParentPosition + displayedSize() - 1 == newChildPosition);

                    if (last)
                        treeViewAdapter.notifyItemChanged(newChildPosition - 1);

                    if (oldParentPosition > 0)
                        treeViewAdapter.notifyItemChanged(oldParentPosition - 1);
                }
            }
        } else {
            mChildTreeNodes.add(childTreeNode);

            Collections.sort(mChildTreeNodes);

            int newParentPosition = treeNodeCollection.getPosition(this);
            Assert.assertTrue(newParentPosition >= 0);

            if (!mModelNode.visibleWhenEmpty() && mChildTreeNodes.size() == 1) {
                treeViewAdapter.notifyItemInserted(newParentPosition);

                if (newParentPosition > 0)
                    treeViewAdapter.notifyItemChanged(newParentPosition - 1);
            } else {
                treeViewAdapter.notifyItemChanged(newParentPosition);
            }
        }
    }

    public boolean getSeparatorVisibility() {
        NodeContainer parent = getParent();
        Assert.assertTrue(parent != null);

        Assert.assertTrue(parent.expanded());

        TreeNodeCollection treeNodeCollection = getTreeNodeCollection();
        Assert.assertTrue(treeNodeCollection != null);

        int positionInCollection = treeNodeCollection.getPosition(this);
        Assert.assertTrue(positionInCollection >= 0);

        if (positionInCollection == treeNodeCollection.displayedSize() - 1)
            return false;

        if (parent.getPosition(this) == parent.displayedSize() - 1)
            return true;

        TreeNode nextTreeNode = treeNodeCollection.getNode(positionInCollection + 1);
        return (nextTreeNode.expanded());
    }

    @Override
    public List<TreeNode> getSelectedChildren() {
        Assert.assertTrue(!mChildTreeNodes.isEmpty());

        return Stream.of(mChildTreeNodes)
                .filter(TreeNode::isSelected)
                .collect(Collectors.toList());
    }

    public void select() {
        Assert.assertTrue(mModelNode.selectable());
        Assert.assertTrue(!mSelected);

        mSelected = true;
    }

    public void onCreateActionMode() {
        TreeNodeCollection treeNodeCollection = getTreeNodeCollection();
        Assert.assertTrue(treeNodeCollection != null);

        TreeViewAdapter treeViewAdapter = treeNodeCollection.getTreeViewAdapter();
        Assert.assertTrue(treeViewAdapter != null);

        int oldPosition = treeNodeCollection.getPosition(this);
        Assert.assertTrue(oldPosition >= 0);

        if (mModelNode.visibleDuringActionMode()) {
            treeViewAdapter.notifyItemChanged(oldPosition);

            Stream.of(mChildTreeNodes)
                    .forEach(TreeNode::onCreateActionMode);
        } else {
            if (mChildTreeNodes.size() > 0) {
                if (mExpanded) {
                    treeViewAdapter.notifyItemRangeRemoved(oldPosition, 1 + childrenDisplayedSize());
                } else {
                    treeViewAdapter.notifyItemRemoved(oldPosition);
                }
            }

            if (oldPosition > 0)
                treeViewAdapter.notifyItemChanged(oldPosition - 1);
        }
    }

    public void onDestroyActionMode() {
        TreeNodeCollection treeNodeCollection = getTreeNodeCollection();
        Assert.assertTrue(treeNodeCollection != null);

        TreeViewAdapter treeViewAdapter = treeNodeCollection.getTreeViewAdapter();
        Assert.assertTrue(treeViewAdapter != null);

        int position = treeNodeCollection.getPosition(this);
        Assert.assertTrue(position >= 0);

        if (mModelNode.visibleDuringActionMode()) {
            treeViewAdapter.notifyItemChanged(position);

            Stream.of(mChildTreeNodes)
                    .forEach(TreeNode::onDestroyActionMode);
        } else {
            if (mChildTreeNodes.size() > 0) {
                if (mExpanded)
                    treeViewAdapter.notifyItemRangeInserted(position, displayedSize());
                else
                    treeViewAdapter.notifyItemInserted(position);
            }

            if (position > 0)
                treeViewAdapter.notifyItemChanged(position - 1);
        }
    }

    @Override
    public TreeNodeCollection getTreeNodeCollection() {
        NodeContainer parent = getParent();
        Assert.assertTrue(parent != null);

        TreeNodeCollection treeNodeCollection = parent.getTreeNodeCollection();
        Assert.assertTrue(treeNodeCollection != null);

        return treeNodeCollection;
    }

    public List<TreeNode> getAllChildren() {
        return mChildTreeNodes;
    }
}
