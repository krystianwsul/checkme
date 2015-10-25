package com.example.krystianwsul.organizatortest.domain.instances;

import java.util.ArrayList;

/**
 * Created by Krystian on 10/23/2015.
 */
public interface ParentInstance extends Instance {
    ArrayList<ChildInstance> getChildren();
}
