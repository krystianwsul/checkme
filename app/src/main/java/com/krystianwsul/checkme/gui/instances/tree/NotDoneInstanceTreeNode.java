package com.krystianwsul.checkme.gui.instances.tree;

import com.krystianwsul.checkme.gui.SelectionCallback;
import com.krystianwsul.checkme.utils.InstanceKey;

import junit.framework.Assert;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class NotDoneInstanceTreeNode extends ChildTreeNode {
    private final WeakReference<NotDoneGroupTreeNode> mNotDoneGroupTreeNodeReference;

    public NotDoneInstanceTreeNode(ChildModelNode childModelNode, ArrayList<InstanceKey> selectedNodes, WeakReference<NotDoneGroupTreeNode> notDoneGroupTreeNodeReference) {
        super(childModelNode, selectedNodes);

        Assert.assertTrue(notDoneGroupTreeNodeReference != null);

        mNotDoneGroupTreeNodeReference = notDoneGroupTreeNodeReference;
    }

    private NotDoneGroupTreeNode getNotDoneGroupTreeNode() {
        NotDoneGroupTreeNode notDoneGroupTreeNode = mNotDoneGroupTreeNodeReference.get();
        Assert.assertTrue(notDoneGroupTreeNode != null);

        return notDoneGroupTreeNode;
    }

    @Override
    protected TreeNodeCollection getTreeNodeCollection() {
        NotDoneGroupTreeNode notDoneGroupTreeNode = getNotDoneGroupTreeNode();
        Assert.assertTrue(notDoneGroupTreeNode != null);

        TreeNodeCollection treeNodeCollection = notDoneGroupTreeNode.getTreeNodeCollection();
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

    public boolean getSeparatorVisibility() {
        NotDoneGroupTreeNode notDoneGroupTreeNode = getNotDoneGroupTreeNode();
        Assert.assertTrue(notDoneGroupTreeNode != null);

        Assert.assertTrue(notDoneGroupTreeNode.expanded());

        TreeNodeCollection treeNodeCollection = notDoneGroupTreeNode.getTreeNodeCollection();
        Assert.assertTrue(treeNodeCollection != null);

        boolean lastInGroup = (notDoneGroupTreeNode.getPosition(this) == notDoneGroupTreeNode.displayedSize() - 1);

        boolean lastInAdapter = (treeNodeCollection.getPosition(this) == treeNodeCollection.displayedSize() - 1);

        return (lastInGroup && !lastInAdapter);
    }

    @Override
    public void update() {
        TreeNodeCollection treeNodeCollection = getTreeNodeCollection();
        Assert.assertTrue(treeNodeCollection != null);

        TreeViewAdapter treeViewAdapter = treeNodeCollection.getTreeViewAdapter();
        Assert.assertTrue(treeViewAdapter != null);

        treeViewAdapter.notifyItemChanged(treeNodeCollection.getPosition(this));
    }

    @Override
    protected Node getParent() {
        return getNotDoneGroupTreeNode();
    }

    @Override
    public List<Node> getSelectedChildren() {
        throw new UnsupportedOperationException();
    }
}
