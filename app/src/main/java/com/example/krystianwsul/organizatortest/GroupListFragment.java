package com.example.krystianwsul.organizatortest;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.example.krystianwsul.organizatortest.arrayadapters.GroupAdapter;
import com.example.krystianwsul.organizatortest.domainmodel.groups.Group;
import com.example.krystianwsul.organizatortest.domainmodel.groups.GroupFactory;
import com.example.krystianwsul.organizatortest.domainmodel.instances.Instance;

import java.util.ArrayList;

/**
 * Created by Krystian on 10/31/2015.
 */
public class GroupListFragment extends Fragment {
    private RecyclerView mGroupList;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.group_list_fragment, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mGroupList = (RecyclerView) getView().findViewById(R.id.groups_list);
        mGroupList.setLayoutManager(new LinearLayoutManager(getContext()));
    }

    @Override
    public void onStart() {
        super.onStart();

        GroupFactory.refresh();

        ArrayList<Group> groupArray = new ArrayList<>(GroupFactory.getInstance().getGroups());
        mGroupList.setAdapter(new GroupAdapter(getContext(), groupArray));
    }
}