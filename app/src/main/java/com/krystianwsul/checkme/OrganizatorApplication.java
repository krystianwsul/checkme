package com.krystianwsul.checkme;

import android.app.Application;

import com.google.android.gms.analytics.ExceptionReporter;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;

public class OrganizatorApplication extends Application {
    private Tracker mTracker;

    @Override
    public void onCreate() {
        super.onCreate();

        Tracker tracker = getDefaultTracker();
        tracker.enableExceptionReporting(true);
        tracker.enableAutoActivityTracking(true);

        ExceptionReporter reporter = new ExceptionReporter(tracker, Thread.getDefaultUncaughtExceptionHandler(), this);
        reporter.setExceptionParser(new AnalyticsExceptionParser(this));
        Thread.setDefaultUncaughtExceptionHandler(reporter);
    }

    synchronized public Tracker getDefaultTracker() {
        if (mTracker == null)
            mTracker = GoogleAnalytics.getInstance(this).newTracker(R.xml.global_tracker);
        return mTracker;
    }
}
