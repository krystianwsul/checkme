package com.krystianwsul.checkme.gui.tree;

import android.support.v7.widget.RecyclerView;

public interface ModelNode extends Comparable<ModelNode> {
    void onBindViewHolder(RecyclerView.ViewHolder viewHolder);

    int getItemViewType();

    boolean selectable();

    void onClick();

    boolean visibleWhenEmpty();

    boolean visibleDuringActionMode();
}
