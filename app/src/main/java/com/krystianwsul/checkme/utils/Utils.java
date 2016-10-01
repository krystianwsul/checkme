package com.krystianwsul.checkme.utils;

import android.app.Activity;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.krystianwsul.checkme.R;
import com.krystianwsul.checkme.utils.time.Date;
import com.krystianwsul.checkme.utils.time.DayOfWeek;

import junit.framework.Assert;

import java.util.Calendar;
import java.util.GregorianCalendar;

public class Utils {
    public static void share(@NonNull String text, @NonNull Activity activity) {
        Assert.assertTrue(!TextUtils.isEmpty(text));

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_TEXT, text);
        intent.setType("text/plain");

        activity.startActivity(Intent.createChooser(intent, activity.getString(R.string.sendTo)));
    }

    public static int getDaysInMonth(int year, int month) {
        Calendar calendar = new GregorianCalendar(year, month - 1, 1);
        return calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
    }

    @NonNull
    public static Date getDateInMonth(int year, int month, int dayOfMonth, boolean beginningOfMonth) {
        if (beginningOfMonth) {
            return new Date(year, month, dayOfMonth);
        } else {
            return new Date(year, month, Utils.getDaysInMonth(year, month) - dayOfMonth + 1);
        }
    }

    @NonNull
    public static Date getDateInMonth(int year, int month, int dayOfMonth, @NonNull DayOfWeek dayOfWeek, boolean beginningOfMonth) {
        if (beginningOfMonth) {
            Date first = new Date(year, month, 1);

            int day;
            if (dayOfWeek.ordinal() >= first.getDayOfWeek().ordinal()) {
                day = (dayOfMonth - 1) * 7 + (dayOfWeek.ordinal() - first.getDayOfWeek().ordinal()) + 1;
            } else {
                day = dayOfMonth * 7 + (dayOfWeek.ordinal() - first.getDayOfWeek().ordinal()) + 1;
            }

            return new Date(year, month, day);
        } else {
            int daysInMonth = getDaysInMonth(year, month);

            Date last = new Date(year, month, daysInMonth);

            int day;
            if (dayOfWeek.ordinal() <= last.getDayOfWeek().ordinal()) {
                day = (dayOfMonth - 1) * 7 + (last.getDayOfWeek().ordinal() - dayOfWeek.ordinal()) + 1;
            } else {
                day = dayOfMonth * 7 + (last.getDayOfWeek().ordinal() - dayOfWeek.ordinal()) + 1;
            }

            return new Date(year, month, daysInMonth - day + 1);
        }
    }
}
