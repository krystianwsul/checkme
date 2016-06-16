package com.krystianwsul.checkme.gui.instances.tree;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.krystianwsul.checkme.utils.time.TimeStamp;

import junit.framework.Assert;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NotDoneGroupTreeCollection {
    private List<NotDoneGroupTreeNode> mNotDoneGroupTreeNodes;

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
                .forEach(NotDoneGroupTreeNode::unselect);
    }

    public List<Node> getSelectedNodes() {
        return Stream.of(mNotDoneGroupTreeNodes)
                .flatMap(NotDoneGroupTreeNode::getSelectedNodes)
                .collect(Collectors.toList());
    }

    public int remove(NotDoneGroupTreeNode notDoneGroupTreeNode) {
        Assert.assertTrue(notDoneGroupTreeNode != null);
        Assert.assertTrue(mNotDoneGroupTreeNodes.contains(notDoneGroupTreeNode));

        TreeNodeCollection treeNodeCollection = getTreeNodeCollection();
        Assert.assertTrue(treeNodeCollection != null);

        TreeViewAdapter treeViewAdapter = treeNodeCollection.getTreeViewAdapter();
        Assert.assertTrue(treeViewAdapter != null);

        int oldPosition = treeNodeCollection.getPosition(notDoneGroupTreeNode);

        mNotDoneGroupTreeNodes.remove(notDoneGroupTreeNode);

        treeViewAdapter.notifyItemRemoved(oldPosition);

        return oldPosition;
    }

    public int displayedSize() {
        int displayedSize = 0;
        for (NotDoneGroupTreeNode notDoneGroupTreeNode : mNotDoneGroupTreeNodes)
            displayedSize += notDoneGroupTreeNode.displayedSize();
        return displayedSize;
    }

    public void updateCheckBoxes() {
        Stream.of(mNotDoneGroupTreeNodes)
                .forEach(NotDoneGroupTreeNode::updateCheckBoxes);
    }

    public Node getNode(int position) {
        Assert.assertTrue(position >= 0);
        Assert.assertTrue(position < displayedSize());

        for (NotDoneGroupTreeNode notDoneGroupTreeNode : mNotDoneGroupTreeNodes) {
            if (position < notDoneGroupTreeNode.displayedSize())
                return notDoneGroupTreeNode.getNode(position);

            position = position - notDoneGroupTreeNode.displayedSize();
        }

        throw new IndexOutOfBoundsException();
    }

    public int getPosition(Node node) {
        int offset = 0;
        for (NotDoneGroupTreeNode notDoneGroupTreeNode : mNotDoneGroupTreeNodes) {
            int position = notDoneGroupTreeNode.getPosition(node);
            if (position >= 0)
                return offset + position;
            offset += notDoneGroupTreeNode.displayedSize();
        }

        return -1;
    }

    public ArrayList<TimeStamp> getExpandedGroups() {
        return Stream.of(mNotDoneGroupTreeNodes)
                .filter(NotDoneGroupTreeNode::expanded)
                .map(notDoneGroupTreeNode -> notDoneGroupTreeNode.getNotDoneGroupModelNode().getExactTimeStamp().toTimeStamp())
                .collect(Collectors.toCollection(ArrayList::new));
    }

    public void setNotDoneGroupTreeNodes(List<NotDoneGroupTreeNode> notDoneGroupTreeNodes) {
        Assert.assertTrue(notDoneGroupTreeNodes != null);

        mNotDoneGroupTreeNodes = notDoneGroupTreeNodes;

        sort();
    }

    private void sort() {
        Collections.sort(mNotDoneGroupTreeNodes);
    }

    public void addNotDoneGroupTreeNode(NotDoneGroupTreeNode notDoneGroupTreeNode) {
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
        updateCheckBoxes();
    }

    public void onDestroyActionMode() {
        updateCheckBoxes();
    }

    public NotDoneGroupModelCollection getNotDoneGroupModelCollection() {
        return mNotDoneGroupModelCollection;
    }
}
