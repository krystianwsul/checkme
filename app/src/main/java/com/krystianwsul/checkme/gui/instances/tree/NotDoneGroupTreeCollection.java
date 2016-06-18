package com.krystianwsul.checkme.gui.instances.tree;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;

import junit.framework.Assert;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.List;

public class NotDoneGroupTreeCollection {
    private List<RootTreeNode> mNotDoneGroupTreeNodes;

    private final NotDoneGroupModelCollection mNotDoneGroupModelCollection;

    private final WeakReference<TreeNodeCollection> mTreeNodeCollectionReference;

    public NotDoneGroupTreeCollection(NotDoneGroupModelCollection notDoneGroupModelCollection, WeakReference<TreeNodeCollection> treeNodeCollectionReference) {
        Assert.assertTrue(notDoneGroupModelCollection != null);
        Assert.assertTrue(treeNodeCollectionReference != null);

        mNotDoneGroupModelCollection = notDoneGroupModelCollection;
        mTreeNodeCollectionReference = treeNodeCollectionReference;
    }

    public void unselect() {
        Stream.of(mNotDoneGroupTreeNodes)
                .forEach(RootTreeNode::unselect);
    }

    public List<Node> getSelectedNodes() {
        return Stream.of(mNotDoneGroupTreeNodes)
                .flatMap(RootTreeNode::getSelectedNodes)
                .collect(Collectors.toList());
    }

    public int remove(RootTreeNode notDoneGroupTreeNode) {
        Assert.assertTrue(notDoneGroupTreeNode != null);
        Assert.assertTrue(mNotDoneGroupTreeNodes.contains(notDoneGroupTreeNode));

        TreeNodeCollection treeNodeCollection = getTreeNodeCollection();
        Assert.assertTrue(treeNodeCollection != null);

        TreeViewAdapter treeViewAdapter = treeNodeCollection.getTreeViewAdapter();
        Assert.assertTrue(treeViewAdapter != null);

        int oldPosition = treeNodeCollection.getPosition(notDoneGroupTreeNode);

        mNotDoneGroupTreeNodes.remove(notDoneGroupTreeNode);

        treeViewAdapter.notifyItemRemoved(oldPosition);

        if (oldPosition > 0)
            treeViewAdapter.notifyItemChanged(oldPosition - 1);

        return oldPosition;
    }

    public int displayedSize() {
        int displayedSize = 0;
        for (RootTreeNode notDoneGroupTreeNode : mNotDoneGroupTreeNodes)
            displayedSize += notDoneGroupTreeNode.displayedSize();
        return displayedSize;
    }

    public Node getNode(int position) {
        Assert.assertTrue(position >= 0);
        Assert.assertTrue(position < displayedSize());

        for (RootTreeNode notDoneGroupTreeNode : mNotDoneGroupTreeNodes) {
            if (position < notDoneGroupTreeNode.displayedSize())
                return notDoneGroupTreeNode.getNode(position);

            position = position - notDoneGroupTreeNode.displayedSize();
        }

        throw new IndexOutOfBoundsException();
    }

    public int getPosition(Node node) {
        int offset = 0;
        for (RootTreeNode notDoneGroupTreeNode : mNotDoneGroupTreeNodes) {
            int position = notDoneGroupTreeNode.getPosition(node);
            if (position >= 0)
                return offset + position;
            offset += notDoneGroupTreeNode.displayedSize();
        }

        return -1;
    }

    public void setNotDoneGroupTreeNodes(List<RootTreeNode> notDoneGroupTreeNodes) {
        Assert.assertTrue(notDoneGroupTreeNodes != null);

        mNotDoneGroupTreeNodes = notDoneGroupTreeNodes;

        sort();
    }

    private void sort() {
        Collections.sort(mNotDoneGroupTreeNodes);
    }

    public void addNotDoneGroupTreeNode(RootTreeNode notDoneGroupTreeNode) {
        Assert.assertTrue(notDoneGroupTreeNode != null);

        mNotDoneGroupTreeNodes.add(notDoneGroupTreeNode);

        sort();

        TreeNodeCollection treeNodeCollection = getTreeNodeCollection();
        Assert.assertTrue(treeNodeCollection != null);

        TreeViewAdapter treeViewAdapter = treeNodeCollection.getTreeViewAdapter();
        Assert.assertTrue(treeViewAdapter != null);

        treeViewAdapter.notifyItemInserted(treeNodeCollection.getPosition(notDoneGroupTreeNode));
    }

    TreeNodeCollection getTreeNodeCollection() {
        TreeNodeCollection treeNodeCollection = mTreeNodeCollectionReference.get();
        Assert.assertTrue(treeNodeCollection != null);

        return treeNodeCollection;
    }

    TreeViewAdapter getTreeViewAdapter() {
        TreeNodeCollection treeNodeCollection = getTreeNodeCollection();
        Assert.assertTrue(treeNodeCollection != null);

        TreeViewAdapter treeViewAdapter = treeNodeCollection.getTreeViewAdapter();
        Assert.assertTrue(treeViewAdapter != null);

        return treeViewAdapter;
    }

    public void onCreateActionMode() {
        Stream.of(mNotDoneGroupTreeNodes)
                .forEach(RootTreeNode::onCreateActionMode);
    }

    public void onDestroyActionMode() {
        Stream.of(mNotDoneGroupTreeNodes)
                .forEach(RootTreeNode::onDestroyActionMode);
    }

    public NotDoneGroupModelCollection getNotDoneGroupModelCollection() {
        return mNotDoneGroupModelCollection;
    }
}
