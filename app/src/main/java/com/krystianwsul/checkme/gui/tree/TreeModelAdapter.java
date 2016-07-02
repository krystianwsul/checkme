package com.krystianwsul.checkme.gui.tree;

import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;

public interface TreeModelAdapter {
    RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType);

    void onBindViewHolder(RecyclerView.ViewHolder holder, int position);

    boolean hasActionMode();

    void incrementSelected();

    void decrementSelected();
}
