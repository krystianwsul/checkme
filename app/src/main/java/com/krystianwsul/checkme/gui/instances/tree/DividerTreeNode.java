package com.krystianwsul.checkme.gui.instances.tree;

import com.krystianwsul.checkme.gui.SelectionCallback;

import junit.framework.Assert;

import java.lang.ref.WeakReference;

public class DividerTreeNode extends RootTreeNode {
    private final WeakReference<TreeNodeCollection> mTreeNodeCollectionReference;

    public DividerTreeNode(RootModelNode rootModelNode, boolean expanded, WeakReference<TreeNodeCollection> treeNodeCollectionReference) {
        super(rootModelNode, expanded, false);

        Assert.assertTrue(treeNodeCollectionReference != null);

        mTreeNodeCollectionReference = treeNodeCollectionReference;
    }

    @Override
    public TreeNodeCollection getTreeNodeCollection() {
        TreeNodeCollection treeNodeCollection = mTreeNodeCollectionReference.get();
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

    protected SelectionCallback getSelectionCallback() {
        TreeViewAdapter treeViewAdapter = getTreeViewAdapter();
        Assert.assertTrue(treeViewAdapter != null);

        SelectionCallback selectionCallback = treeViewAdapter.getSelectionCallback();
        Assert.assertTrue(selectionCallback != null);

        return selectionCallback;
    }

    @Override
    protected boolean visibleDuringActionMode() {
        return false;
    }

    @Override
    protected boolean visibleWhenEmpty() {
        return false;
    }
}
