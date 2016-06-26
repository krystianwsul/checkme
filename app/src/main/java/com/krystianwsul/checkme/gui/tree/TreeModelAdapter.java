package com.krystianwsul.checkme.gui.tree;

import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;

import com.krystianwsul.checkme.gui.SelectionCallback;

public interface TreeModelAdapter {
    RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType);

    void onBindViewHolder(RecyclerView.ViewHolder holder, int position);
    SelectionCallback getSelectionCallback();
}
