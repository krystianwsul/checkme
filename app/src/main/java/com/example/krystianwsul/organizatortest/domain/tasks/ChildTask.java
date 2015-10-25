package com.example.krystianwsul.organizatortest.domain.tasks;

/**
 * Created by Krystian on 10/13/2015.
 */
public interface ChildTask extends Task {
    public ParentTask getParent();
}
