package com.krystianwsul.checkme.utils;

import android.app.Activity;
import android.content.Intent;
import android.text.TextUtils;

import com.krystianwsul.checkme.R;

import junit.framework.Assert;

public class Utils {
    public static void share(String text, Activity activity) {
        Assert.assertTrue(!TextUtils.isEmpty(text));
        Assert.assertTrue(activity != null);

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_TEXT, text);
        intent.setType("text/plain");

        activity.startActivity(Intent.createChooser(intent, activity.getString(R.string.sendTo)));
    }
}
