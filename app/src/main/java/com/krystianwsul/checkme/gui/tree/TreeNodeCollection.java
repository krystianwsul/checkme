package com.krystianwsul.checkme.gui.tree;

import android.support.annotation.NonNull;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;

import junit.framework.Assert;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.List;

public class TreeNodeCollection implements NodeContainer {
    private List<TreeNode> mTreeNodes;

    private final WeakReference<TreeViewAdapter> mTreeViewAdapterReference;

    public TreeNodeCollection(WeakReference<TreeViewAdapter> treeViewAdapterReference) {
        Assert.assertTrue(treeViewAdapterReference != null);

        mTreeViewAdapterReference = treeViewAdapterReference;
    }

    TreeNode getNode(int position) {
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
        Assert.assertTrue(treeNode != null);

        return treeNode.getItemViewType();
    }

    public void setNodes(List<TreeNode> rootTreeNodes) {
        Assert.assertTrue(rootTreeNodes != null);

        mTreeNodes = rootTreeNodes;

        Collections.sort(mTreeNodes);
    }

    @NonNull
    TreeViewAdapter getTreeViewAdapter() {
        TreeViewAdapter treeViewAdapter = mTreeViewAdapterReference.get();
        Assert.assertTrue(treeViewAdapter != null);

        return treeViewAdapter;
    }

    public int displayedSize() {
        int displayedSize = 0;
        for (TreeNode notDoneGroupTreeNode : mTreeNodes)
            displayedSize += notDoneGroupTreeNode.displayedSize();
        return displayedSize;
    }

    @NonNull
    List<TreeNode> getSelectedNodes() {
        return Stream.of(mTreeNodes)
                .flatMap(TreeNode::getSelectedNodes)
                .collect(Collectors.toList());
    }

    void onCreateActionMode() {
        Stream.of(mTreeNodes)
                .forEach(TreeNode::onCreateActionMode);
    }

    void onDestroyActionMode() {
        Stream.of(mTreeNodes)
                .forEach(TreeNode::onDestroyActionMode);
    }

    void unselect() {
        Stream.of(mTreeNodes)
                .forEach(TreeNode::unselect);
    }

    @Override
    public void add(@NonNull TreeNode notDoneGroupTreeNode) {
        mTreeNodes.add(notDoneGroupTreeNode);

        Collections.sort(mTreeNodes);

        TreeViewAdapter treeViewAdapter = getTreeViewAdapter();

        int newPosition = getPosition(notDoneGroupTreeNode);
        Assert.assertTrue(newPosition >= 0);

        treeViewAdapter.notifyItemInserted(newPosition);

        if (newPosition > 0)
            treeViewAdapter.notifyItemChanged(newPosition - 1);
    }

    @Override
    public void remove(@NonNull TreeNode notDoneGroupTreeNode) {
        Assert.assertTrue(mTreeNodes.contains(notDoneGroupTreeNode));

        TreeViewAdapter treeViewAdapter = getTreeViewAdapter();

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
        Stream.of(mTreeNodes).forEach(TreeNode::selectAll);
    }

    @Override
    public int getIndentation() {
        return 0;
    }
}
