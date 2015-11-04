package com.example.krystianwsul.organizatortest;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.example.krystianwsul.organizatortest.domainmodel.tasks.RootTask;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.Task;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.TaskTest;

import java.util.ArrayList;

/**
 * Created by Krystian on 10/31/2015.
 */
public class TaskListFragment extends Fragment {
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.task_list_fragment, container, false);
    }

    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        ListView showTasksList = (ListView) getView().findViewById(R.id.tasks_list);
        ArrayList<TaskTest> tasks = new ArrayList<>();
        for (RootTask task : Task.getRootTasks())
            tasks.add((RootTask) task);
        showTasksList.setAdapter(new TaskAdapter(getContext(), tasks));

        showTasksList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Task task = (Task) parent.getItemAtPosition(position);
                Intent intent = new Intent(view.getContext(), ShowTask.class);
                intent.putExtra("taskId", task.getId());
                startActivity(intent);
            }
        });
    }
}
