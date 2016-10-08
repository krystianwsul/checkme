package com.krystianwsul.checkme.gui;

import com.codetroopers.betterpickers.calendardatepicker.CalendarDatePickerDialogFragment;
import com.codetroopers.betterpickers.calendardatepicker.MonthAdapter;
import com.krystianwsul.checkme.MyCrashlytics;
import com.krystianwsul.checkme.utils.time.Date;

import java.util.Calendar;

public class MyCalendarFragment extends CalendarDatePickerDialogFragment {
    public MyCalendarFragment() {
        super();

        setDateRange(new MonthAdapter.CalendarDay(Calendar.getInstance()), null);
    }

    public void setDate(Date date) {
        setPreselectedDate(date.getYear(), date.getMonth() - 1, date.getDay());
    }

    @Override
    public void onResume() {
        super.onResume();

        MyCrashlytics.log("MyCalendarFragment.onResume");
    }
}
