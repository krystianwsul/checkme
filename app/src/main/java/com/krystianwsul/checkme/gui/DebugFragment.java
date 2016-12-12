package com.krystianwsul.checkme.gui;


import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.krystianwsul.checkme.DataDiff;
import com.krystianwsul.checkme.MyCrashlytics;
import com.krystianwsul.checkme.R;
import com.krystianwsul.checkme.domainmodel.DomainFactory;
import com.krystianwsul.checkme.notifications.TickService;
import com.krystianwsul.checkme.utils.time.ExactTimeStamp;

import junit.framework.Assert;

public class DebugFragment extends AbstractFragment {
    public static DebugFragment newInstance() {
        return new DebugFragment();
    }

    public DebugFragment() {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_debug, container, false);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        View view = getView();
        Assert.assertTrue(view != null);

        Button debugException = (Button) view.findViewById(R.id.debug_exception);
        Assert.assertTrue(debugException != null);

        debugException.setOnClickListener(v -> {
            int i = 0;
            int j = 1 / i;
        });

        Button debugTick = (Button) view.findViewById(R.id.debug_tick);
        Assert.assertTrue(debugTick != null);

        debugTick.setOnClickListener(v -> TickService.startServiceDebug(getActivity(), "DebugFragment: TickService.startServiceDebug"));

        TextView debugData = (TextView) view.findViewById(R.id.debug_data);
        Assert.assertTrue(debugData != null);

        Button debugLoad = (Button) view.findViewById(R.id.debug_load);
        Assert.assertTrue(debugLoad != null);

        debugLoad.setOnClickListener(v -> {
            StringBuilder stringBuilder = new StringBuilder();

            SharedPreferences sharedPreferences = getActivity().getSharedPreferences(TickService.TICK_PREFERENCES, Context.MODE_PRIVATE);
            long lastTick = sharedPreferences.getLong(TickService.LAST_TICK_KEY, -1);
            String tickLog = sharedPreferences.getString(TickService.TICK_LOG, "");

            ExactTimeStamp lastTickExactTimeStamp = new ExactTimeStamp(lastTick);

            stringBuilder.append("last beeping tick: ");
            stringBuilder.append(lastTickExactTimeStamp.toString());
            stringBuilder.append("tick log:\n");
            stringBuilder.append(tickLog);

            DomainFactory domainFactory = DomainFactory.getDomainFactory(getActivity());

            stringBuilder.append("\ndata load time: ");
            stringBuilder.append((domainFactory.getReadMillis() + domainFactory.getInstantiateMillis()));
            stringBuilder.append("ms (");
            stringBuilder.append(domainFactory.getReadMillis());
            stringBuilder.append(" + ");
            stringBuilder.append(domainFactory.getInstantiateMillis());
            stringBuilder.append(")");

            stringBuilder.append("\ntasks: ");
            stringBuilder.append(domainFactory.getTaskCount());
            stringBuilder.append(", instances: ");
            stringBuilder.append(domainFactory.getInstanceCount());
            stringBuilder.append(", custom times: ");
            stringBuilder.append(domainFactory.getCustomTimeCount());

            ExactTimeStamp t1 = ExactTimeStamp.getNow();
            DomainFactory.getDomainFactory(getContext()).getGroupListData(getActivity(), ExactTimeStamp.getNow(), 0, MainActivity.TimeRange.DAY);
            ExactTimeStamp t2 = ExactTimeStamp.getNow();

            stringBuilder.append("\ntoday: ");
            stringBuilder.append((t2.getLong() - t1.getLong()));
            stringBuilder.append(" ms");

            stringBuilder.append("\ncrashlytics enabled: ");
            stringBuilder.append(MyCrashlytics.getEnabled());

            debugData.setText(stringBuilder);
        });

        Button debugDiffButton = (Button) view.findViewById(R.id.debug_diff_button);
        Assert.assertTrue(debugDiffButton != null);

        TextView debugDiffText = (TextView) view.findViewById(R.id.debug_diff_text);
        Assert.assertTrue(debugDiffText != null);

        debugDiffButton.setOnClickListener(v -> debugDiffText.setText(DataDiff.getDiff()));
    }
}
