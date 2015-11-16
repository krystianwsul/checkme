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
            convertView = inflater.inflate(R.layout.show_group_row, parent, false);

            GroupHolder groupHolder = new GroupHolder();
            groupHolder.groupRowName = (TextView) convertView.findViewById(R.id.group_row_name);
            groupHolder.groupRowDetails = (TextView) convertView.findViewById(R.id.group_row_details);
            groupHolder.groupRowImg = (ImageView) convertView.findViewById(R.id.group_row_img);
            convertView.setTag(groupHolder);
        }

        GroupHolder groupHolder = (GroupHolder) convertView.getTag();

        Group group = mGroups.get(position);

        groupHolder.groupRowName.setText(group.getNameText(mContext));

        groupHolder.groupRowDetails.setText(group.getDetailsText(mContext));

        Resources resources = mContext.getResources();

        if (group.singleInstance() && group.getSingleSinstance().getChildInstances().isEmpty())
            groupHolder.groupRowImg.setBackground(resources.getDrawable(R.drawable.ic_label_outline_black_18dp));
        else
            groupHolder.groupRowImg.setBackground(resources.getDrawable(R.drawable.ic_list_black_18dp));

        return convertView;
    }

}
