package com.krystianwsul.treeadapter;

import android.support.annotation.NonNull;

import java.util.List;

public interface NodeContainer {
    int displayedSize();

    int getPosition(@NonNull TreeNode treeNode);

    boolean isExpanded();

    void update();

    void updateRecursive();

    @NonNull
    List<TreeNode> getSelectedChildren();

    @NonNull
    TreeNodeCollection getTreeNodeCollection();

    void remove(@NonNull TreeNode treeNode);

    void add(@NonNull TreeNode treeNode);

    int getIndentation();
}
