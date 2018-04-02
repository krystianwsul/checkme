package com.krystianwsul.checkme.gui.instances.tree;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.krystianwsul.treeadapter.ModelNode;

import kotlin.Pair;

public abstract class GroupHolderNode implements ModelNode {
    final float mDensity;
    final int mIndentation;

    GroupHolderNode(float density, int indentation) {
        mDensity = density;
        mIndentation = indentation;
    }

    abstract int getNameVisibility();

    @NonNull
    abstract String getName();

    abstract int getNameColor();

    abstract boolean getNameSingleLine();

    @Nullable
    Pair<String, Integer> getDetails() {
        return null;
    }

    @Nullable
    Pair<String, Integer> getChildren() {
        return null;
    }

    abstract int getExpandVisibility();

    abstract int getExpandImageResource();

    @NonNull
    abstract View.OnClickListener getExpandOnClickListener();

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

        int nameVisibility = getNameVisibility();
        //noinspection ResourceType
        groupHolder.getMGroupRowName().setVisibility(nameVisibility);
        if (nameVisibility == View.VISIBLE) {
            groupHolder.getMGroupRowName().setText(getName());
            groupHolder.getMGroupRowName().setTextColor(getNameColor());
            groupHolder.getMGroupRowName().setSingleLine(getNameSingleLine());
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

        int expandVisibility = getExpandVisibility();
        //noinspection ResourceType
        groupHolder.getMGroupRowExpand().setVisibility(expandVisibility);
        if (expandVisibility == View.VISIBLE) {
            groupHolder.getMGroupRowExpand().setImageResource(getExpandImageResource());
            groupHolder.getMGroupRowExpand().setOnClickListener(getExpandOnClickListener());
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
