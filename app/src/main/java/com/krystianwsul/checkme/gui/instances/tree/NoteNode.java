package com.krystianwsul.checkme.gui.instances.tree;

import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.view.View;

import com.krystianwsul.checkme.R;
import com.krystianwsul.treeadapter.ModelNode;
import com.krystianwsul.treeadapter.NodeContainer;
import com.krystianwsul.treeadapter.TreeNode;

import junit.framework.Assert;

import java.util.ArrayList;

class NoteNode extends GroupHolderNode implements ModelNode {
    @NonNull
    private final GroupListFragment.GroupAdapter mGroupAdapter;

    private final String mNote;

    private TreeNode mTreeNode;

    NoteNode(float density, @NonNull String note, @NonNull GroupListFragment.GroupAdapter groupAdapter) {
        super(density, 0);

        Assert.assertTrue(!TextUtils.isEmpty(note));

        mNote = note;
        mGroupAdapter = groupAdapter;
    }

    @NonNull
    TreeNode initialize(@NonNull NodeContainer nodeContainer) {
        mTreeNode = new TreeNode(this, nodeContainer, false, false);

        mTreeNode.setChildTreeNodes(new ArrayList<>());
        return mTreeNode;
    }

    @NonNull
    private TreeNode getTreeNode() {
        Assert.assertTrue(mTreeNode != null);

        return mTreeNode;
    }

    @NonNull
    private GroupListFragment getGroupListFragment() {
        return mGroupAdapter.mGroupListFragment;
    }

    @Override
    int getNameVisibility() {
        return View.VISIBLE;
    }

    @NonNull
    @Override
    String getName() {
        return mNote;
    }

    @Override
    int getNameColor() {
        return ContextCompat.getColor(getGroupListFragment().getActivity(), R.color.textPrimary);
    }

    @Override
    boolean getNameSingleLine() {
        return false;
    }

    @Override
    int getDetailsVisibility() {
        return View.GONE;
    }

    @NonNull
    @Override
    String getDetails() {
        throw new UnsupportedOperationException();
    }

    @Override
    int getDetailsColor() {
        throw new UnsupportedOperationException();
    }

    @Override
    int getChildrenVisibility() {
        return View.GONE;
    }

    @NonNull
    @Override
    String getChildren() {
        throw new UnsupportedOperationException();
    }

    @Override
    int getChildrenColor() {
        throw new UnsupportedOperationException();
    }

    @Override
    int getExpandVisibility() {
        return View.GONE;
    }

    @Override
    int getExpandImageResource() {
        throw new UnsupportedOperationException();
    }

    @NonNull
    @Override
    View.OnClickListener getExpandOnClickListener() {
        throw new UnsupportedOperationException();
    }

    @Override
    int getCheckBoxVisibility() {
        return View.GONE;
    }

    @Override
    boolean getCheckBoxChecked() {
        throw new UnsupportedOperationException();
    }

    @NonNull
    @Override
    View.OnClickListener getCheckBoxOnClickListener() {
        throw new UnsupportedOperationException();
    }

    @Override
    int getSeparatorVisibility() {
        return (getTreeNode().getSeparatorVisibility() ? View.VISIBLE : View.INVISIBLE);
    }

    @Override
    int getBackgroundColor() {
        return Color.TRANSPARENT;
    }

    @Override
    View.OnLongClickListener getOnLongClickListener() {
        return null;
    }

    @Override
    View.OnClickListener getOnClickListener() {
        return null;
    }

    @Override
    public boolean selectable() {
        return false;
    }

    @Override
    public void onClick() {

    }

    @Override
    public boolean visibleWhenEmpty() {
        return true;
    }

    @Override
    public boolean visibleDuringActionMode() {
        return false;
    }

    @Override
    public boolean separatorVisibleWhenNotExpanded() {
        return true;
    }

    @Override
    public int compareTo(@NonNull ModelNode o) {
        Assert.assertTrue(o instanceof NotDoneGroupNode || o instanceof UnscheduledNode || o instanceof DividerNode);

        return -1;
    }
}
