package com.krystianwsul.checkme.gui.tree;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;

public interface ModelNode extends Comparable<ModelNode> {
    void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder);

    int getItemViewType();

    boolean selectable();

    void onClick();

    boolean visibleWhenEmpty();

    boolean visibleDuringActionMode();

    boolean separatorVisibleWhenNotExapanded();
}
