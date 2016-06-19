package com.krystianwsul.checkme;

import android.text.TextUtils;

import com.krystianwsul.checkme.utils.time.ExactTimeStamp;

import junit.framework.Assert;

import java.util.Collection;
import java.util.LinkedList;

public class EventBuffer {
    private static EventBuffer sInstance;

    private final LinkedList<String> mEvents = new LinkedList<>();

    public synchronized static EventBuffer getInstance() {
        if (sInstance == null)
            sInstance = new EventBuffer();
        return sInstance;
    }

    public synchronized void add(String s) {
        Assert.assertTrue(!TextUtils.isEmpty(s));

        if (mEvents.size() > 30)
            mEvents.remove();

        mEvents.add(ExactTimeStamp.getNow().getHourMili() + ": " + s);
    }

    public synchronized Collection<String> get() {
        return mEvents;
    }
}
