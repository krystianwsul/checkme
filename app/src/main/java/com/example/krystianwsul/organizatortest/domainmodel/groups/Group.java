package com.example.krystianwsul.organizatortest.domainmodel.groups;

import android.content.Context;

import com.example.krystianwsul.organizatortest.domainmodel.dates.DateTime;

import java.util.ArrayList;

/**
 * Created by Krystian on 11/9/2015.
 */
public interface Group {
    String getName();
    String getScheduleText(Context context);
    ArrayList<Group> getChildGroups();
    DateTime getDateTime();
    String getIntentKey();
    int getIntentValue();
}
