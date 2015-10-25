package com.example.krystianwsul.organizatortest.domain.tasks;

import java.util.ArrayList;

/**
 * Created by Krystian on 10/13/2015.
 */
public interface ParentTask extends Task {
    void addChild(ChildTask child);
    public ArrayList<ChildTask> getChildren();
}
