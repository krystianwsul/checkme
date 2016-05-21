package com.example.krystianwsul.organizator.gui;

import com.codetroopers.betterpickers.calendardatepicker.CalendarDatePickerDialogFragment;
import com.codetroopers.betterpickers.calendardatepicker.MonthAdapter;
import com.example.krystianwsul.organizator.utils.time.Date;

import java.util.Calendar;

public class MyCalendarFragment extends CalendarDatePickerDialogFragment {
    public MyCalendarFragment() {
        super();

        setDateRange(new MonthAdapter.CalendarDay(Calendar.getInstance()), null);
    }

    public void setDate(Date date) {
        setPreselectedDate(date.getYear(), date.getMonth() - 1, date.getDay());
    }
}
