package com.krystianwsul.checkme.gui.instances.tree;

import java.util.List;

public interface NodeContainer {
    int displayedSize();

    TreeNode getNode(int position);

    int getPosition(TreeNode treeNode);
    boolean expanded();

    void update();

    List<TreeNode> getSelectedChildren();

    TreeNodeCollection getTreeNodeCollection();
}
