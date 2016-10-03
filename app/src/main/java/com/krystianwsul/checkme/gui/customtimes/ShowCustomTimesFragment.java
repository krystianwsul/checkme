package com.krystianwsul.checkme.gui.customtimes;


import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.krystianwsul.checkme.MyCrashlytics;
import com.krystianwsul.checkme.R;
import com.krystianwsul.checkme.domainmodel.DomainFactory;
import com.krystianwsul.checkme.gui.SelectionCallback;
import com.krystianwsul.checkme.loaders.ShowCustomTimesLoader;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.List;

public class ShowCustomTimesFragment extends Fragment implements LoaderManager.LoaderCallbacks<ShowCustomTimesLoader.Data> {
    private static final String SELECTED_CUSTOM_TIME_IDS_KEY = "selectedCustomTimeIds";

    private FloatingActionButton mShowTimesFab;
    private RecyclerView mShowTimesList;
    private CustomTimesAdapter mCustomTimesAdapter;
    private TextView mEmptyText;

    private ArrayList<Integer> mSelectedCustomTimeIds;

    private final SelectionCallback mSelectionCallback = new SelectionCallback() {
        @Override
        protected void unselect() {
            mCustomTimesAdapter.unselect();
        }

        @Override
        protected void onMenuClick(MenuItem menuItem) {
            ArrayList<Integer> customTimeIds = mCustomTimesAdapter.getSelected();
            Assert.assertTrue(customTimeIds != null);
            Assert.assertTrue(!customTimeIds.isEmpty());

            switch (menuItem.getItemId()) {
                case R.id.action_custom_times_delete:
                    mCustomTimesAdapter.removeSelected();
                    break;
                default:
                    throw new UnsupportedOperationException();
            }
        }

        @Override
        protected void onFirstAdded() {
            ((AppCompatActivity) getActivity()).startSupportActionMode(this);

            mActionMode.getMenuInflater().inflate(R.menu.menu_custom_times, mActionMode.getMenu());

            mShowTimesFab.setVisibility(View.GONE);

            ((CustomTimesListListener) getActivity()).onCreateCustomTimesActionMode(mActionMode);
        }

        @Override
        protected void onSecondAdded() {

        }

        @Override
        protected void onOtherAdded() {

        }

        @Override
        protected void onLastRemoved() {
            mShowTimesFab.setVisibility(View.VISIBLE);

            ((CustomTimesListListener) getActivity()).onDestroyCustomTimesActionMode();
        }

        @Override
        protected void onSecondToLastRemoved() {

        }

        @Override
        protected void onOtherRemoved() {

        }
    };

    public static ShowCustomTimesFragment newInstance() {
        return new ShowCustomTimesFragment();
    }

    public ShowCustomTimesFragment() {

    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        Assert.assertTrue(context instanceof CustomTimesListListener);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_show_custom_times, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        View view = getView();
        Assert.assertTrue(view != null);

        mShowTimesList = (RecyclerView) view.findViewById(R.id.show_times_list);
        mShowTimesList.setLayoutManager(new LinearLayoutManager(getActivity()));

        mShowTimesFab = (FloatingActionButton) view.findViewById(R.id.show_times_fab);
        Assert.assertTrue(mShowTimesFab != null);

        mShowTimesFab.setOnClickListener(v -> startActivity(ShowCustomTimeActivity.getCreateIntent(getActivity())));

        mEmptyText = (TextView) view.findViewById(R.id.empty_text);
        Assert.assertTrue(mEmptyText != null);

        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(SELECTED_CUSTOM_TIME_IDS_KEY)) {
                mSelectedCustomTimeIds = savedInstanceState.getIntegerArrayList(SELECTED_CUSTOM_TIME_IDS_KEY);
                Assert.assertTrue(mSelectedCustomTimeIds != null);
                Assert.assertTrue(!mSelectedCustomTimeIds.isEmpty());
            }
        }

        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public void onResume() {
        MyCrashlytics.log("ShowCustomTimesFragment.onResume");

        super.onResume();
    }

    @Override
    public Loader<ShowCustomTimesLoader.Data> onCreateLoader(int id, Bundle args) {
        return new ShowCustomTimesLoader(getActivity());
    }

    @Override
    public void onLoadFinished(Loader<ShowCustomTimesLoader.Data> loader, ShowCustomTimesLoader.Data data) {
        Assert.assertTrue(data != null);

        if (mCustomTimesAdapter != null) {
            ArrayList<Integer> selectedCustomTimeIds = mCustomTimesAdapter.getSelected();
            if (selectedCustomTimeIds.isEmpty())
                mSelectedCustomTimeIds = null;
            else
                mSelectedCustomTimeIds = selectedCustomTimeIds;
        }

        mCustomTimesAdapter = new CustomTimesAdapter(data, this, mSelectedCustomTimeIds);
        mShowTimesList.setAdapter(mCustomTimesAdapter);

        mSelectionCallback.setSelected(mCustomTimesAdapter.getSelected().size());

        mShowTimesFab.setVisibility(View.VISIBLE);

        if (data.Entries.isEmpty()) {
            mShowTimesList.setVisibility(View.GONE);
            mEmptyText.setVisibility(View.VISIBLE);
            mEmptyText.setText(R.string.custom_times_empty);
        } else {
            mShowTimesList.setVisibility(View.VISIBLE);
            mEmptyText.setVisibility(View.GONE);
        }
    }

    @Override
    public void onLoaderReset(Loader<ShowCustomTimesLoader.Data> loader) {
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (mCustomTimesAdapter != null) {
            ArrayList<Integer> selectedCustomTimeIds = mCustomTimesAdapter.getSelected();
            if (!selectedCustomTimeIds.isEmpty())
                outState.putIntegerArrayList(SELECTED_CUSTOM_TIME_IDS_KEY, selectedCustomTimeIds);
        }
    }

    public static class CustomTimesAdapter extends RecyclerView.Adapter<CustomTimesAdapter.CustomTimeHolder> {
        private final int mDataId;
        private final List<CustomTimeWrapper> mCustomTimeWrappers;

        @NonNull
        private final ShowCustomTimesFragment mShowCustomTimesFragment;

        CustomTimesAdapter(@NonNull ShowCustomTimesLoader.Data data, @NonNull ShowCustomTimesFragment showCustomTimesFragment, @Nullable ArrayList<Integer> selectedCustomTimeIds) {
            mDataId = data.DataId;
            mCustomTimeWrappers = Stream.of(data.Entries)
                    .map(customTimeData -> new CustomTimeWrapper(customTimeData, selectedCustomTimeIds))
                    .collect(Collectors.toList());

            mShowCustomTimesFragment = showCustomTimesFragment;
        }

        @Override
        public int getItemCount() {
            return mCustomTimeWrappers.size();
        }

        void unselect() {
            Stream.of(mCustomTimeWrappers)
                    .filter(customTimeWrapper -> customTimeWrapper.mSelected)
                    .forEach(customTimeWrapper -> {
                        customTimeWrapper.mSelected = false;
                        notifyItemChanged(mCustomTimeWrappers.indexOf(customTimeWrapper));
                    });
        }

        @Override
        public CustomTimeHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater layoutInflater = LayoutInflater.from(mShowCustomTimesFragment.getActivity());
            View showCustomTimesRow = layoutInflater.inflate(R.layout.row_show_custom_times, parent, false);

            TextView timesRowName = (TextView) showCustomTimesRow.findViewById(R.id.times_row_name);

            return new CustomTimeHolder(showCustomTimesRow, timesRowName);
        }

        @Override
        public void onBindViewHolder(final CustomTimeHolder customTimeHolder, int position) {
            CustomTimeWrapper customTimeWrapper = mCustomTimeWrappers.get(position);
            Assert.assertTrue(customTimeWrapper != null);

            customTimeHolder.mTimesRowName.setText(customTimeWrapper.mCustomTimeData.Name);

            if (customTimeWrapper.mSelected)
                customTimeHolder.mShowCustomTimeRow.setBackgroundColor(ContextCompat.getColor(mShowCustomTimesFragment.getActivity(), R.color.selected));
            else
                customTimeHolder.mShowCustomTimeRow.setBackgroundColor(Color.TRANSPARENT);

            customTimeHolder.mShowCustomTimeRow.setOnLongClickListener(v -> {
                customTimeHolder.onLongClick();
                return true;
            });

            customTimeHolder.mShowCustomTimeRow.setOnClickListener(v -> {
                if (mShowCustomTimesFragment.mSelectionCallback.hasActionMode())
                    customTimeHolder.onLongClick();
                else
                    customTimeHolder.onRowClick();
            });
        }

        ArrayList<Integer> getSelected() {
            return new ArrayList<>(Stream.of(mCustomTimeWrappers)
                .filter(customTimeWrapper -> customTimeWrapper.mSelected)
                .map(customTimeWrapper -> customTimeWrapper.mCustomTimeData.Id)
                .collect(Collectors.toList()));
        }

        void removeSelected() {
            List<CustomTimeWrapper> selectedCustomTimeWrappers = Stream.of(mCustomTimeWrappers)
                    .filter(customTimeWrapper -> customTimeWrapper.mSelected)
                    .collect(Collectors.toList());

            for (CustomTimeWrapper customTimeWrapper : selectedCustomTimeWrappers) {
                int position = mCustomTimeWrappers.indexOf(customTimeWrapper);
                mCustomTimeWrappers.remove(position);
                notifyItemRemoved(position);
            }

            List<Integer> selectedCustomTimeIds = Stream.of(selectedCustomTimeWrappers)
                    .map(customTimeWrapper -> customTimeWrapper.mCustomTimeData.Id)
                    .collect(Collectors.toList());

            DomainFactory.getDomainFactory(mShowCustomTimesFragment.getActivity()).setCustomTimeCurrent(mShowCustomTimesFragment.getActivity(), mDataId, selectedCustomTimeIds);
        }

        class CustomTimeHolder extends RecyclerView.ViewHolder {
            final View mShowCustomTimeRow;
            final TextView mTimesRowName;

            CustomTimeHolder(View showCustomTimesRow, TextView timesRowName) {
                super(showCustomTimesRow);

                Assert.assertTrue(timesRowName != null);

                mShowCustomTimeRow = showCustomTimesRow;
                mTimesRowName = timesRowName;
            }

            void onRowClick() {
                CustomTimeWrapper customTimeWrapper = mCustomTimeWrappers.get(getAdapterPosition());
                Assert.assertTrue(customTimeWrapper != null);

                mShowCustomTimesFragment.getActivity().startActivity(ShowCustomTimeActivity.getEditIntent(customTimeWrapper.mCustomTimeData.Id, mShowCustomTimesFragment.getActivity()));
            }

            void onLongClick() {
                int position = getAdapterPosition();

                CustomTimeWrapper customTimeWrapper = mCustomTimeWrappers.get(position);
                Assert.assertTrue(customTimeWrapper != null);

                customTimeWrapper.mSelected = !customTimeWrapper.mSelected;

                if (customTimeWrapper.mSelected) {
                    mShowCustomTimesFragment.mSelectionCallback.incrementSelected();
                } else {
                    mShowCustomTimesFragment.mSelectionCallback.decrementSelected();
                }

                notifyItemChanged(position);
            }
        }
    }

    private static class CustomTimeWrapper {
        final ShowCustomTimesLoader.CustomTimeData mCustomTimeData;
        boolean mSelected = false;

        CustomTimeWrapper(ShowCustomTimesLoader.CustomTimeData customTimeData, ArrayList<Integer> selectedCustomTimeIds) {
            Assert.assertTrue(customTimeData != null);

            mCustomTimeData = customTimeData;

            if (selectedCustomTimeIds != null) {
                Assert.assertTrue(!selectedCustomTimeIds.isEmpty());

                mSelected = selectedCustomTimeIds.contains(mCustomTimeData.Id);
            }
        }
    }

    public interface CustomTimesListListener {
        void onCreateCustomTimesActionMode(ActionMode actionMode);
        void onDestroyCustomTimesActionMode();
    }
}
