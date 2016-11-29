package com.krystianwsul.checkme.gui.projects;


import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.krystianwsul.checkme.R;

public class ProjectListFragment extends Fragment {
    @NonNull
    public static ProjectListFragment newInstance() {
        return new ProjectListFragment();
    }

    public ProjectListFragment() {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_project_list, container, false);
    }
}
