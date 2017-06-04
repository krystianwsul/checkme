package com.krystianwsul.treeadapter;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;

public interface TreeModelAdapter {
    RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType);

    void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position);

    boolean hasActionMode();

    void incrementSelected();

    void decrementSelected();
}
