package com.example.krystianwsul.organizator;

import android.app.Application;
import android.content.Context;

import org.acra.ACRA;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;

@ReportsCrashes(mailTo = "patricius@gmail.com",
                mode = ReportingInteractionMode.TOAST,
                resToastText = R.string.crash_toast_text)
public class OgranizatorApplication extends Application {
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);

        ACRA.init(this);
    }
}
