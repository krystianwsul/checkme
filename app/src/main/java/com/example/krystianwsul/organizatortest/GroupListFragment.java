package com.example.krystianwsul.organizatortest;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.example.krystianwsul.organizatortest.adapters.GroupAdapter;
import com.example.krystianwsul.organizatortest.domainmodel.groups.Group;
import com.example.krystianwsul.organizatortest.domainmodel.groups.GroupFactory;
import com.example.krystianwsul.organizatortest.domainmodel.instances.Instance;

import java.util.ArrayList;

/**
 * Created by Krystian on 10/31/2015.
 */
public class GroupListFragment extends Fragment {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.group_list_fragment, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        ListView groupList = (ListView) getView().findViewById(R.id.groups_list);

        ArrayList<Group> groupArray = new ArrayList<>(GroupFactory.getInstance().getGroups());

        groupList.setAdapter(new GroupAdapter(getContext(), groupArray));

        groupList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Group group = (Group) parent.getItemAtPosition(position);
                Intent intent = getIntent(group, view.getContext());
                startActivity(intent);
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
}
