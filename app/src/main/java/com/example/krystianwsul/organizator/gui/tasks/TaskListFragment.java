package com.example.krystianwsul.organizator.gui.tasks;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.krystianwsul.organizator.R;
import com.example.krystianwsul.organizator.loaders.TaskListLoader;

import junit.framework.Assert;

import java.util.ArrayList;

public class TaskListFragment extends Fragment implements LoaderManager.LoaderCallbacks<TaskListLoader.Data> {
    private RecyclerView mTasksRecycler;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.task_list_fragment, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        View view = getView();
        Assert.assertTrue(view != null);

        mTasksRecycler = (RecyclerView) view.findViewById(R.id.task_list_fragment_recycler);
        mTasksRecycler.setLayoutManager(new LinearLayoutManager(getContext()));

        FloatingActionButton floatingActionButton = (FloatingActionButton) getView().findViewById(R.id.task_list_fragment_fab);
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(CreateRootTaskActivity.getCreateIntent(getContext()));
            }
        });

        getLoaderManager().initLoader(0, null, this);
    }

    public void uncheck() {
        TaskAdapter taskAdapter = (TaskAdapter) mTasksRecycler.getAdapter();
        Assert.assertTrue(taskAdapter != null);

        taskAdapter.uncheck();
    }

    public ArrayList<Integer> getSelected() {
        TaskAdapter taskAdapter = (TaskAdapter) mTasksRecycler.getAdapter();
        Assert.assertTrue(taskAdapter != null);

        return taskAdapter.getSelected();
    }

    @Override
    public Loader<TaskListLoader.Data> onCreateLoader(int id, Bundle args) {
        return new TaskListLoader(getActivity());
    }

    @Override
    public void onLoadFinished(Loader<TaskListLoader.Data> loader, TaskListLoader.Data data) {
        ArrayList<TaskAdapter.Data> taskDatas = new ArrayList<>();

        for (TaskListLoader.RootTaskData rootTaskData : data.RootTaskDatas)
            taskDatas.add(new TaskAdapter.Data(rootTaskData.TaskId, rootTaskData.Name, rootTaskData.ScheduleText, rootTaskData.HasChildTasks));

        mTasksRecycler.setAdapter(new TaskAdapter(getActivity(), taskDatas, data.DataId, new TaskAdapter.OnCheckedChangedListener() {
            @Override
            public void OnCheckedChanged() {
                ((TaskAdapter.OnCheckedChangedListener) getActivity()).OnCheckedChanged();
            }
        }));
    }

    @Override
    public void onLoaderReset(Loader<TaskListLoader.Data> loader) {
    }

    public void removeSelected() {
        TaskAdapter taskAdapter = (TaskAdapter) mTasksRecycler.getAdapter();
        Assert.assertTrue(taskAdapter != null);

        taskAdapter.removeSelected();
    }
}
