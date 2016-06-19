package com.krystianwsul.checkme.gui.instances.tree;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;

import junit.framework.Assert;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.List;

public class TreeNodeCollection implements NodeContainer {
    private List<TreeNode> mNotDoneGroupTreeNodes;

    private final ModelNodeCollection mModelNodeCollection;

    private final WeakReference<TreeViewAdapter> mTreeViewAdapterReference;

    public TreeNodeCollection(ModelNodeCollection modelNodeCollection, WeakReference<TreeViewAdapter> treeViewAdapterReference) {
        Assert.assertTrue(modelNodeCollection != null);
        Assert.assertTrue(treeViewAdapterReference != null);

        mModelNodeCollection = modelNodeCollection;
        mTreeViewAdapterReference = treeViewAdapterReference;
    }

    public TreeNode getNode(int position) {
        Assert.assertTrue(position >= 0);
        Assert.assertTrue(position < displayedSize());

        for (TreeNode notDoneGroupTreeNode : mNotDoneGroupTreeNodes) {
            if (position < notDoneGroupTreeNode.displayedSize())
                return notDoneGroupTreeNode.getNode(position);

            position = position - notDoneGroupTreeNode.displayedSize();
        }

        throw new IndexOutOfBoundsException();
    }

    public int getPosition(TreeNode treeNode) {
        int offset = 0;
        for (TreeNode notDoneGroupTreeNode : mNotDoneGroupTreeNodes) {
            int position = notDoneGroupTreeNode.getPosition(treeNode);
            if (position >= 0)
                return offset + position;
            offset += notDoneGroupTreeNode.displayedSize();
        }

        return -1;
    }

    public int getItemViewType(int position) {
        TreeNode treeNode = getNode(position);
        Assert.assertTrue(treeNode != null);

        return treeNode.getItemViewType();
    }

    public void setNodes(List<TreeNode> rootTreeNodes) {
        Assert.assertTrue(rootTreeNodes != null);

        mNotDoneGroupTreeNodes = rootTreeNodes;

        Collections.sort(mNotDoneGroupTreeNodes);
    }

    TreeViewAdapter getTreeViewAdapter() {
        TreeViewAdapter treeViewAdapter = mTreeViewAdapterReference.get();
        Assert.assertTrue(treeViewAdapter != null);

        return treeViewAdapter;
    }

    public int displayedSize() {
        int displayedSize = 0;
        for (TreeNode notDoneGroupTreeNode : mNotDoneGroupTreeNodes)
            displayedSize += notDoneGroupTreeNode.displayedSize();
        return displayedSize;
    }

    public List<TreeNode> getSelectedNodes() {
        return Stream.of(mNotDoneGroupTreeNodes)
                .flatMap(TreeNode::getSelectedNodes)
                .collect(Collectors.toList());
    }

    public void onCreateActionMode() {
        Stream.of(mNotDoneGroupTreeNodes)
                .forEach(TreeNode::onCreateActionMode);
    }

    public void onDestroyActionMode() {
        Stream.of(mNotDoneGroupTreeNodes)
                .forEach(TreeNode::onDestroyActionMode);
    }

    public void unselect() {
        Stream.of(mNotDoneGroupTreeNodes)
                .forEach(TreeNode::unselect);
    }

    public void addNotDoneGroupTreeNode(TreeNode notDoneGroupTreeNode) {
        Assert.assertTrue(notDoneGroupTreeNode != null);

        mNotDoneGroupTreeNodes.add(notDoneGroupTreeNode);

        Collections.sort(mNotDoneGroupTreeNodes);

        TreeViewAdapter treeViewAdapter = getTreeViewAdapter();
        Assert.assertTrue(treeViewAdapter != null);

        treeViewAdapter.notifyItemInserted(getPosition(notDoneGroupTreeNode));
    }

    public void remove(TreeNode notDoneGroupTreeNode) {
        Assert.assertTrue(notDoneGroupTreeNode != null);
        Assert.assertTrue(mNotDoneGroupTreeNodes.contains(notDoneGroupTreeNode));

        TreeViewAdapter treeViewAdapter = getTreeViewAdapter();
        Assert.assertTrue(treeViewAdapter != null);

        int oldPosition = getPosition(notDoneGroupTreeNode);

        mNotDoneGroupTreeNodes.remove(notDoneGroupTreeNode);

        treeViewAdapter.notifyItemRemoved(oldPosition);

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

    @Override
    public List<TreeNode> getSelectedChildren() {
        return getSelectedNodes();
    }

    @Override
    public TreeNodeCollection getTreeNodeCollection() {
        return this;
    }
}
