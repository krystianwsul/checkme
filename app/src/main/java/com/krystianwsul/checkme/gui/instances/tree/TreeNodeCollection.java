package com.krystianwsul.checkme.gui.instances.tree;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;

import junit.framework.Assert;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.List;

public class TreeNodeCollection {
    private List<RootTreeNode> mNotDoneGroupTreeNodes;

    private final ModelNodeCollection mModelNodeCollection;

    private final WeakReference<TreeViewAdapter> mTreeViewAdapterReference;

    public TreeNodeCollection(ModelNodeCollection modelNodeCollection, WeakReference<TreeViewAdapter> treeViewAdapterReference) {
        Assert.assertTrue(modelNodeCollection != null);
        Assert.assertTrue(treeViewAdapterReference != null);

        mModelNodeCollection = modelNodeCollection;
        mTreeViewAdapterReference = treeViewAdapterReference;
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

    public int getItemViewType(int position) {
        Node node = getNode(position);
        Assert.assertTrue(node != null);

        return node.getItemViewType();
    }

    public void setNodes(List<RootTreeNode> rootTreeNodes) {
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
        for (RootTreeNode notDoneGroupTreeNode : mNotDoneGroupTreeNodes)
            displayedSize += notDoneGroupTreeNode.displayedSize();
        return displayedSize;
    }

    public List<Node> getSelectedNodes() {
        return Stream.of(mNotDoneGroupTreeNodes)
                .flatMap(RootTreeNode::getSelectedNodes)
                .collect(Collectors.toList());
    }

    public void onCreateActionMode() {
        Stream.of(mNotDoneGroupTreeNodes)
                .forEach(RootTreeNode::onCreateActionMode);
    }

    public void onDestroyActionMode() {
        Stream.of(mNotDoneGroupTreeNodes)
                .forEach(RootTreeNode::onDestroyActionMode);
    }

    public void unselect() {
        Stream.of(mNotDoneGroupTreeNodes)
                .forEach(RootTreeNode::unselect);
    }

    public void addNotDoneGroupTreeNode(RootTreeNode notDoneGroupTreeNode) {
        Assert.assertTrue(notDoneGroupTreeNode != null);

        mNotDoneGroupTreeNodes.add(notDoneGroupTreeNode);

        Collections.sort(mNotDoneGroupTreeNodes);

        TreeViewAdapter treeViewAdapter = getTreeViewAdapter();
        Assert.assertTrue(treeViewAdapter != null);

        treeViewAdapter.notifyItemInserted(getPosition(notDoneGroupTreeNode));
    }

    public int remove(RootTreeNode notDoneGroupTreeNode) {
        Assert.assertTrue(notDoneGroupTreeNode != null);
        Assert.assertTrue(mNotDoneGroupTreeNodes.contains(notDoneGroupTreeNode));

        TreeViewAdapter treeViewAdapter = getTreeViewAdapter();
        Assert.assertTrue(treeViewAdapter != null);

        int oldPosition = getPosition(notDoneGroupTreeNode);

        mNotDoneGroupTreeNodes.remove(notDoneGroupTreeNode);

        treeViewAdapter.notifyItemRemoved(oldPosition);

        if (oldPosition > 0)
            treeViewAdapter.notifyItemChanged(oldPosition - 1);

        return oldPosition;
    }
}
