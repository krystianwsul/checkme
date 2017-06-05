package com.krystianwsul.treeadapter;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.annimon.stream.function.Consumer;
import com.annimon.stream.function.Function;

import junit.framework.Assert;

import java.util.Collections;
import java.util.List;

public class TreeNodeCollection implements NodeContainer {
    @Nullable
    private List<TreeNode> mTreeNodes;

    @NonNull
    final TreeViewAdapter mTreeViewAdapter;

    public TreeNodeCollection(@NonNull TreeViewAdapter treeViewAdapter) {
        mTreeViewAdapter = treeViewAdapter;
    }

    @NonNull
    TreeNode getNode(int position) {
        if (mTreeNodes == null)
            throw new SetTreeNodesNotCalledException();

        Assert.assertTrue(position >= 0);
        Assert.assertTrue(position < displayedSize());

        for (TreeNode treeNode : mTreeNodes) {
            if (position < treeNode.displayedSize())
                return treeNode.getNode(position);

            position = position - treeNode.displayedSize();
        }

        throw new IndexOutOfBoundsException();
    }

    public int getPosition(@NonNull TreeNode treeNode) {
        if (mTreeNodes == null)
            throw new SetTreeNodesNotCalledException();

        int offset = 0;
        for (TreeNode notDoneGroupTreeNode : mTreeNodes) {
            int position = notDoneGroupTreeNode.getPosition(treeNode);
            if (position >= 0)
                return offset + position;
            offset += notDoneGroupTreeNode.displayedSize();
        }

        return -1;
    }

    int getItemViewType(int position) {
        TreeNode treeNode = getNode(position);

        return treeNode.getItemViewType();
    }

    public void setNodes(@NonNull List<TreeNode> rootTreeNodes) {
        Assert.assertTrue(mTreeNodes == null);

        mTreeNodes = rootTreeNodes;

        Collections.sort(mTreeNodes);
    }

    public int displayedSize() {
        if (mTreeNodes == null)
            throw new SetTreeNodesNotCalledException();

        int displayedSize = 0;
        for (TreeNode notDoneGroupTreeNode : mTreeNodes)
            displayedSize += notDoneGroupTreeNode.displayedSize();
        return displayedSize;
    }

    @NonNull
    List<TreeNode> getSelectedNodes() {
        if (mTreeNodes == null)
            throw new SetTreeNodesNotCalledException();

        return Stream.of(mTreeNodes)
                .flatMap(new Function<TreeNode, Stream<TreeNode>>() {
                    @Override
                    public Stream<TreeNode> apply(TreeNode treeNode) {
                        return treeNode.getSelectedNodes();
                    }
                })
                .collect(Collectors.<TreeNode>toList());
    }

    void onCreateActionMode() {
        if (mTreeNodes == null)
            throw new SetTreeNodesNotCalledException();

        Stream.of(mTreeNodes)
                .forEach(new Consumer<TreeNode>() {
                    @Override
                    public void accept(TreeNode treeNode) {
                        treeNode.onCreateActionMode();
                    }
                });
    }

    void onDestroyActionMode() {
        if (mTreeNodes == null)
            throw new SetTreeNodesNotCalledException();

        Stream.of(mTreeNodes)
                .forEach(new Consumer<TreeNode>() {
                    @Override
                    public void accept(TreeNode treeNode) {
                        treeNode.onDestroyActionMode();
                    }
                });
    }

    void unselect() {
        if (mTreeNodes == null)
            throw new SetTreeNodesNotCalledException();

        Stream.of(mTreeNodes)
                .forEach(new Consumer<TreeNode>() {
                    @Override
                    public void accept(TreeNode treeNode) {
                        treeNode.unselect();
                    }
                });
    }

    @Override
    public void add(@NonNull TreeNode notDoneGroupTreeNode) {
        if (mTreeNodes == null)
            throw new SetTreeNodesNotCalledException();

        mTreeNodes.add(notDoneGroupTreeNode);

        Collections.sort(mTreeNodes);

        TreeViewAdapter treeViewAdapter = mTreeViewAdapter;

        int newPosition = getPosition(notDoneGroupTreeNode);
        Assert.assertTrue(newPosition >= 0);

        treeViewAdapter.notifyItemInserted(newPosition);

        if (newPosition > 0)
            treeViewAdapter.notifyItemChanged(newPosition - 1);
    }

    @Override
    public void remove(@NonNull TreeNode notDoneGroupTreeNode) {
        if (mTreeNodes == null)
            throw new SetTreeNodesNotCalledException();

        Assert.assertTrue(mTreeNodes.contains(notDoneGroupTreeNode));

        TreeViewAdapter treeViewAdapter = mTreeViewAdapter;

        int oldPosition = getPosition(notDoneGroupTreeNode);
        Assert.assertTrue(oldPosition >= 0);

        int displayedSize = notDoneGroupTreeNode.displayedSize();

        mTreeNodes.remove(notDoneGroupTreeNode);

        treeViewAdapter.notifyItemRangeRemoved(oldPosition, displayedSize);

        if (oldPosition > 0)
            treeViewAdapter.notifyItemChanged(oldPosition - 1);
    }

    @Override
    public boolean expanded() {
        return true;
    }

    @Override
    public void update() {

    }

    @NonNull
    @Override
    public List<TreeNode> getSelectedChildren() {
        return getSelectedNodes();
    }

    @NonNull
    @Override
    public TreeNodeCollection getTreeNodeCollection() {
        return this;
    }

    public void selectAll() {
        if (mTreeNodes == null)
            throw new SetTreeNodesNotCalledException();

        Stream.of(mTreeNodes)
                .forEach(new Consumer<TreeNode>() {
                    @Override
                    public void accept(TreeNode treeNode) {
                        treeNode.selectAll();
                    }
                });
    }

    @Override
    public int getIndentation() {
        return 0;
    }

    public static class SetTreeNodesNotCalledException extends InitializationException {
        private SetTreeNodesNotCalledException() {
            super("TreeNodeCollection.seTreeNodes() has not been called.");
        }
    }
}
