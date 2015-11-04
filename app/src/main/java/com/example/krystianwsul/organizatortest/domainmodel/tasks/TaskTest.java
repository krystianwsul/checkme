package com.example.krystianwsul.organizatortest.domainmodel.tasks;

import android.content.Context;

import java.util.ArrayList;

/**
 * Created by Krystian on 11/4/2015.
 */
public interface TaskTest {
    String getName();
    String getScheduleText(Context context);
    ArrayList<TaskTest> getChildTasks();
}
