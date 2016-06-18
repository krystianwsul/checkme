package com.krystianwsul.checkme.gui.instances.tree;

import com.krystianwsul.checkme.gui.SelectionCallback;

import junit.framework.Assert;

import java.lang.ref.WeakReference;

public class NotDoneGroupTreeNode extends RootTreeNode {
    private final WeakReference<NotDoneGroupTreeCollection> mNotDoneGroupTreeCollectionReference;

    public NotDoneGroupTreeNode(RootModelNode rootModelNode, boolean expanded, WeakReference<NotDoneGroupTreeCollection> notDoneGroupTreeCollectionReference, boolean selected) {
        super(rootModelNode, expanded, selected);

        Assert.assertTrue(notDoneGroupTreeCollectionReference != null);

        mNotDoneGroupTreeCollectionReference = notDoneGroupTreeCollectionReference;
    }

    private NotDoneGroupTreeCollection getNotDoneGroupTreeCollection() {
        NotDoneGroupTreeCollection notDoneGroupTreeCollection = mNotDoneGroupTreeCollectionReference.get();
        Assert.assertTrue(notDoneGroupTreeCollection != null);

        return notDoneGroupTreeCollection;
    }

    private TreeViewAdapter getTreeViewAdapter() {
        NotDoneGroupTreeCollection notDoneGroupTreeCollection = getNotDoneGroupTreeCollection();
        Assert.assertTrue(notDoneGroupTreeCollection != null);

        TreeViewAdapter treeViewAdapter = notDoneGroupTreeCollection.getTreeViewAdapter();
        Assert.assertTrue(treeViewAdapter != null);

        return treeViewAdapter;
    }

    @Override
    public TreeNodeCollection getTreeNodeCollection() {
        NotDoneGroupTreeCollection notDoneGroupTreeCollection = getNotDoneGroupTreeCollection();
        Assert.assertTrue(notDoneGroupTreeCollection != null);

        TreeNodeCollection treeNodeCollection = notDoneGroupTreeCollection.getTreeNodeCollection();
        Assert.assertTrue(treeNodeCollection != null);

        return treeNodeCollection;
    }

    protected SelectionCallback getSelectionCallback() {
        TreeViewAdapter treeViewAdapter = getTreeViewAdapter();
        Assert.assertTrue(treeViewAdapter != null);

        return treeViewAdapter.getSelectionCallback();
    }

    @Override
    protected boolean visibleDuringActionMode() {
        return true;
    }

    @Override
    protected boolean visibleWhenEmpty() {
        return true;
    }
}
