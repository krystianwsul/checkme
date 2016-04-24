package com.example.krystianwsul.organizator.gui;


import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.example.krystianwsul.organizator.R;
import com.example.krystianwsul.organizator.domainmodel.DomainFactory;
import com.example.krystianwsul.organizator.notifications.TickService;

import junit.framework.Assert;

public class DebugFragment extends Fragment {
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

        TextView debugTime = (TextView) view.findViewById(R.id.debug_time);
        Assert.assertTrue(debugTime != null);

        DomainFactory domainFactory = DomainFactory.getDomainFactory(getActivity());
        debugTime.setText("data load time: " + (domainFactory.getReadMillis() + domainFactory.getInstantiateMilis()) + "ms (" + domainFactory.getReadMillis() + " + " + domainFactory.getInstantiateMilis() + ")");

        Button debugTick = (Button) view.findViewById(R.id.debug_tick);
        Assert.assertTrue(debugTick != null);

        debugTick.setOnClickListener(v -> TickService.startService(getActivity()));

        final TextView debugRecords = (TextView) view.findViewById(R.id.debug_records);
        Assert.assertTrue(debugRecords != null);

        debugRecords.setText("tasks: " + domainFactory.getTaskCount() + ", instances: " + domainFactory.getInstanceCount());
    }
}
