package com.example.krystianwsul.organizator.gui.tasks;


import android.content.Intent;
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
import com.example.krystianwsul.organizator.loaders.ShowTaskFragmentLoader;

import junit.framework.Assert;

import java.util.ArrayList;

public class ShowTaskFragment extends Fragment implements LoaderManager.LoaderCallbacks<ShowTaskFragmentLoader.Data> {
    private static final String INTENT_KEY = "taskId";

    private RecyclerView mFragmentShowTaskRecycler;
    private FloatingActionButton mFragmentShowTaskFab;

    private int mTaskId;

    private ShowTaskFragmentLoader.Data mData;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_show_task, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        View view = getView();
        Assert.assertTrue(view != null);

        mFragmentShowTaskRecycler = (RecyclerView) view.findViewById(R.id.fragment_show_task_recycler);
        mFragmentShowTaskRecycler.setLayoutManager(new LinearLayoutManager(getContext()));

        mFragmentShowTaskFab = (FloatingActionButton) getView().findViewById(R.id.fragment_show_task_fab);

        Intent intent = getActivity().getIntent();
        Assert.assertTrue(intent.hasExtra(INTENT_KEY));
        mTaskId = intent.getIntExtra(INTENT_KEY, -1);
        Assert.assertTrue(mTaskId != -1);

        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public Loader<ShowTaskFragmentLoader.Data> onCreateLoader(int id, Bundle args) {
        return new ShowTaskFragmentLoader(getActivity(), mTaskId);
    }

    @Override
    public void onLoadFinished(Loader<ShowTaskFragmentLoader.Data> loader, final ShowTaskFragmentLoader.Data data) {
        mData = data;

        mFragmentShowTaskFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(CreateChildTaskActivity.getCreateIntent(getActivity(), data.TaskId));
            }
        });

        ArrayList<TaskAdapter.Data> taskDatas = new ArrayList<>();
        for (ShowTaskFragmentLoader.ChildTaskData childTaskData : data.ChildTaskDatas)
            taskDatas.add(new TaskAdapter.Data(childTaskData.TaskId, childTaskData.Name, null, childTaskData.HasChildTasks));

        mFragmentShowTaskRecycler.setAdapter(new TaskAdapter(getActivity(), taskDatas, data.DataId, null));
    }

    @Override
    public void onLoaderReset(Loader<ShowTaskFragmentLoader.Data> loader) {
    }
}
