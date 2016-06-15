package com.krystianwsul.checkme.gui.instances.tree;

import android.view.ViewGroup;

import com.krystianwsul.checkme.gui.SelectionCallback;
import com.krystianwsul.checkme.gui.instances.GroupListFragment;

public interface TreeModelAdapter {
    GroupListFragment.GroupAdapter.AbstractHolder onCreateViewHolder(ViewGroup parent, int viewType);
    void onBindViewHolder(GroupListFragment.GroupAdapter.AbstractHolder holder, int position);
    SelectionCallback getSelectionCallback();
}
