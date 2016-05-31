package com.krystianwsul.checkme.gui;

import android.app.Dialog;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.text.TextUtils;

import com.afollestad.materialdialogs.MaterialDialog;
import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.krystianwsul.checkme.R;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.List;

public class TimeDialogFragment extends DialogFragment {
    private static final String CUSTOM_TIMES_KEY = "customTimes";

    private TimeDialogListener mTimeDialogListener;

    public static TimeDialogFragment newInstance(ArrayList<CustomTimeData> customTimeDatas) {
        Assert.assertTrue(customTimeDatas != null);

        TimeDialogFragment timeDialogFragment = new TimeDialogFragment();

        Bundle args = new Bundle();
        args.putParcelableArrayList(CUSTOM_TIMES_KEY, customTimeDatas);
        timeDialogFragment.setArguments(args);

        return timeDialogFragment;
    }

    public TimeDialogFragment() {

    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle args = getArguments();
        Assert.assertTrue(args != null);
        Assert.assertTrue(args.containsKey(CUSTOM_TIMES_KEY));

        ArrayList<CustomTimeData> customTimeDatas = args.getParcelableArrayList(CUSTOM_TIMES_KEY);
        Assert.assertTrue(customTimeDatas != null);

        List<String> names = Stream.of(customTimeDatas)
                .map(customTimeData -> customTimeData.Name)
                .collect(Collectors.toList());
        names.add(getString(R.string.other));

        return new MaterialDialog.Builder(getActivity())
                .title(R.string.time_dialog_title)
                .items(names)
                .itemsCallback((dialog, view, which, text) -> {
                    Assert.assertTrue(mTimeDialogListener != null);
                    Assert.assertTrue(which < names.size());
                    Assert.assertTrue(which <= customTimeDatas.size());

                    if (which == customTimeDatas.size()) {
                        mTimeDialogListener.onHourMinuteSelected();
                    } else {
                        int id = customTimeDatas.get(which).Id;
                        mTimeDialogListener.onCustomTimeSelected(id);
                    }
                })
                .show();
    }

    public void setTimeDialogListener(TimeDialogListener timeDialogListener) {
        Assert.assertTrue(timeDialogListener != null);
        mTimeDialogListener = timeDialogListener;
    }

    public interface TimeDialogListener {
        void onCustomTimeSelected(int customTimeId);
        void onHourMinuteSelected();
    }

    public static class CustomTimeData implements Parcelable {
        public final int Id;
        public final String Name;

        public CustomTimeData(int id, String name) {
            Assert.assertTrue(name != null);
            Assert.assertTrue(!TextUtils.isEmpty(name));

            Id = id;
            Name = name;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(Id);
            dest.writeString(Name);
        }

        public static final Parcelable.Creator<CustomTimeData> CREATOR = new Creator<CustomTimeData>() {
            @Override
            public CustomTimeData createFromParcel(Parcel source) {
                int id = source.readInt();
                String name = source.readString();

                Assert.assertTrue(name != null);
                Assert.assertTrue(!TextUtils.isEmpty(name));

                return new CustomTimeData(id, name);
            }

            @Override
            public CustomTimeData[] newArray(int size) {
                return new CustomTimeData[size];
            }
        };
    }
}
