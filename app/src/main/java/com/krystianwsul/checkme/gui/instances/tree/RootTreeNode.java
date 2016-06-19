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

public class RootTreeNode extends Node implements NodeContainer, Comparable<RootTreeNode> {
    private final WeakReference<NodeContainer> mParentReference;
    private final RootModelNode mRootModelNode;

    private List<ChildTreeNode> mChildTreeNodes;

    private boolean mExpanded;

    private boolean mSelected = false;

    public RootTreeNode(RootModelNode rootModelNode, WeakReference<NodeContainer> parentReference, boolean expanded, boolean selected) {
        Assert.assertTrue(rootModelNode != null);
        Assert.assertTrue(parentReference != null);

        mRootModelNode = rootModelNode;
        mParentReference = parentReference;
        mExpanded = expanded;
        mSelected = selected;

        Assert.assertTrue(!mSelected || mRootModelNode.selectable());
    }

    @Override
    public void onBindViewHolder(GroupListFragment.GroupAdapter.AbstractHolder abstractHolder) {
        mRootModelNode.onBindViewHolder(abstractHolder);
    }

    @Override
    public int getItemViewType() {
        return mRootModelNode.getItemViewType();
    }

    public void setChildTreeNodes(List<ChildTreeNode> childTreeNodes) {
        Assert.assertTrue(childTreeNodes != null);

        mChildTreeNodes = childTreeNodes;

        Collections.sort(mChildTreeNodes);
    }

    @Override
    public int displayedSize() {
        SelectionCallback selectionCallback = getSelectionCallback();
        Assert.assertTrue(selectionCallback != null);

        if ((!mRootModelNode.visibleWhenEmpty() && mChildTreeNodes.isEmpty()) || (!mRootModelNode.visibleDuringActionMode() && selectionCallback.hasActionMode())) {
            return 0;
        } else {
            if (mExpanded) {
                return 1 + mChildTreeNodes.size();
            } else {
                return 1;
            }
        }
    }

    @Override
    public boolean expanded() {
        return mExpanded;
    }

    @Override
    public Node getNode(int position) {
        Assert.assertTrue(position >= 0);
        Assert.assertTrue(!mChildTreeNodes.isEmpty() || mRootModelNode.visibleWhenEmpty());
        Assert.assertTrue(!getSelectionCallback().hasActionMode() || mRootModelNode.visibleDuringActionMode());
        Assert.assertTrue(position < displayedSize());

        if (position == 0)
            return this;

        Assert.assertTrue(mExpanded);

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
            Assert.assertTrue(mExpanded);
            return mChildTreeNodes.indexOf(childTreeNode) + 1;
        }

        return -1;
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
                mRootModelNode.onClick();
            }
        };
    }

    private void onLongClick() {
        if (!mRootModelNode.selectable())
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

    public void remove(ChildTreeNode childTreeNode) {
        Assert.assertTrue(childTreeNode != null);

        TreeNodeCollection treeNodeCollection = getTreeNodeCollection();
        Assert.assertTrue(treeNodeCollection != null);

        TreeViewAdapter treeViewAdapter = treeNodeCollection.getTreeViewAdapter();
        Assert.assertTrue(treeViewAdapter != null);

        Assert.assertTrue(mChildTreeNodes.contains(childTreeNode));

        Assert.assertTrue(!mChildTreeNodes.isEmpty());

        boolean lastInParent = (mChildTreeNodes.indexOf(childTreeNode) == mChildTreeNodes.size() - 1);

        int oldParentPosition = treeNodeCollection.getPosition(this);
        int oldChildPosition = treeNodeCollection.getPosition(childTreeNode);

        mChildTreeNodes.remove(childTreeNode);

        if (mChildTreeNodes.isEmpty()) {
            mExpanded = false;

            if (oldParentPosition == 0) {
                if (mRootModelNode.visibleWhenEmpty()) {
                    treeViewAdapter.notifyItemChanged(oldParentPosition);
                    treeViewAdapter.notifyItemRemoved(oldParentPosition + 1);
                } else {
                    treeViewAdapter.notifyItemRangeRemoved(oldParentPosition, 2);
                }
            } else {
                if (mRootModelNode.visibleWhenEmpty()) {
                    treeViewAdapter.notifyItemRangeChanged(oldParentPosition - 1, 2);
                    treeViewAdapter.notifyItemRemoved(oldParentPosition + 1);
                } else {
                    treeViewAdapter.notifyItemChanged(oldParentPosition - 1);
                    treeViewAdapter.notifyItemRangeRemoved(oldParentPosition, 2);
                }
            }
        } else {
            treeViewAdapter.notifyItemChanged(oldParentPosition);
            treeViewAdapter.notifyItemRemoved(oldChildPosition);

            if (lastInParent)
                treeViewAdapter.notifyItemChanged(oldChildPosition - 1);

            if (oldParentPosition > 0)
                treeViewAdapter.notifyItemChanged(oldParentPosition - 1);
        }
    }

    public Stream<Node> getSelectedNodes() {
        Assert.assertTrue(!mSelected || mRootModelNode.selectable());

        ArrayList<Node> selectedNodes = new ArrayList<>();

        if (mSelected)
            selectedNodes.add(this);

        selectedNodes.addAll(Stream.of(mChildTreeNodes)
                .filter(ChildTreeNode::isSelected)
                .collect(Collectors.toList()));

        return Stream.of(selectedNodes);
    }

    public void add(ChildTreeNode childTreeNode) {
        Assert.assertTrue(childTreeNode != null);

        TreeNodeCollection treeNodeCollection = getTreeNodeCollection();
        Assert.assertTrue(treeNodeCollection != null);

        TreeViewAdapter treeViewAdapter = treeNodeCollection.getTreeViewAdapter();
        Assert.assertTrue(treeViewAdapter != null);

        if (mExpanded) {
            if (mRootModelNode.visibleWhenEmpty()) {
                int oldParentPosition = treeNodeCollection.getPosition(this);

                mChildTreeNodes.add(childTreeNode);

                Collections.sort(mChildTreeNodes);

                int newChildPosition = treeNodeCollection.getPosition(childTreeNode);

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

                    mChildTreeNodes.add(childTreeNode);

                    Collections.sort(mChildTreeNodes);

                    int newChildPosition = treeNodeCollection.getPosition(childTreeNode);

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

            if (!mRootModelNode.visibleWhenEmpty() && mChildTreeNodes.size() == 1) {
                treeViewAdapter.notifyItemInserted(newParentPosition);

                if (newParentPosition > 0)
                    treeViewAdapter.notifyItemChanged(newParentPosition - 1);
            } else {
                treeViewAdapter.notifyItemChanged(newParentPosition);
            }
        }
    }

    public boolean getSeparatorVisibility() {
        if (expanded())
            return false;

        TreeNodeCollection treeNodeCollection = getTreeNodeCollection();
        Assert.assertTrue(treeNodeCollection != null);

        int position = treeNodeCollection.getPosition(this);

        boolean last = (treeNodeCollection.displayedSize() == position + 1);

        if (!last) {
            Node nextNode = treeNodeCollection.getNode(position + 1);
            return (nextNode.expanded());
        } else {
            return false;
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

    public boolean isSelected() {
        return mSelected;
    }

    public RootModelNode getRootModelNode() {
        return mRootModelNode;
    }

    @Override
    public List<Node> getSelectedChildren() {
        Assert.assertTrue(!mChildTreeNodes.isEmpty());
        Assert.assertTrue(!mSelected);

        return Stream.of(mChildTreeNodes)
                .filter(ChildTreeNode::isSelected)
                .collect(Collectors.toList());
    }

    @Override
    public int compareTo(@NonNull RootTreeNode another) {
        return mRootModelNode.compareTo(another.mRootModelNode);
    }

    public void unselect() {
        Assert.assertTrue(!mSelected || mRootModelNode.selectable());

        TreeNodeCollection treeNodeCollection = getTreeNodeCollection();
        Assert.assertTrue(treeNodeCollection != null);

        TreeViewAdapter treeViewAdapter = treeNodeCollection.getTreeViewAdapter();
        Assert.assertTrue(treeViewAdapter != null);

        if (mSelected) {
            Assert.assertTrue(mRootModelNode.selectable());

            mSelected = false;
            treeViewAdapter.notifyItemChanged(treeNodeCollection.getPosition(this));
        }

        List<ChildTreeNode> selected = getSelectedNodes()
                .map(node -> (ChildTreeNode) node)
                .collect(Collectors.toList());

        if (!selected.isEmpty()) {
            Assert.assertTrue(mExpanded);

            Stream.of(selected)
                    .forEach(ChildTreeNode::unselect);

            treeViewAdapter.notifyItemChanged(treeNodeCollection.getPosition(this));
        }
    }

    public void onCreateActionMode() {
        TreeNodeCollection treeNodeCollection = getTreeNodeCollection();
        Assert.assertTrue(treeNodeCollection != null);

        TreeViewAdapter treeViewAdapter = treeNodeCollection.getTreeViewAdapter();
        Assert.assertTrue(treeViewAdapter != null);

        int oldPosition = treeNodeCollection.getPosition(this);
        Assert.assertTrue(oldPosition >= 0);

        if (mRootModelNode.visibleDuringActionMode()) {
            treeViewAdapter.notifyItemRangeChanged(oldPosition, displayedSize());
        } else {
            if (mChildTreeNodes.size() > 0) {
                if (mExpanded)
                    treeViewAdapter.notifyItemRangeRemoved(oldPosition, mChildTreeNodes.size() + 1);
                else
                    treeViewAdapter.notifyItemRemoved(oldPosition);
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

        if (mRootModelNode.visibleDuringActionMode()) {
            treeViewAdapter.notifyItemRangeChanged(position, displayedSize());
        } else {
            if (mChildTreeNodes.size() > 0) {
                if (mExpanded)
                    treeViewAdapter.notifyItemRangeInserted(position, mChildTreeNodes.size() + 1);
                else
                    treeViewAdapter.notifyItemInserted(position);
            }

            if (position > 0)
                treeViewAdapter.notifyItemChanged(position - 1);
        }
    }

    public void select() {
        Assert.assertTrue(mRootModelNode.selectable());
        Assert.assertTrue(!mSelected);

        mSelected = true;
    }

    private NodeContainer getParent() {
        NodeContainer parent = mParentReference.get();
        Assert.assertTrue(parent != null);

        return parent;
    }

    @Override
    public TreeNodeCollection getTreeNodeCollection() {
        NodeContainer parent = getParent();
        Assert.assertTrue(parent != null);

        TreeNodeCollection treeNodeCollection = parent.getTreeNodeCollection();
        Assert.assertTrue(treeNodeCollection != null);

        return treeNodeCollection;
    }

    private TreeViewAdapter getTreeViewAdapter() {
        TreeNodeCollection treeNodeCollection = getTreeNodeCollection();
        Assert.assertTrue(treeNodeCollection != null);

        TreeViewAdapter treeViewAdapter = treeNodeCollection.getTreeViewAdapter();
        Assert.assertTrue(treeViewAdapter != null);

        return treeViewAdapter;
    }

    private SelectionCallback getSelectionCallback() {
        TreeViewAdapter treeViewAdapter = getTreeViewAdapter();
        Assert.assertTrue(treeViewAdapter != null);

        return treeViewAdapter.getSelectionCallback();
    }
}
