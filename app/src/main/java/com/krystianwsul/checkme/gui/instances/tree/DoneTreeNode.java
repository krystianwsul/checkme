package com.krystianwsul.checkme.gui.instances.tree;

import junit.framework.Assert;

import java.lang.ref.WeakReference;
import java.util.List;

public class DoneTreeNode extends ChildTreeNode {
    private final WeakReference<DividerTreeNode> mDividerTreeNodeReference;

    public DoneTreeNode(ChildModelNode childModelNode, WeakReference<DividerTreeNode> dividerTreeNodeReference) {
        super(childModelNode, null);

        Assert.assertTrue(dividerTreeNodeReference != null);

        mDividerTreeNodeReference = dividerTreeNodeReference;
    }

    @Override
    public void update() {
        throw new UnsupportedOperationException();
    }

    private DividerTreeNode getDividerTreeNode() {
        DividerTreeNode dividerTreeNode = mDividerTreeNodeReference.get();
        Assert.assertTrue(dividerTreeNode != null);

        return dividerTreeNode;
    }

    @Override
    protected TreeNodeCollection getTreeNodeCollection() {
        DividerTreeNode dividerTreeNode = getDividerTreeNode();
        Assert.assertTrue(dividerTreeNode != null);

        TreeNodeCollection treeNodeCollection = dividerTreeNode.getTreeNodeCollection();
        Assert.assertTrue(treeNodeCollection != null);

        return treeNodeCollection;
    }

    @Override
    protected Node getParent() {
        return getDividerTreeNode();
    }

    @Override
    public List<Node> getSelectedChildren() {
        throw new UnsupportedOperationException();
    }
}
