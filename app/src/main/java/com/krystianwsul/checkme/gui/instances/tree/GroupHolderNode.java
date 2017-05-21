package com.krystianwsul.checkme.gui.instances.tree;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.View;

abstract class GroupHolderNode {
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

    abstract int getDetailsVisibility();

    @NonNull
    abstract String getDetails();

    abstract int getDetailsColor();

    abstract int getChildrenVisibility();

    @NonNull
    abstract String getChildren();

    abstract int getChildrenColor();

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
    abstract View.OnLongClickListener getOnLongClickListener();

    @Nullable
    abstract View.OnClickListener getOnClickListener();

    @SuppressWarnings("unused")
    public final void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder) {
        final GroupListFragment.GroupAdapter.GroupHolder groupHolder = (GroupListFragment.GroupAdapter.GroupHolder) viewHolder;

        int padding = 48 * mIndentation;

        groupHolder.mGroupRowContainer.setPadding((int) (padding * mDensity + 0.5f), 0, 0, 0);

        int nameVisibility = getNameVisibility();
        //noinspection ResourceType
        groupHolder.mGroupRowName.setVisibility(nameVisibility);
        if (nameVisibility == View.VISIBLE) {
            groupHolder.mGroupRowName.setText(getName());
            groupHolder.mGroupRowName.setTextColor(getNameColor());
            groupHolder.mGroupRowName.setSingleLine(getNameSingleLine());
        }

        int detailsVisibility = getDetailsVisibility();
        //noinspection ResourceType
        groupHolder.mGroupRowDetails.setVisibility(detailsVisibility);
        if (detailsVisibility == View.VISIBLE) {
            groupHolder.mGroupRowDetails.setText(getDetails());
            groupHolder.mGroupRowDetails.setTextColor(getDetailsColor());
        }

        int childrenVisibility = getChildrenVisibility();
        //noinspection ResourceType
        groupHolder.mGroupRowChildren.setVisibility(childrenVisibility);
        if (childrenVisibility == View.VISIBLE) {
            groupHolder.mGroupRowChildren.setText(getChildren());
            groupHolder.mGroupRowChildren.setTextColor(getChildrenColor());
        }

        int expandVisibility = getExpandVisibility();
        //noinspection ResourceType
        groupHolder.mGroupRowExpand.setVisibility(expandVisibility);
        if (expandVisibility == View.VISIBLE) {
            groupHolder.mGroupRowExpand.setImageResource(getExpandImageResource());
            groupHolder.mGroupRowExpand.setOnClickListener(getExpandOnClickListener());
        }

        int checkBoxVisibility = getCheckBoxVisibility();
        //noinspection ResourceType
        groupHolder.mGroupRowCheckBox.setVisibility(checkBoxVisibility);
        if (checkBoxVisibility == View.VISIBLE) {
            groupHolder.mGroupRowCheckBox.setChecked(getCheckBoxChecked());
            groupHolder.mGroupRowCheckBox.setOnClickListener(getCheckBoxOnClickListener());
        }

        //noinspection ResourceType
        groupHolder.mGroupRowSeparator.setVisibility(getSeparatorVisibility());

        groupHolder.mGroupRow.setBackgroundColor(getBackgroundColor());

        groupHolder.mGroupRow.setOnLongClickListener(getOnLongClickListener());

        groupHolder.mGroupRow.setOnClickListener(getOnClickListener());
    }

    @SuppressWarnings("unused")
    public final int getItemViewType() {
        return GroupListFragment.GroupAdapter.TYPE_GROUP;
    }
}
