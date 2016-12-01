package com.krystianwsul.checkme.utils;

import android.app.Activity;
import android.content.Intent;
import android.os.Parcel;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.krystianwsul.checkme.R;
import com.krystianwsul.checkme.firebase.UserData;
import com.krystianwsul.checkme.utils.time.Date;
import com.krystianwsul.checkme.utils.time.DayOfWeek;

import junit.framework.Assert;

import java.util.Calendar;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.Set;

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

    @NonNull
    public static String ordinal(int number) {
        String ret = String.valueOf(number);

        if (!Locale.getDefault().getLanguage().equals("pl")) {
            int mod = number % 10;
            switch (mod) {
                case 1:
                    ret += "st";
                    break;
                case 2:
                    ret += "nd";
                    break;
                case 3:
                    ret += "rd";
                    break;
                default:
                    ret += "th";
                    break;
            }
        }

        return ret;
    }

    @NonNull
    public static Set<String> userDatasToKeys(@NonNull Collection<UserData> userDatas) {
        return Stream.of(userDatas)
                .map(UserData::getKey)
                .collect(Collectors.toSet());
    }

    static void writeStringToParcel(@NonNull Parcel parcel, @Nullable String string) {
        if (TextUtils.isEmpty(string)) {
            parcel.writeInt(0);
        } else {
            parcel.writeInt(1);
            parcel.writeString(string);
        }
    }

    @Nullable
    static String readStringFromParcel(@NonNull Parcel parcel) {
        if (parcel.readInt() == 0)
            return null;

        return parcel.readString();
    }
}
