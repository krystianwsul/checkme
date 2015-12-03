package com.example.krystianwsul.organizatortest;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.krystianwsul.organizatortest.arrayadapters.GroupAdapter;

import junit.framework.Assert;

public class GroupListFragment extends Fragment {
    private RecyclerView mGroupList;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.group_list_fragment, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        View view = getView();
        Assert.assertTrue(view != null);
        mGroupList = (RecyclerView) view.findViewById(R.id.groups_list);
        mGroupList.setLayoutManager(new LinearLayoutManager(getContext()));
    }

    @Override
    public void onStart() {
        super.onStart();
        mGroupList.setAdapter(new GroupAdapter(getContext()));
    }
}