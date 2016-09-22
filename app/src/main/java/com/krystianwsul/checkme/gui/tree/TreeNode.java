package com.krystianwsul.checkme.gui.tree;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;

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

    public TreeNode(@NonNull ModelNode modelNode, @NonNull WeakReference<NodeContainer> parentReference, boolean expanded, boolean selected) {
        mModelNode = modelNode;
        mParentReference = parentReference;

        mExpanded = expanded;
        mSelected = selected;

        Assert.assertTrue(!mSelected || mModelNode.selectable());
    }

    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder) {
        mModelNode.onBindViewHolder(viewHolder);
    }

    int getItemViewType() {
        return mModelNode.getItemViewType();
    }

    public void update() {
        TreeNodeCollection treeNodeCollection = getTreeNodeCollection();

        TreeViewAdapter treeViewAdapter = treeNodeCollection.getTreeViewAdapter();

        treeViewAdapter.notifyItemChanged(treeNodeCollection.getPosition(this));
    }

    @NonNull
    public ModelNode getModelNode() {
        return mModelNode;
    }

    public void setChildTreeNodes(@NonNull List<TreeNode> childTreeNodes) {
        Assert.assertTrue(!mExpanded || !childTreeNodes.isEmpty());

        mChildTreeNodes = childTreeNodes;

        Collections.sort(mChildTreeNodes);
    }

    public boolean expanded() {
        Assert.assertTrue(!mExpanded || visibleSize() > 1);
        return mExpanded;
    }

    @Override
    public int compareTo(@NonNull TreeNode another) {
        return mModelNode.compareTo(another.mModelNode);
    }

    @NonNull
    public View.OnLongClickListener getOnLongClickListener() {
        return v -> {
            onLongClick();
            return true;
        };
    }


    @NonNull
    public View.OnClickListener getOnClickListener() {
        return v -> {
            if (hasActionMode()) {
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

        TreeViewAdapter treeViewAdapter = treeNodeCollection.getTreeViewAdapter();

        NodeContainer parent = getParent();

        mSelected = !mSelected;

        if (mSelected) {
            incrementSelected();

            if (parent.getSelectedChildren().size() == 1) // first in group
                parent.update();
        } else {
            decrementSelected();

            if (parent.getSelectedChildren().size() == 0) // last in group
                parent.update();
        }

        treeViewAdapter.notifyItemChanged(treeNodeCollection.getPosition(this));
    }

    @NonNull
    public NodeContainer getParent() {
        NodeContainer parent = mParentReference.get();
        Assert.assertTrue(parent != null);

        return parent;
    }

    private boolean hasActionMode() {
        return getTreeViewAdapter().hasActionMode();
    }

    private void incrementSelected() {
        getTreeViewAdapter().incrementSelected();
    }

    private void decrementSelected() {
        getTreeViewAdapter().decrementSelected();
    }

    @NonNull
    private TreeViewAdapter getTreeViewAdapter() {
        return getTreeNodeCollection().getTreeViewAdapter();
    }

    public int displayedSize() {
        if ((!mModelNode.visibleWhenEmpty() && mChildTreeNodes.isEmpty()) || (!mModelNode.visibleDuringActionMode() && hasActionMode())) {
            return 0;
        } else {
            if (mExpanded) {
                return 1 + Stream.of(mChildTreeNodes).map(TreeNode::displayedSize).reduce(0, (a, b) -> a + b);
            } else {
                return 1;
            }
        }
    }

    private int visibleSize() {
        if ((!mModelNode.visibleWhenEmpty() && mChildTreeNodes.isEmpty())) {
            return 0;
        } else {
            if (mExpanded) {
                return 1 + Stream.of(mChildTreeNodes).map(TreeNode::visibleSize).reduce(0, (a, b) -> a + b);
            } else {
                return 1;
            }
        }
    }

    @NonNull
    TreeNode getNode(int position) {
        Assert.assertTrue(position >= 0);
        Assert.assertTrue(!mChildTreeNodes.isEmpty() || mModelNode.visibleWhenEmpty());
        Assert.assertTrue(mModelNode.visibleDuringActionMode() || !hasActionMode());
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

    public int getPosition(@NonNull TreeNode treeNode) {
        if (treeNode == this)
            return 0;

        if (!mExpanded)
            return -1;

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

        TreeViewAdapter treeViewAdapter = treeNodeCollection.getTreeViewAdapter();

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

    public void selectAll() {
        Assert.assertTrue(!mSelected);

        TreeNodeCollection treeNodeCollection = getTreeNodeCollection();

        TreeViewAdapter treeViewAdapter = treeNodeCollection.getTreeViewAdapter();

        if (mModelNode.selectable()) {
            mSelected = true;

            treeViewAdapter.notifyItemChanged(treeNodeCollection.getPosition(this));

            treeViewAdapter.incrementSelected();
        }

        if (mExpanded) {
            Assert.assertTrue(!mChildTreeNodes.isEmpty());

            Stream.of(mChildTreeNodes).forEach(TreeNode::selectAll);
        }
    }

    @NonNull
    Stream<TreeNode> getSelectedNodes() {
        Assert.assertTrue(mChildTreeNodes != null);

        Assert.assertTrue(!mSelected || mModelNode.selectable());

        ArrayList<TreeNode> selectedTreeNodes = new ArrayList<>();

        if (mSelected)
            selectedTreeNodes.add(this);

        selectedTreeNodes.addAll(Stream.of(mChildTreeNodes)
                .flatMap(TreeNode::getSelectedNodes)
                .collect(Collectors.toList()));

        return Stream.of(selectedTreeNodes);
    }

    @NonNull
    public View.OnClickListener getExpandListener() {
        return v -> {
            Assert.assertTrue(!mChildTreeNodes.isEmpty());

            TreeNodeCollection treeNodeCollection = getTreeNodeCollection();

            TreeViewAdapter treeViewAdapter = treeNodeCollection.getTreeViewAdapter();

            Assert.assertTrue(!(!getSelectedChildren().isEmpty() && hasActionMode()));

            int position = treeNodeCollection.getPosition(this);
            Assert.assertTrue(position >= 0);

            if (mExpanded) { // hiding
                Assert.assertTrue(getSelectedChildren().isEmpty());

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

    private boolean visible() {
        NodeContainer nodeContainer = getParent();

        if (!mModelNode.visibleDuringActionMode() && hasActionMode())
            return false;

        if (!mModelNode.visibleWhenEmpty() && mChildTreeNodes.isEmpty())
            return false;

        if (nodeContainer instanceof TreeNodeCollection) {
            return true;
        } else {
            Assert.assertTrue(nodeContainer instanceof TreeNode);
            TreeNode parent = (TreeNode) nodeContainer;

            return (parent.visible() && parent.expanded());
        }
    }

    @Override
    public void remove(@NonNull TreeNode childTreeNode) {
        TreeNodeCollection treeNodeCollection = getTreeNodeCollection();

        TreeViewAdapter treeViewAdapter = treeNodeCollection.getTreeViewAdapter();

        Assert.assertTrue(!mChildTreeNodes.isEmpty());
        Assert.assertTrue(mChildTreeNodes.contains(childTreeNode));

        int childDisplayedSize = childTreeNode.displayedSize();

        int oldParentPosition = treeNodeCollection.getPosition(this);

        int oldChildPosition = treeNodeCollection.getPosition(childTreeNode);

        boolean expanded = expanded();
        boolean visible = visible();

        mChildTreeNodes.remove(childTreeNode);

        if (!visible)
            return;

        Assert.assertTrue(oldParentPosition >= 0);

        if (expanded) {
            Assert.assertTrue(oldChildPosition >= 0);

            if (Stream.of(mChildTreeNodes).map(TreeNode::displayedSize).reduce(0, (lhs, rhs) -> lhs + rhs) == 0) {
                mExpanded = false;

                if (oldParentPosition == 0) {
                    if (mModelNode.visibleWhenEmpty()) {
                        Log.e("asdf", "notifyItemChanged(" + oldParentPosition + ")");
                        treeViewAdapter.notifyItemChanged(oldParentPosition);
                        Log.e("asdf", "notifyItemRangeRemoved(" + (oldParentPosition + 1) + ", " + childDisplayedSize + ")");
                        treeViewAdapter.notifyItemRangeRemoved(oldParentPosition + 1, childDisplayedSize);
                    } else {
                        Log.e("asdf", "notifyItemRangeRemoved(" + (oldParentPosition + 1) + ", " + childDisplayedSize + ")");
                        treeViewAdapter.notifyItemRangeRemoved(oldParentPosition, 1 + childDisplayedSize);
                    }
                } else {
                    if (mModelNode.visibleWhenEmpty()) {
                        Log.e("asdf", "notifyItemRangeChanged(" + (oldParentPosition - 1) + ", " + 2 + ")");
                        treeViewAdapter.notifyItemRangeChanged(oldParentPosition - 1, 2);
                        Log.e("asdf", "notifyItemRangeRemoved(" + (oldParentPosition + 1) + ", " + childDisplayedSize + ")");
                        treeViewAdapter.notifyItemRangeRemoved(oldParentPosition + 1, childDisplayedSize);
                    } else {
                        Log.e("asdf", "notifyItemChanged(" + (oldParentPosition - 1) + ")");
                        treeViewAdapter.notifyItemChanged(oldParentPosition - 1);
                        Log.e("asdf", "notifyItemRangeRemoved(" + oldParentPosition + ", " + (1 + childDisplayedSize) + ")");
                        treeViewAdapter.notifyItemRangeRemoved(oldParentPosition, 1 + childDisplayedSize);
                    }
                }
            } else {
                Log.e("asdf", "notifyItemChanged(" + oldParentPosition + ")");
                treeViewAdapter.notifyItemChanged(oldParentPosition);
                Log.e("asdf", "notifyItemRangeRemoved(" + oldChildPosition + ", " + childDisplayedSize + ")");
                treeViewAdapter.notifyItemRangeRemoved(oldChildPosition, childDisplayedSize);

                Log.e("asdf", "notifyItemChanged(" + (oldChildPosition - 1) + ")");
                treeViewAdapter.notifyItemChanged(oldChildPosition - 1);

                if (oldParentPosition > 0) {
                    Log.e("asdf", "notifyItemChanged(" + (oldParentPosition - 1) + ")");
                    treeViewAdapter.notifyItemChanged(oldParentPosition - 1);
                }
            }
        } else {
            if (Stream.of(mChildTreeNodes).map(TreeNode::displayedSize).reduce(0, (lhs, rhs) -> lhs + rhs) == 0) {
                if (oldParentPosition == 0) {
                    if (mModelNode.visibleWhenEmpty()) {
                        treeViewAdapter.notifyItemChanged(oldParentPosition);
                    } else {
                        treeViewAdapter.notifyItemRemoved(oldParentPosition);
                    }
                } else {
                    if (mModelNode.visibleWhenEmpty()) {
                        treeViewAdapter.notifyItemChanged(oldParentPosition);
                    } else {
                        treeViewAdapter.notifyItemChanged(oldParentPosition - 1);
                        treeViewAdapter.notifyItemRemoved(oldParentPosition);
                    }
                }
            } else {
                treeViewAdapter.notifyItemChanged(oldParentPosition);

                if (oldParentPosition > 0) {
                    treeViewAdapter.notifyItemChanged(oldParentPosition - 1);
                }
            }
        }
    }

    public void removeAll() {
        List<TreeNode> oldChildTreeNodes = new ArrayList<>(mChildTreeNodes);
        Stream.of(oldChildTreeNodes)
                .forEach(this::remove);
    }

    @Override
    public void add(@NonNull TreeNode childTreeNode) {
        TreeNodeCollection treeNodeCollection = getTreeNodeCollection();

        TreeViewAdapter treeViewAdapter = treeNodeCollection.getTreeViewAdapter();

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

            if (mModelNode.visibleDuringActionMode() || !hasActionMode()) {
                int newParentPosition = treeNodeCollection.getPosition(this);
                Assert.assertTrue(newParentPosition >= 0);

                if (!mModelNode.visibleWhenEmpty() && mChildTreeNodes.size() == 1) {
                    Log.e("asdf", "notifyItemInserted(" + newParentPosition + ")");
                    treeViewAdapter.notifyItemInserted(newParentPosition);

                    if (newParentPosition > 0) {
                        Log.e("asdf", "notifyItemChanged(" + (newParentPosition - 1) + ")");
                        treeViewAdapter.notifyItemChanged(newParentPosition - 1);
                    }
                } else {
                    Log.e("asdf", "notifyItemChanged(" + newParentPosition + ")");
                    treeViewAdapter.notifyItemChanged(newParentPosition);
                }
            }
        }
    }

    public boolean getSeparatorVisibility() {
        NodeContainer parent = getParent();

        Assert.assertTrue(parent.expanded());

        TreeNodeCollection treeNodeCollection = getTreeNodeCollection();

        int positionInCollection = treeNodeCollection.getPosition(this);
        Assert.assertTrue(positionInCollection >= 0);

        if (positionInCollection == treeNodeCollection.displayedSize() - 1)
            return false;

        if (parent.getPosition(this) == parent.displayedSize() - 1)
            return true;

        TreeNode nextTreeNode = treeNodeCollection.getNode(positionInCollection + 1);
        Assert.assertTrue(nextTreeNode != null);

        return (nextTreeNode.expanded());
    }

    @NonNull
    @Override
    public List<TreeNode> getSelectedChildren() {
        Assert.assertTrue(!mChildTreeNodes.isEmpty());

        return Stream.of(mChildTreeNodes)
                .filter(TreeNode::isSelected)
                .collect(Collectors.toList());
    }

    void onCreateActionMode() {
        TreeNodeCollection treeNodeCollection = getTreeNodeCollection();

        TreeViewAdapter treeViewAdapter = treeNodeCollection.getTreeViewAdapter();

        int oldPosition = treeNodeCollection.getPosition(this);
        Assert.assertTrue(oldPosition >= 0);

        if (mModelNode.visibleDuringActionMode()) {
            treeViewAdapter.notifyItemChanged(oldPosition);

            if (mExpanded)
                Stream.of(mChildTreeNodes)
                        .forEach(TreeNode::onCreateActionMode);
        } else {
            if (mChildTreeNodes.size() > 0) {
                if (mExpanded) {
                    treeViewAdapter.notifyItemRangeRemoved(oldPosition, visibleSize());
                } else {
                    treeViewAdapter.notifyItemRemoved(oldPosition);
                }
            } else {
                treeViewAdapter.notifyItemRemoved(oldPosition);
            }

            if (oldPosition > 0)
                treeViewAdapter.notifyItemChanged(oldPosition - 1);
        }
    }

    void onDestroyActionMode() {
        TreeNodeCollection treeNodeCollection = getTreeNodeCollection();

        TreeViewAdapter treeViewAdapter = treeNodeCollection.getTreeViewAdapter();

        int position = treeNodeCollection.getPosition(this);
        Assert.assertTrue(position >= 0);

        if (mModelNode.visibleDuringActionMode()) {
            treeViewAdapter.notifyItemChanged(position);

            if (mExpanded)
                Stream.of(mChildTreeNodes)
                        .forEach(TreeNode::onDestroyActionMode);
        } else {
            if (mChildTreeNodes.size() > 0) {
                if (mExpanded) {
                    treeViewAdapter.notifyItemRangeInserted(position, displayedSize());
                } else {
                    treeViewAdapter.notifyItemInserted(position);
                }
            }

            if (position > 0)
                treeViewAdapter.notifyItemChanged(position - 1);
        }
    }

    @NonNull
    @Override
    public TreeNodeCollection getTreeNodeCollection() {
        return getParent().getTreeNodeCollection();
    }

    @NonNull
    public List<TreeNode> getAllChildren() {
        return mChildTreeNodes;
    }

    @Override
    public int getIndentation() {
        return getParent().getIndentation() + 1;
    }

    public void select() {
        Assert.assertTrue(!mSelected);
        Assert.assertTrue(mModelNode.selectable());

        onLongClick();
    }

    public void deselect() {
        Assert.assertTrue(mSelected);

        onLongClick();
    }
}
