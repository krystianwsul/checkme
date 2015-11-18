package com.example.krystianwsul.organizatortest.arrayadapters;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TextView;

import com.example.krystianwsul.organizatortest.R;
import com.example.krystianwsul.organizatortest.ShowGroupActivity;
import com.example.krystianwsul.organizatortest.ShowInstanceActivity;
import com.example.krystianwsul.organizatortest.domainmodel.groups.Group;
import com.example.krystianwsul.organizatortest.domainmodel.instances.Instance;

import junit.framework.Assert;

import java.util.ArrayList;

/**
 * Created by Krystian on 10/23/2015.
 */
public class GroupAdapter extends RecyclerView.Adapter<GroupAdapter.GroupHolder> {
    private final Context mContext;
    private final ArrayList<Group> mGroups;

    public GroupAdapter(Context context, ArrayList<Group> groups) {
        Assert.assertTrue(context != null);
        Assert.assertTrue(groups != null);
        Assert.assertTrue(!groups.isEmpty());

        mContext = context;
        mGroups = groups;
    }

    @Override
    public GroupHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        TableLayout groupRow = (TableLayout) LayoutInflater.from(parent.getContext()).inflate(R.layout.show_group_row, parent, false);

        TextView groupRowName = (TextView) groupRow.findViewById(R.id.group_row_name);
        TextView groupRowDetails = (TextView) groupRow.findViewById(R.id.group_row_details);
        ImageView groupRowImg = (ImageView) groupRow.findViewById(R.id.group_row_img);
        CheckBox groupCheckBox = (CheckBox) groupRow.findViewById(R.id.group_row_checkbox);

        GroupHolder groupHolder = new GroupHolder(groupRow, groupRowName, groupRowDetails, groupRowImg, groupCheckBox);
        return groupHolder;
    }

    @Override
    public void onBindViewHolder(GroupHolder groupHolder, int position) {
        final Group group = mGroups.get(position);

        groupHolder.mGroupRowName.setText(group.getNameText(mContext));

        groupHolder.mGroupRowDetails.setText(group.getDetailsText(mContext));

        Resources resources = mContext.getResources();

        if (group.singleInstance() && group.getSingleSinstance().getChildInstances().isEmpty())
            groupHolder.mGroupRowImg.setBackground(resources.getDrawable(R.drawable.ic_label_outline_black_18dp));
        else
            groupHolder.mGroupRowImg.setBackground(resources.getDrawable(R.drawable.ic_list_black_18dp));

        if (group.singleInstance()) {
            groupHolder.mGroupRowCheckBox.setVisibility(View.VISIBLE);

            groupHolder.mGroupRowCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    Assert.assertTrue(group.singleInstance());
                    group.getSingleSinstance().setDone(isChecked);
                }
            });

            groupHolder.mGroupRowCheckBox.setChecked(group.getSingleSinstance().getDone() != null);
        } else {
            groupHolder.mGroupRowCheckBox.setVisibility(View.INVISIBLE);
        }

        groupHolder.mGroupRow.setOnClickListener(new TableLayout.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = getIntent(group, view.getContext());
                mContext.startActivity(intent);
            }

            private Intent getIntent(Group group, Context context) {
                if (group.singleInstance()) {
                    Instance instance = group.getSingleSinstance();
                    return ShowInstanceActivity.getIntent(instance, context);
                } else {
                    return ShowGroupActivity.getIntent(group, context);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return mGroups.size();
    }

    public static class GroupHolder extends RecyclerView.ViewHolder {
        public final TableLayout mGroupRow;
        public final TextView mGroupRowName;
        public final TextView mGroupRowDetails;
        public final ImageView mGroupRowImg;
        public final CheckBox mGroupRowCheckBox;

        public GroupHolder(TableLayout groupRow, TextView groupRowName, TextView groupRowDetails, ImageView groupRowImg, CheckBox groupRowCheckBox) {
            super(groupRow);

            Assert.assertTrue(groupRow != null);
            Assert.assertTrue(groupRowName != null);
            Assert.assertTrue(groupRowDetails != null);
            Assert.assertTrue(groupRowImg != null);
            Assert.assertTrue(groupRowCheckBox != null);

            mGroupRow = groupRow;
            mGroupRowName = groupRowName;
            mGroupRowDetails = groupRowDetails;
            mGroupRowImg = groupRowImg;
            mGroupRowCheckBox = groupRowCheckBox;
        }
    }
}
