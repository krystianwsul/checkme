package com.example.krystianwsul.organizatortest.gui.tasks;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.krystianwsul.organizatortest.R;
import com.example.krystianwsul.organizatortest.domainmodel.dates.TimeStamp;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.TaskFactory;
import com.example.krystianwsul.organizatortest.gui.MainActivity;

import junit.framework.Assert;

public class TaskListFragment extends Fragment implements MainActivity.RefreshFragment {
    private RecyclerView mTasksRecycler;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.task_list_fragment, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        View view = getView();
        Assert.assertTrue(view != null);

        mTasksRecycler = (RecyclerView) view.findViewById(R.id.tasks_recycler);
        mTasksRecycler.setLayoutManager(new LinearLayoutManager(getContext()));

        FloatingActionButton floatingActionButton = (FloatingActionButton) getView().findViewById(R.id.task_list_fragment_fab);
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(CreateRootTaskActivity.getCreateIntent(getContext()));
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        refresh();
    }

    @Override
    public void refresh() {
        mTasksRecycler.setAdapter(new TaskAdapter(getActivity(), TaskFactory.getInstance().getRootTasks(TimeStamp.getNow())));
    }
}
