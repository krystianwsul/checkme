package com.example.krystianwsul.organizatortest.arrayadapters;

import android.content.Context;
import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.krystianwsul.organizatortest.R;
import com.example.krystianwsul.organizatortest.domainmodel.groups.Group;

import junit.framework.Assert;

import java.util.ArrayList;

/**
 * Created by Krystian on 10/23/2015.
 */
public class GroupAdapter extends ArrayAdapter<Group> {
    private final Context mContext;
    private final ArrayList<Group> mGroups;

    public GroupAdapter(Context context, ArrayList<Group> groups) {
        super(context, -1, groups);

        Assert.assertTrue(context != null);
        Assert.assertTrue(groups != null);
        Assert.assertTrue(!groups.isEmpty());

        mContext = context;
        mGroups = groups;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(mContext);
            convertView = inflater.inflate(R.layout.show_task_row, parent, false);

            GroupHolder groupHolder = new GroupHolder();
            groupHolder.taskRowName = (TextView) convertView.findViewById(R.id.task_row_name);
            groupHolder.taskRowDetails = (TextView) convertView.findViewById(R.id.task_row_details);
            groupHolder.taskRowImg = (ImageView) convertView.findViewById(R.id.task_row_img);
            convertView.setTag(groupHolder);
        }

        GroupHolder groupHolder = (GroupHolder) convertView.getTag();

        Group group = mGroups.get(position);

        groupHolder.taskRowName.setText(group.getNameText(mContext));

        groupHolder.taskRowDetails.setText(group.getDetailsText(mContext));

        Resources resources = mContext.getResources();

        if (group.singleInstance() && group.getSingleSinstance().getChildInstances().isEmpty())
            groupHolder.taskRowImg.setBackground(resources.getDrawable(R.drawable.ic_label_outline_black_18dp));
        else
            groupHolder.taskRowImg.setBackground(resources.getDrawable(R.drawable.ic_list_black_18dp));

        return convertView;
    }

    private class GroupHolder {
        public TextView taskRowName;
        public TextView taskRowDetails;
        public ImageView taskRowImg;
    }
}
