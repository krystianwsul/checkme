package com.krystianwsul.checkme.gui;

import android.app.Dialog;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.afollestad.materialdialogs.MaterialDialog;
import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.krystianwsul.checkme.R;
import com.krystianwsul.checkme.utils.CustomTimeKey;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.List;

public class TimeDialogFragment extends AbstractDialogFragment {
    private static final String CUSTOM_TIMES_KEY = "customTimes";

    private TimeDialogListener mTimeDialogListener;

    @NonNull
    public static TimeDialogFragment newInstance(@NonNull ArrayList<CustomTimeData> customTimeDatas) {
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
        names.add(getString(R.string.add));

        return new MaterialDialog.Builder(getActivity())
                .title(R.string.time_dialog_title)
                .items(names)
                .itemsCallback((dialog, view, which, text) -> {
                    Assert.assertTrue(mTimeDialogListener != null);
                    Assert.assertTrue(which < names.size());
                    Assert.assertTrue(which < customTimeDatas.size() + 2);

                    if (which < customTimeDatas.size()) {
                        CustomTimeKey customTimeKey = customTimeDatas.get(which).mCustomTimeKey;
                        mTimeDialogListener.onCustomTimeSelected(customTimeKey);
                    } else if (which == customTimeDatas.size()) {
                        mTimeDialogListener.onOtherSelected();
                    } else {
                        Assert.assertTrue(which == customTimeDatas.size() + 1);
                        mTimeDialogListener.onAddSelected();
                    }
                })
                .show();
    }

    public void setTimeDialogListener(@NonNull TimeDialogListener timeDialogListener) {
        mTimeDialogListener = timeDialogListener;
    }

    public interface TimeDialogListener {
        void onCustomTimeSelected(@NonNull CustomTimeKey customTimeKey);

        void onOtherSelected();

        void onAddSelected();
    }

    public static class CustomTimeData implements Parcelable {
        @NonNull
        public final CustomTimeKey mCustomTimeKey;

        @NonNull
        public final String Name;

        public CustomTimeData(@NonNull CustomTimeKey customTimeKey, @NonNull String name) {
            Assert.assertTrue(!TextUtils.isEmpty(name));

            mCustomTimeKey = customTimeKey;
            Name = name;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeParcelable(mCustomTimeKey, 0);
            dest.writeString(Name);
        }

        public static final Parcelable.Creator<CustomTimeData> CREATOR = new Creator<CustomTimeData>() {
            @Override
            public CustomTimeData createFromParcel(Parcel source) {
                CustomTimeKey customTimeKey = source.readParcelable(CustomTimeKey.class.getClassLoader());
                Assert.assertTrue(customTimeKey != null);

                String name = source.readString();
                Assert.assertTrue(!TextUtils.isEmpty(name));

                return new CustomTimeData(customTimeKey, name);
            }

            @Override
            public CustomTimeData[] newArray(int size) {
                return new CustomTimeData[size];
            }
        };
    }
}
