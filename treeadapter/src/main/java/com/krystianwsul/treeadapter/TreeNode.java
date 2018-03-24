package com.krystianwsul.treeadapter;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.annimon.stream.function.BiFunction;
import com.annimon.stream.function.Consumer;
import com.annimon.stream.function.Function;
import com.annimon.stream.function.Predicate;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TreeNode implements Comparable<TreeNode>, NodeContainer {
    @NonNull
    private final ModelNode mModelNode;

    @Nullable
    private List<TreeNode> mChildTreeNodes;

    @NonNull
    private final NodeContainer mParent;

    private boolean mExpanded;

    private boolean mSelected = false;

    public TreeNode(@NonNull ModelNode modelNode, @NonNull NodeContainer parent, boolean expanded, boolean selected) {
        if (selected && !modelNode.isSelectable())
            throw new NotSelectableSelectedException();

        if (modelNode.isSelectable() && !modelNode.isVisibleDuringActionMode())
            throw new SelectableNotVisibleException();

        mModelNode = modelNode;
        mParent = parent;

        mExpanded = expanded;
        mSelected = selected;
    }

    public void setChildTreeNodes(@NonNull List<TreeNode> childTreeNodes) {
        if (mChildTreeNodes != null)
            throw new SetChildTreeNodesCalledTwiceException();

        if (mExpanded && childTreeNodes.isEmpty())
            throw new EmptyExpandedException();

        mChildTreeNodes = new ArrayList<>(childTreeNodes);

        Collections.sort(mChildTreeNodes);
    }

    void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder) {
        mModelNode.onBindViewHolder(viewHolder);
    }

    int getItemViewType() {
        return mModelNode.getItemViewType();
    }

    @Override
    public void update() {
        TreeNodeCollection treeNodeCollection = getTreeNodeCollection();

        treeNodeCollection.getMTreeViewAdapter().notifyItemChanged(treeNodeCollection.getPosition(this));
    }

    @Override
    public void updateRecursive() {
        update();

        getParent().updateRecursive();
    }

    @NonNull
    public ModelNode getModelNode() {
        return mModelNode;
    }

    @Override
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
        return new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                TreeNode.this.onLongClick();
                return true;
            }
        };
    }

    @NonNull
    public View.OnClickListener getOnClickListener() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (hasActionMode()) {
                    onLongClick();
                } else {
                    mModelNode.onClick();
                }
            }
        };
    }

    private void onLongClick() {
        if (!mModelNode.isSelectable())
            return;

        TreeNodeCollection treeNodeCollection = getTreeNodeCollection();

        NodeContainer parent = getParent();

        mSelected = !mSelected;

        if (mSelected) {
            incrementSelected();

            if (parent.getSelectedChildren().size() == 1) // first in group
                parent.updateRecursive();
        } else {
            decrementSelected();

            if (parent.getSelectedChildren().size() == 0) // last in group
                parent.updateRecursive();
        }

        treeNodeCollection.getMTreeViewAdapter().notifyItemChanged(treeNodeCollection.getPosition(this));
    }

    @NonNull
    public NodeContainer getParent() {
        return mParent;
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
        return getTreeNodeCollection().getMTreeViewAdapter();
    }

    @Override
    public int displayedSize() {
        if (mChildTreeNodes == null)
            throw new SetChildTreeNodesNotCalledException();

        if ((!mModelNode.isVisibleWhenEmpty() && mChildTreeNodes.isEmpty()) || (!mModelNode.isVisibleDuringActionMode() && hasActionMode())) {
            return 0;
        } else {
            if (mExpanded) {
                return 1 + Stream.of(mChildTreeNodes)
                        .map(new Function<TreeNode, Integer>() {
                            @Override
                            public Integer apply(TreeNode treeNode) {
                                return treeNode.displayedSize();
                            }
                        })
                        .reduce(0, new BiFunction<Integer, Integer, Integer>() {
                            @Override
                            public Integer apply(Integer a, Integer b) {
                                return a + b;
                            }
                        });
            } else {
                return 1;
            }
        }
    }

    private int visibleSize() {
        if (mChildTreeNodes == null)
            throw new SetChildTreeNodesNotCalledException();

        if ((!mModelNode.isVisibleWhenEmpty() && mChildTreeNodes.isEmpty())) {
            return 0;
        } else {
            if (mExpanded) {
                return 1 + Stream.of(mChildTreeNodes)
                        .map(new Function<TreeNode, Integer>() {
                            @Override
                            public Integer apply(TreeNode treeNode) {
                                return treeNode.visibleSize();
                            }
                        })
                        .reduce(0, new BiFunction<Integer, Integer, Integer>() {
                            @Override
                            public Integer apply(Integer a, Integer b) {
                                return a + b;
                            }
                        });
            } else {
                return 1;
            }
        }
    }

    @NonNull
    TreeNode getNode(int position) {
        if (mChildTreeNodes == null)
            throw new SetChildTreeNodesNotCalledException();

        Assert.assertTrue(position >= 0);
        Assert.assertTrue(!mChildTreeNodes.isEmpty() || mModelNode.isVisibleWhenEmpty());
        Assert.assertTrue(mModelNode.isVisibleDuringActionMode() || !hasActionMode());
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

    @Override
    public int getPosition(@NonNull TreeNode treeNode) {
        if (mChildTreeNodes == null)
            throw new SetChildTreeNodesNotCalledException();

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
        Assert.assertTrue(!mSelected || mModelNode.isSelectable());

        return mSelected;
    }

    public void unselect() {
        Assert.assertTrue(!mSelected || mModelNode.isSelectable());

        TreeNodeCollection treeNodeCollection = getTreeNodeCollection();

        if (mSelected) {
            Assert.assertTrue(mModelNode.isSelectable());
            Assert.assertTrue(mModelNode.isVisibleDuringActionMode());

            mSelected = false;
            treeNodeCollection.getMTreeViewAdapter().notifyItemChanged(treeNodeCollection.getPosition(this));
        }

        List<TreeNode> selected = getSelectedNodes()
                .collect(Collectors.<TreeNode>toList());

        if (!selected.isEmpty()) {
            Assert.assertTrue(mExpanded);

            Stream.of(selected)
                    .forEach(new Consumer<TreeNode>() {
                        @Override
                        public void accept(TreeNode treeNode) {
                            treeNode.unselect();
                        }
                    });

            treeNodeCollection.getMTreeViewAdapter().notifyItemChanged(treeNodeCollection.getPosition(this));
        }
    }

    void selectAll() {
        if (mChildTreeNodes == null)
            throw new SetChildTreeNodesNotCalledException();

        if (mSelected)
            throw new SelectAllException();

        TreeNodeCollection treeNodeCollection = getTreeNodeCollection();

        if (mModelNode.isSelectable()) {
            Assert.assertTrue(mModelNode.isVisibleDuringActionMode());

            mSelected = true;

            treeNodeCollection.getMTreeViewAdapter().notifyItemChanged(treeNodeCollection.getPosition(this));

            treeNodeCollection.getMTreeViewAdapter().incrementSelected();
        }

        if (mExpanded) {
            Assert.assertTrue(!mChildTreeNodes.isEmpty());

            Stream.of(mChildTreeNodes)
                    .forEach(new Consumer<TreeNode>() {
                        @Override
                        public void accept(TreeNode treeNode) {
                            treeNode.selectAll();
                        }
                    });
        }
    }

    @NonNull
    Stream<TreeNode> getSelectedNodes() {
        if (mChildTreeNodes == null)
            throw new SetChildTreeNodesNotCalledException();

        Assert.assertTrue(!mSelected || mModelNode.isSelectable());

        ArrayList<TreeNode> selectedTreeNodes = new ArrayList<>();

        if (mSelected) {
            Assert.assertTrue(mModelNode.isVisibleDuringActionMode());
            selectedTreeNodes.add(this);
        }

        selectedTreeNodes.addAll(Stream.of(mChildTreeNodes)
                .flatMap(new Function<TreeNode, Stream<TreeNode>>() {
                    @Override
                    public Stream<TreeNode> apply(TreeNode treeNode) {
                        return treeNode.getSelectedNodes();
                    }
                })
                .collect(Collectors.<TreeNode>toList()));

        return Stream.of(selectedTreeNodes);
    }

    @SuppressWarnings("RedundantIfStatement")
    public boolean canBeShown() {
        if (mChildTreeNodes == null)
            throw new SetChildTreeNodesNotCalledException();

        if (!mModelNode.isVisibleDuringActionMode() && hasActionMode())
            return false;

        if (!mModelNode.isVisibleWhenEmpty() && mChildTreeNodes.isEmpty())
            return false;

        return true;
    }

    @SuppressWarnings("RedundantIfStatement")
    public boolean getExpandVisible() {
        if (mChildTreeNodes == null)
            throw new SetChildTreeNodesNotCalledException();

        if (!visible())
            throw new InvisibleNodeException();

        if (mChildTreeNodes.isEmpty())
            return false;

        if (Stream.of(mChildTreeNodes).noneMatch(new Predicate<TreeNode>() {
            @Override
            public boolean test(TreeNode value) {
                return value.canBeShown();
            }
        }))
            return false;

        if (hasActionMode() && hasSelectedDescendants())
            return false;

        return true;
    }

    @NonNull
    public View.OnClickListener getExpandListener() {
        if (mChildTreeNodes == null)
            throw new SetChildTreeNodesNotCalledException();

        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mChildTreeNodes.isEmpty())
                    throw new EmptyExpandedException();

                TreeNodeCollection treeNodeCollection = getTreeNodeCollection();

                if (hasSelectedDescendants() && hasActionMode())
                    throw new DescendantSelectedException();

                int position = treeNodeCollection.getPosition(TreeNode.this);
                Assert.assertTrue(position >= 0);

                if (mExpanded) { // hiding
                    if (hasSelectedDescendants())
                        throw new SelectedChildrenException();

                    int displayedSize = displayedSize();
                    mExpanded = false;
                    treeNodeCollection.getMTreeViewAdapter().notifyItemRangeRemoved(position + 1, displayedSize - 1);
                } else { // showing
                    mExpanded = true;
                    treeNodeCollection.getMTreeViewAdapter().notifyItemRangeInserted(position + 1, displayedSize() - 1);
                }

                if (position > 0) {
                    treeNodeCollection.getMTreeViewAdapter().notifyItemRangeChanged(position - 1, 2);
                } else {
                    treeNodeCollection.getMTreeViewAdapter().notifyItemChanged(position);
                }
            }
        };
    }

    private boolean visible() {
        if (mChildTreeNodes == null)
            throw new SetChildTreeNodesNotCalledException();

        NodeContainer nodeContainer = getParent();

        if (!canBeShown())
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
        if (mChildTreeNodes == null)
            throw new SetChildTreeNodesNotCalledException();

        TreeNodeCollection treeNodeCollection = getTreeNodeCollection();

        if (!mChildTreeNodes.contains(childTreeNode))
            throw new NoSuchNodeException();

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

            if (0 == Stream.of(mChildTreeNodes)
                    .map(new Function<TreeNode, Integer>() {
                        @Override
                        public Integer apply(TreeNode treeNode) {
                            return treeNode.displayedSize();
                        }
                    })
                    .reduce(0, new BiFunction<Integer, Integer, Integer>() {
                        @Override
                        public Integer apply(Integer a, Integer b) {
                            return a + b;
                        }
                    })) {
                mExpanded = false;

                if (oldParentPosition == 0) {
                    if (mModelNode.isVisibleWhenEmpty()) {
                        treeNodeCollection.getMTreeViewAdapter().notifyItemChanged(oldParentPosition);
                        treeNodeCollection.getMTreeViewAdapter().notifyItemRangeRemoved(oldParentPosition + 1, childDisplayedSize);
                    } else {
                        treeNodeCollection.getMTreeViewAdapter().notifyItemRangeRemoved(oldParentPosition, 1 + childDisplayedSize);
                    }
                } else {
                    if (mModelNode.isVisibleWhenEmpty()) {
                        treeNodeCollection.getMTreeViewAdapter().notifyItemRangeChanged(oldParentPosition - 1, 2);
                        treeNodeCollection.getMTreeViewAdapter().notifyItemRangeRemoved(oldParentPosition + 1, childDisplayedSize);
                    } else {
                        treeNodeCollection.getMTreeViewAdapter().notifyItemChanged(oldParentPosition - 1);
                        treeNodeCollection.getMTreeViewAdapter().notifyItemRangeRemoved(oldParentPosition, 1 + childDisplayedSize);
                    }
                }
            } else {
                treeNodeCollection.getMTreeViewAdapter().notifyItemChanged(oldParentPosition);
                treeNodeCollection.getMTreeViewAdapter().notifyItemRangeRemoved(oldChildPosition, childDisplayedSize);

                treeNodeCollection.getMTreeViewAdapter().notifyItemChanged(oldChildPosition - 1);

                if (oldParentPosition > 0)
                    treeNodeCollection.getMTreeViewAdapter().notifyItemChanged(oldParentPosition - 1);
            }
        } else {
            if (0 == Stream.of(mChildTreeNodes)
                    .map(new Function<TreeNode, Integer>() {
                        @Override
                        public Integer apply(TreeNode treeNode) {
                            return treeNode.displayedSize();
                        }
                    })
                    .reduce(0, new BiFunction<Integer, Integer, Integer>() {
                        @Override
                        public Integer apply(Integer a, Integer b) {
                            return a + b;
                        }
                    })) {
                if (oldParentPosition == 0) {
                    if (mModelNode.isVisibleWhenEmpty()) {
                        treeNodeCollection.getMTreeViewAdapter().notifyItemChanged(oldParentPosition);
                    } else {
                        treeNodeCollection.getMTreeViewAdapter().notifyItemRemoved(oldParentPosition);
                    }
                } else {
                    if (mModelNode.isVisibleWhenEmpty()) {
                        treeNodeCollection.getMTreeViewAdapter().notifyItemChanged(oldParentPosition);
                    } else {
                        treeNodeCollection.getMTreeViewAdapter().notifyItemChanged(oldParentPosition - 1);
                        treeNodeCollection.getMTreeViewAdapter().notifyItemRemoved(oldParentPosition);
                    }
                }
            } else {
                treeNodeCollection.getMTreeViewAdapter().notifyItemChanged(oldParentPosition);

                if (oldParentPosition > 0)
                    treeNodeCollection.getMTreeViewAdapter().notifyItemChanged(oldParentPosition - 1);
            }
        }
    }

    public void removeAll() {
        if (mChildTreeNodes == null)
            throw new SetChildTreeNodesNotCalledException();

        List<TreeNode> oldChildTreeNodes = new ArrayList<>(mChildTreeNodes);
        Stream.of(oldChildTreeNodes)
                .forEach(new Consumer<TreeNode>() {
                    @Override
                    public void accept(TreeNode treeNode) {
                        remove(treeNode);
                    }
                });
    }

    @Override
    public void add(@NonNull TreeNode childTreeNode) {
        if (mChildTreeNodes == null)
            throw new SetChildTreeNodesNotCalledException();

        TreeNodeCollection treeNodeCollection = getTreeNodeCollection();

        if (mExpanded) {
            if (mModelNode.isVisibleWhenEmpty()) {
                int oldParentPosition = treeNodeCollection.getPosition(this);
                Assert.assertTrue(oldParentPosition >= 0);

                mChildTreeNodes.add(childTreeNode);

                Collections.sort(mChildTreeNodes);

                int newChildPosition = treeNodeCollection.getPosition(childTreeNode);
                Assert.assertTrue(newChildPosition >= 0);

                treeNodeCollection.getMTreeViewAdapter().notifyItemChanged(oldParentPosition);
                treeNodeCollection.getMTreeViewAdapter().notifyItemInserted(newChildPosition);

                boolean last = (oldParentPosition + displayedSize() - 1 == newChildPosition);

                if (last)
                    treeNodeCollection.getMTreeViewAdapter().notifyItemChanged(newChildPosition - 1);

                if (oldParentPosition > 0)
                    treeNodeCollection.getMTreeViewAdapter().notifyItemChanged(oldParentPosition - 1);
            } else {
                if (mChildTreeNodes.isEmpty()) {
                    mChildTreeNodes.add(childTreeNode);

                    Collections.sort(mChildTreeNodes);

                    int newParentPosition = treeNodeCollection.getPosition(this);

                    treeNodeCollection.getMTreeViewAdapter().notifyItemInserted(newParentPosition + 1);

                    if (newParentPosition > 0)
                        treeNodeCollection.getMTreeViewAdapter().notifyItemChanged(newParentPosition - 1);
                } else {
                    int oldParentPosition = treeNodeCollection.getPosition(this);
                    Assert.assertTrue(oldParentPosition >= 0);

                    mChildTreeNodes.add(childTreeNode);

                    Collections.sort(mChildTreeNodes);

                    int newChildPosition = treeNodeCollection.getPosition(childTreeNode);
                    Assert.assertTrue(newChildPosition >= 0);

                    treeNodeCollection.getMTreeViewAdapter().notifyItemChanged(oldParentPosition);
                    treeNodeCollection.getMTreeViewAdapter().notifyItemInserted(newChildPosition);

                    boolean last = (oldParentPosition + displayedSize() - 1 == newChildPosition);

                    if (last)
                        treeNodeCollection.getMTreeViewAdapter().notifyItemChanged(newChildPosition - 1);

                    if (oldParentPosition > 0)
                        treeNodeCollection.getMTreeViewAdapter().notifyItemChanged(oldParentPosition - 1);
                }
            }
        } else {
            mChildTreeNodes.add(childTreeNode);

            Collections.sort(mChildTreeNodes);

            if (mModelNode.isVisibleDuringActionMode() || !hasActionMode()) {
                int newParentPosition = treeNodeCollection.getPosition(this);
                Assert.assertTrue(newParentPosition >= 0);

                if (!mModelNode.isVisibleWhenEmpty() && mChildTreeNodes.size() == 1) {
                    treeNodeCollection.getMTreeViewAdapter().notifyItemInserted(newParentPosition);

                    if (newParentPosition > 0)
                        treeNodeCollection.getMTreeViewAdapter().notifyItemChanged(newParentPosition - 1);
                } else {
                    treeNodeCollection.getMTreeViewAdapter().notifyItemChanged(newParentPosition);
                }
            }
        }
    }

    public boolean getSeparatorVisibility() {
        NodeContainer parent = getParent();

        if (!parent.expanded())
            throw new InvisibleNodeException();

        TreeNodeCollection treeNodeCollection = getTreeNodeCollection();

        int positionInCollection = treeNodeCollection.getPosition(this);
        Assert.assertTrue(positionInCollection >= 0);

        if (positionInCollection == treeNodeCollection.displayedSize() - 1)
            return false;

        if (parent.getPosition(this) == parent.displayedSize() - 1)
            return true;

        TreeNode nextTreeNode = treeNodeCollection.getNode(positionInCollection + 1);

        return (nextTreeNode.expanded() || mModelNode.isSeparatorVisibleWhenNotExpanded());
    }

    @NonNull
    @Override
    public List<TreeNode> getSelectedChildren() {
        if (mChildTreeNodes == null)
            throw new SetChildTreeNodesNotCalledException();

        if (mChildTreeNodes.isEmpty())
            throw new NoChildrenException();

        return Stream.of(mChildTreeNodes)
                .filter(new Predicate<TreeNode>() {
                    @Override
                    public boolean test(TreeNode treeNode) {
                        return treeNode.isSelected();
                    }
                })
                .collect(Collectors.<TreeNode>toList());
    }

    public boolean hasSelectedDescendants() {
        if (mChildTreeNodes == null)
            throw new SetChildTreeNodesNotCalledException();

        return Stream.of(mChildTreeNodes).anyMatch(new Predicate<TreeNode>() {
            @Override
            public boolean test(TreeNode value) {
                return (value.isSelected() || value.hasSelectedDescendants());
            }
        });
    }

    void onCreateActionMode() {
        if (mChildTreeNodes == null)
            throw new SetChildTreeNodesNotCalledException();

        TreeNodeCollection treeNodeCollection = getTreeNodeCollection();

        int position = treeNodeCollection.getPosition(this);
        Assert.assertTrue(position >= 0);

        if (mModelNode.isVisibleDuringActionMode()) {
            treeNodeCollection.getMTreeViewAdapter().notifyItemChanged(position);

            if (mExpanded)
                Stream.of(mChildTreeNodes)
                        .forEach(new Consumer<TreeNode>() {
                            @Override
                            public void accept(TreeNode treeNode) {
                                treeNode.onCreateActionMode();
                            }
                        });
        } else {
            if (mChildTreeNodes.size() > 0) {
                if (mExpanded) {
                    treeNodeCollection.getMTreeViewAdapter().notifyItemRangeRemoved(position, visibleSize());
                } else {
                    treeNodeCollection.getMTreeViewAdapter().notifyItemRemoved(position);
                }
            } else if (mModelNode.isVisibleWhenEmpty()) {
                treeNodeCollection.getMTreeViewAdapter().notifyItemRemoved(position);
            }

            if (position > 0)
                treeNodeCollection.getMTreeViewAdapter().notifyItemChanged(position - 1);
        }
    }

    void onDestroyActionMode() {
        if (mChildTreeNodes == null)
            throw new SetChildTreeNodesNotCalledException();

        TreeNodeCollection treeNodeCollection = getTreeNodeCollection();

        int position = treeNodeCollection.getPosition(this);
        Assert.assertTrue(position >= 0);

        if (mModelNode.isVisibleDuringActionMode()) {
            treeNodeCollection.getMTreeViewAdapter().notifyItemChanged(position);

            if (mExpanded)
                Stream.of(mChildTreeNodes)
                        .forEach(new Consumer<TreeNode>() {
                            @Override
                            public void accept(TreeNode treeNode) {
                                treeNode.onDestroyActionMode();
                            }
                        });
        } else {
            if (mChildTreeNodes.size() > 0) {
                if (mExpanded) {
                    treeNodeCollection.getMTreeViewAdapter().notifyItemRangeInserted(position, displayedSize());
                } else {
                    treeNodeCollection.getMTreeViewAdapter().notifyItemInserted(position);
                }
            } else if (mModelNode.isVisibleWhenEmpty()) {
                treeNodeCollection.getMTreeViewAdapter().notifyItemInserted(position);
            }

            if (position > 0)
                treeNodeCollection.getMTreeViewAdapter().notifyItemChanged(position - 1);
        }
    }

    @NonNull
    @Override
    public TreeNodeCollection getTreeNodeCollection() {
        return getParent().getTreeNodeCollection();
    }

    @NonNull
    public List<TreeNode> getAllChildren() {
        if (mChildTreeNodes == null)
            throw new SetChildTreeNodesNotCalledException();

        return mChildTreeNodes;
    }

    @Override
    public int getIndentation() {
        return getParent().getIndentation() + 1;
    }

    public void select() {
        if (mSelected)
            throw new SelectCalledTwiceException();

        if (!mModelNode.isSelectable())
            throw new NotSelectableSelectedException();

        onLongClick();
    }

    public void deselect() {
        if (!mSelected)
            throw new NotSelectedException();

        onLongClick();
    }

    public boolean isExpanded() {
        return mExpanded;
    }

    @SuppressWarnings("WeakerAccess")
    public static class SetChildTreeNodesNotCalledException extends InitializationException {
        private SetChildTreeNodesNotCalledException() {
            super("TreeNode.setChildTreeNodes() has not been called.");
        }
    }

    @SuppressWarnings("WeakerAccess")
    public static class SetChildTreeNodesCalledTwiceException extends InitializationException {
        private SetChildTreeNodesCalledTwiceException() {
            super("TreeNode.setChildTreeNodes() has already been called.");
        }
    }

    @SuppressWarnings("WeakerAccess")
    public static class NotSelectableSelectedException extends IllegalStateException {
        private NotSelectableSelectedException() {
            super("A TreeNode cannot be selected if its ModelNode is not selectable.");
        }
    }

    @SuppressWarnings("WeakerAccess")
    public static class SelectableNotVisibleException extends IllegalStateException {
        private SelectableNotVisibleException() {
            super("A TreeNode cannot be selectable if it isn't visible during action mode.");
        }
    }

    @SuppressWarnings("WeakerAccess")
    public static class EmptyExpandedException extends IllegalStateException {
        private EmptyExpandedException() {
            super("A TreeNode cannot be expanded if it has no children.");
        }
    }

    @SuppressWarnings("WeakerAccess")
    public static class SelectAllException extends UnsupportedOperationException {
        private SelectAllException() {
            super("TreeViewAdapter.selectAll() can be called only if no nodes are selected.");
        }
    }

    @SuppressWarnings("WeakerAccess")
    public static class SelectedChildrenException extends UnsupportedOperationException {
        private SelectedChildrenException() {
            super("A TreeNode cannot be collaped if it has selected children.");
        }
    }

    @SuppressWarnings("WeakerAccess")
    public static class NoSuchNodeException extends IllegalArgumentException {
        private NoSuchNodeException() {
            super("The given node is not a direct descendant of this TreeNode.");
        }
    }

    @SuppressWarnings("WeakerAccess")
    public static class InvisibleNodeException extends UnsupportedOperationException {
        private InvisibleNodeException() {
            super("This operation is meaningless if the node is not visible.");
        }
    }

    @SuppressWarnings("WeakerAccess")
    public static class NoChildrenException extends UnsupportedOperationException {
        private NoChildrenException() {
            super("Can't get selected children of a node that has no children.");
        }
    }

    @SuppressWarnings("WeakerAccess")
    public static class SelectCalledTwiceException extends UnsupportedOperationException {
        private SelectCalledTwiceException() {
            super("Can't select a node that is already selected.");
        }
    }

    @SuppressWarnings("WeakerAccess")
    public static class NotSelectedException extends UnsupportedOperationException {
        private NotSelectedException() {
            super("Can't deselect a node that is not selected.");
        }
    }

    @SuppressWarnings("WeakerAccess")
    public static class DescendantSelectedException extends UnsupportedOperationException {
        private DescendantSelectedException() {
            super("Can't change a node's expansion state when it has selected descendants.");
        }
    }
}
