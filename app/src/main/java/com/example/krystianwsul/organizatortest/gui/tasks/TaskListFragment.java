package com.example.krystianwsul.organizatortest.gui.tasks;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.example.krystianwsul.organizatortest.R;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.RootTask;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.Task;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.TaskFactory;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;

public class TaskListFragment extends Fragment {
    private ListView mTasksList;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.task_list_fragment, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        View view = getView();
        Assert.assertTrue(view != null);

        mTasksList = (ListView) view.findViewById(R.id.tasks_list);

        mTasksList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Task task = (Task) parent.getItemAtPosition(position);
                startActivity(ShowTaskActivity.getIntent(task, view.getContext()));
            }
        });

        FloatingActionButton floatingActionButton = (FloatingActionButton) getView().findViewById(R.id.task_list_fragment_fab);
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(CreateRootTaskActivity.getIntent(getContext()));
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();

        Collection<RootTask> allRootTasks = TaskFactory.getInstance().getRootTasks();
        ArrayList<Task> currentRootTasks = new ArrayList<>();
        for (RootTask rootTask : allRootTasks)
            if (rootTask.current())
                currentRootTasks.add(rootTask);

        Collections.sort(currentRootTasks, new Comparator<Task>() {
            @Override
            public int compare(Task lhs, Task rhs) {
                return lhs.getName().compareTo(rhs.getName());
            }
        });

        mTasksList.setAdapter(new TaskAdapter(getContext(), currentRootTasks));
    }
}
