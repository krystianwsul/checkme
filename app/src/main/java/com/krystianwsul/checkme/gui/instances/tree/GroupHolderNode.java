package com.krystianwsul.checkme.gui.instances.tree;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.krystianwsul.treeadapter.ModelNode;

import kotlin.Pair;
import kotlin.Triple;

public abstract class GroupHolderNode implements ModelNode {
    final float mDensity;
    final int mIndentation;

    GroupHolderNode(float density, int indentation) {
        mDensity = density;
        mIndentation = indentation;
    }

    @Nullable
    Triple<String, Integer, Boolean> getName() {
        return null;
    }

    @Nullable
    Pair<String, Integer> getDetails() {
        return null;
    }

    @Nullable
    Pair<String, Integer> getChildren() {
        return null;
    }

    @Nullable
    Pair<Integer, View.OnClickListener> getExpand() {
        return null;
    }

    abstract int getCheckBoxVisibility();

    abstract boolean getCheckBoxChecked();

    @NonNull
    abstract View.OnClickListener getCheckBoxOnClickListener();

    abstract int getSeparatorVisibility();

    abstract int getBackgroundColor();

    @Nullable
    abstract View.OnLongClickListener getOnLongClickListener(RecyclerView.ViewHolder viewHolder);

    @Nullable
    abstract View.OnClickListener getOnClickListener();

    @SuppressWarnings("unused")
    public final void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder) {
        final GroupListFragment.GroupAdapter.GroupHolder groupHolder = (GroupListFragment.GroupAdapter.GroupHolder) viewHolder;

        int padding = 48 * mIndentation;

        groupHolder.getMGroupRowContainer().setPadding((int) (padding * mDensity + 0.5f), 0, 0, 0);

        Triple<String, Integer, Boolean> name = getName();
        if (name != null) {
            groupHolder.getMGroupRowName().setVisibility(View.VISIBLE);
            groupHolder.getMGroupRowName().setText(name.getFirst());
            groupHolder.getMGroupRowName().setTextColor(name.getSecond());
            groupHolder.getMGroupRowName().setSingleLine(name.getThird());
        } else {
            groupHolder.getMGroupRowName().setVisibility(View.INVISIBLE);
        }

        Pair<String, Integer> details = getDetails();
        if (details != null) {
            groupHolder.getMGroupRowDetails().setVisibility(View.VISIBLE);
            groupHolder.getMGroupRowDetails().setText(details.getFirst());
            groupHolder.getMGroupRowDetails().setTextColor(details.getSecond());
        } else {
            groupHolder.getMGroupRowDetails().setVisibility(View.GONE);
        }

        Pair<String, Integer> children = getChildren();
        if (children != null) {
            groupHolder.getMGroupRowChildren().setVisibility(View.VISIBLE);
            groupHolder.getMGroupRowChildren().setText(children.getFirst());
            groupHolder.getMGroupRowChildren().setTextColor(children.getSecond());
        } else {
            groupHolder.getMGroupRowChildren().setVisibility(View.GONE);
        }

        Pair<Integer, View.OnClickListener> expand = getExpand();
        if (expand != null) {
            groupHolder.getMGroupRowExpand().setVisibility(View.VISIBLE);
            groupHolder.getMGroupRowExpand().setImageResource(expand.getFirst());
            groupHolder.getMGroupRowExpand().setOnClickListener(expand.getSecond());
        } else {
            groupHolder.getMGroupRowExpand().setVisibility(View.INVISIBLE);
        }

        int checkBoxVisibility = getCheckBoxVisibility();
        //noinspection ResourceType
        groupHolder.getMGroupRowCheckBox().setVisibility(checkBoxVisibility);
        if (checkBoxVisibility == View.VISIBLE) {
            groupHolder.getMGroupRowCheckBox().setChecked(getCheckBoxChecked());
            groupHolder.getMGroupRowCheckBox().setOnClickListener(getCheckBoxOnClickListener());
        }

        //noinspection ResourceType
        groupHolder.getMGroupRowSeparator().setVisibility(getSeparatorVisibility());

        groupHolder.getMGroupRow().setBackgroundColor(getBackgroundColor());

        groupHolder.getMGroupRow().setOnLongClickListener(getOnLongClickListener(viewHolder));

        groupHolder.getMGroupRow().setOnClickListener(getOnClickListener());
    }

    @SuppressWarnings("unused")
    public final int getItemViewType() {
        return GroupListFragment.GroupAdapter.Companion.getTYPE_GROUP();
    }
}
