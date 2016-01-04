package com.example.krystianwsul.organizator.gui.tasks;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.krystianwsul.organizator.R;
import com.example.krystianwsul.organizator.domainmodel.dates.TimeStamp;
import com.example.krystianwsul.organizator.domainmodel.tasks.Task;
import com.example.krystianwsul.organizator.domainmodel.tasks.TaskFactory;
import com.example.krystianwsul.organizator.gui.MainActivity;

import junit.framework.Assert;

import java.util.ArrayList;

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

    public void setEditing(boolean editing) {
        TaskAdapter taskAdapter = (TaskAdapter) mTasksRecycler.getAdapter();
        Assert.assertTrue(taskAdapter != null);

        taskAdapter.setEditing(editing);
    }

    public ArrayList<Task> getSelected() {
        TaskAdapter taskAdapter = (TaskAdapter) mTasksRecycler.getAdapter();
        Assert.assertTrue(taskAdapter != null);

        return taskAdapter.getSelected();
    }
}
