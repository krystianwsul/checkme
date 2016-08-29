package com.krystianwsul.checkme.gui.tasks;

import android.content.Context;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.krystianwsul.checkme.R;
import com.krystianwsul.checkme.loaders.ScheduleLoader;
import com.krystianwsul.checkme.utils.ScheduleType;
import com.krystianwsul.checkme.utils.time.DayOfWeek;
import com.krystianwsul.checkme.utils.time.TimePairPersist;

import junit.framework.Assert;

import java.util.List;
import java.util.Map;

public class RepeatingScheduleFragment extends Fragment {
    protected static final String SCHEDULE_HINT_KEY = "scheduleHint";
    protected static final String ROOT_TASK_ID_KEY = "rootTaskId";

    protected static final String HOUR_MINUTE_PICKER_POSITION_KEY = "hourMinutePickerPosition";

    protected static final String SCHEDULE_ENTRY_KEY = "scheduleEntries";

    protected static final String WEEKLY_SCHEDULE_DIALOG = "weeklyScheduleDialog";
    protected static final String DAILY_SCHEDULE_DIALOG_TAG = "dailyScheduleDialog";

    protected int mHourMinutePickerPosition = -1;

    protected RecyclerView mScheduleTimes;
    protected ScheduleAdapter mScheduleAdapter;

    protected CreateTaskActivity.ScheduleHint mScheduleHint;

    protected Bundle mSavedInstanceState;

    protected Integer mRootTaskId;
    protected ScheduleLoader.Data mData;

    protected FloatingActionButton mScheduleFab;

    protected boolean mFirst = true;

    protected List<ScheduleEntry> mScheduleEntries;

    protected final DailyScheduleDialogFragment.DailyScheduleDialogListener mDailyScheduleDialogListener = new DailyScheduleDialogFragment.DailyScheduleDialogListener() {
        @Override
        public void onDailyScheduleDialogResult(TimePairPersist timePairPersist) {
            Assert.assertTrue(timePairPersist != null);
            Assert.assertTrue(mHourMinutePickerPosition != -1);
            Assert.assertTrue(mData != null);

            DailyScheduleFragment.DailyScheduleEntry dailyScheduleEntry = (DailyScheduleFragment.DailyScheduleEntry) mScheduleEntries.get(mHourMinutePickerPosition);
            Assert.assertTrue(dailyScheduleEntry != null);

            dailyScheduleEntry.mTimePairPersist = timePairPersist;
            mScheduleAdapter.notifyItemChanged(mHourMinutePickerPosition);

            mHourMinutePickerPosition = -1;
        }
    };

    protected final WeeklyScheduleDialogFragment.WeeklyScheduleDialogListener mWeeklyScheduleDialogListener = new WeeklyScheduleDialogFragment.WeeklyScheduleDialogListener() {
        @Override
        public void onWeeklyScheduleDialogResult(DayOfWeek dayOfWeek, TimePairPersist timePairPersist) {
            Assert.assertTrue(dayOfWeek != null);
            Assert.assertTrue(timePairPersist != null);

            Assert.assertTrue(mHourMinutePickerPosition != -1);
            Assert.assertTrue(mData != null);

            WeeklyScheduleFragment.WeeklyScheduleEntry weeklyScheduleEntry = (WeeklyScheduleFragment.WeeklyScheduleEntry) mScheduleEntries.get(mHourMinutePickerPosition);
            Assert.assertTrue(weeklyScheduleEntry != null);

            weeklyScheduleEntry.mDayOfWeek = dayOfWeek;
            weeklyScheduleEntry.mTimePairPersist = timePairPersist;

            mScheduleAdapter.notifyItemChanged(mHourMinutePickerPosition);

            mHourMinutePickerPosition = -1;
        }
    };

    protected class ScheduleAdapter extends RecyclerView.Adapter<ScheduleAdapter.ScheduleHolder> {
        private final Context mContext;

        public ScheduleAdapter(Context context) {
            Assert.assertTrue(context != null);

            mContext = context;
        }

        @Override
        public ScheduleHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View scheduleRow = LayoutInflater.from(mContext).inflate(R.layout.row_schedule, parent, false);

            TextView scheduleTime = (TextView) scheduleRow.findViewById(R.id.schedule_text);
            Assert.assertTrue(scheduleTime != null);

            ImageView scheduleImage = (ImageView) scheduleRow.findViewById(R.id.schedule_image);
            Assert.assertTrue(scheduleImage != null);

            return new ScheduleHolder(scheduleRow, scheduleTime, scheduleImage);
        }

        @Override
        public void onBindViewHolder(final ScheduleHolder scheduleHolder, int position) {
            final ScheduleEntry scheduleEntry = mScheduleEntries.get(position);
            Assert.assertTrue(scheduleEntry != null);

            scheduleHolder.mScheduleText.setText(scheduleEntry.getText(mData.CustomTimeDatas));

            scheduleHolder.mScheduleText.setOnClickListener(v -> scheduleHolder.onTextClick());

            scheduleHolder.mScheduleImage.setVisibility(scheduleEntry.getShowDelete() ? View.VISIBLE : View.INVISIBLE);

            scheduleHolder.mScheduleImage.setOnClickListener(v -> {
                Assert.assertTrue(mScheduleEntries.size() > 1);
                scheduleHolder.delete();

                if (mScheduleEntries.size() == 1) {
                    mScheduleEntries.get(0).setShowDelete(false);
                    notifyItemChanged(0);
                }
            });
        }

        @Override
        public int getItemCount() {
            return mScheduleEntries.size();
        }

        public void addScheduleEntry(ScheduleEntry scheduleEntry) {
            Assert.assertTrue(scheduleEntry != null);

            int position = mScheduleEntries.size();
            Assert.assertTrue(position > 0);

            if (position == 1) {
                mScheduleEntries.get(0).setShowDelete(true);
                notifyItemChanged(0);
            }

            mScheduleEntries.add(position, scheduleEntry);
            notifyItemInserted(position);
        }

        public class ScheduleHolder extends RecyclerView.ViewHolder {
            public final TextView mScheduleText;
            public final ImageView mScheduleImage;

            public ScheduleHolder(View scheduleRow, TextView scheduleText, ImageView scheduleImage) {
                super(scheduleRow);

                Assert.assertTrue(scheduleText != null);
                Assert.assertTrue(scheduleImage != null);

                mScheduleText = scheduleText;
                mScheduleImage = scheduleImage;
            }

            public void onTextClick() {
                Assert.assertTrue(mData != null);

                mHourMinutePickerPosition = getAdapterPosition();

                ScheduleEntry scheduleEntry = mScheduleEntries.get(mHourMinutePickerPosition);
                Assert.assertTrue(scheduleEntry != null);

                switch (scheduleEntry.getScheduleType()) {
                    case SINGLE:
                        throw new UnsupportedOperationException();
                    case DAILY:
                        DailyScheduleFragment.DailyScheduleEntry dailyScheduleEntry = (DailyScheduleFragment.DailyScheduleEntry) scheduleEntry;

                        DailyScheduleDialogFragment dailyScheduleDialogFragment = DailyScheduleDialogFragment.newInstance(dailyScheduleEntry.mTimePairPersist);
                        Assert.assertTrue(dailyScheduleDialogFragment != null);

                        dailyScheduleDialogFragment.initialize(mData.CustomTimeDatas, mDailyScheduleDialogListener);

                        dailyScheduleDialogFragment.show(getChildFragmentManager(), DAILY_SCHEDULE_DIALOG_TAG);
                        break;
                    case WEEKLY:
                        WeeklyScheduleFragment.WeeklyScheduleEntry weeklyScheduleEntry = (WeeklyScheduleFragment.WeeklyScheduleEntry) scheduleEntry;

                        WeeklyScheduleDialogFragment weeklyScheduleDialogFragment = WeeklyScheduleDialogFragment.newInstance(weeklyScheduleEntry.mDayOfWeek, weeklyScheduleEntry.mTimePairPersist);
                        Assert.assertTrue(weeklyScheduleDialogFragment != null);

                        weeklyScheduleDialogFragment.initialize(mData.CustomTimeDatas, mWeeklyScheduleDialogListener);

                        weeklyScheduleDialogFragment.show(getChildFragmentManager(), WEEKLY_SCHEDULE_DIALOG);
                        break;
                    default:
                        throw new UnsupportedOperationException();
                }
            }

            public void delete() {
                int position = getAdapterPosition();
                mScheduleEntries.remove(position);
                notifyItemRemoved(position);
            }
        }
    }

    public static abstract class ScheduleEntry implements Parcelable {
        @Override
        public int describeContents() {
            return 0;
        }

        public abstract ScheduleType getScheduleType();

        public abstract String getText(Map<Integer, ScheduleLoader.CustomTimeData> customTimeDatas);

        public abstract void setShowDelete(boolean delete);

        public abstract boolean getShowDelete();

        public static final Creator<ScheduleEntry> CREATOR = new Creator<ScheduleEntry>() {
            @Override
            public ScheduleEntry createFromParcel(Parcel in) {
                ScheduleType scheduleType = (ScheduleType) in.readSerializable();
                Assert.assertTrue(scheduleType != null);

                switch (scheduleType) {
                    case SINGLE:
                        throw new UnsupportedOperationException();
                    case DAILY:
                        return new DailyScheduleFragment.DailyScheduleEntry(in);
                    case WEEKLY:
                        return new WeeklyScheduleFragment.WeeklyScheduleEntry(in);
                    default:
                        throw new UnsupportedOperationException();
                }
            }

            @Override
            public ScheduleEntry[] newArray(int size) {
                return new ScheduleEntry[size];
            }
        };
    }
}
