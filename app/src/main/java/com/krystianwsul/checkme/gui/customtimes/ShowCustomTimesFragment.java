package com.krystianwsul.checkme.gui.customtimes;


import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
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
import com.krystianwsul.checkme.R;
import com.krystianwsul.checkme.domainmodel.DomainFactory;
import com.krystianwsul.checkme.gui.AbstractFragment;
import com.krystianwsul.checkme.gui.FabUser;
import com.krystianwsul.checkme.gui.SelectionCallback;
import com.krystianwsul.checkme.loaders.ShowCustomTimesLoader;
import com.krystianwsul.checkme.persistencemodel.SaveService;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.List;

public class ShowCustomTimesFragment extends AbstractFragment implements LoaderManager.LoaderCallbacks<ShowCustomTimesLoader.Data>, FabUser {
    private static final String SELECTED_CUSTOM_TIME_IDS_KEY = "selectedCustomTimeIds";

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

                    updateSelectAll();

                    break;
                default:
                    throw new UnsupportedOperationException();
            }
        }

        @Override
        protected void onFirstAdded() {
            ((AppCompatActivity) getActivity()).startSupportActionMode(this);

            mActionMode.getMenuInflater().inflate(R.menu.menu_custom_times, mActionMode.getMenu());

            updateFabVisibility();

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
            updateFabVisibility();

            ((CustomTimesListListener) getActivity()).onDestroyCustomTimesActionMode();
        }

        @Override
        protected void onSecondToLastRemoved() {

        }

        @Override
        protected void onOtherRemoved() {

        }
    };

    @Nullable
    private FloatingActionButton mShowTimesFab;

    private ShowCustomTimesLoader.Data mData;

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

        mShowTimesList = view.findViewById(R.id.show_times_list);
        mShowTimesList.setLayoutManager(new LinearLayoutManager(getActivity()));

        mEmptyText = view.findViewById(R.id.empty_text);
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
    public Loader<ShowCustomTimesLoader.Data> onCreateLoader(int id, Bundle args) {
        return new ShowCustomTimesLoader(getActivity());
    }

    @Override
    public void onLoadFinished(Loader<ShowCustomTimesLoader.Data> loader, ShowCustomTimesLoader.Data data) {
        Assert.assertTrue(data != null);

        mData = data;

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

        if (data.Entries.isEmpty()) {
            mShowTimesList.setVisibility(View.GONE);
            mEmptyText.setVisibility(View.VISIBLE);
            mEmptyText.setText(R.string.custom_times_empty);
        } else {
            mShowTimesList.setVisibility(View.VISIBLE);
            mEmptyText.setVisibility(View.GONE);
        }

        updateSelectAll();

        updateFabVisibility();
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

    private void updateSelectAll() {
        Assert.assertTrue(mCustomTimesAdapter != null);

        ((CustomTimesListListener) getActivity()).setCustomTimesSelectAllVisibility(mCustomTimesAdapter.getItemCount() != 0);
    }

    public void selectAll() {
        mCustomTimesAdapter.selectAll();
    }

    @Override
    public void setFab(@NonNull FloatingActionButton floatingActionButton) {
        mShowTimesFab = floatingActionButton;

        mShowTimesFab.setOnClickListener(v -> startActivity(ShowCustomTimeActivity.getCreateIntent(getActivity())));

        updateFabVisibility();
    }

    private void updateFabVisibility() {
        if (mShowTimesFab == null)
            return;

        if (mData != null && !mSelectionCallback.hasActionMode()) {
            mShowTimesFab.show();
        } else {
            mShowTimesFab.hide();
        }
    }

    @Override
    public void clearFab() {
        if (mShowTimesFab == null)
            return;

        mShowTimesFab.setOnClickListener(null);

        mShowTimesFab = null;
    }

    class CustomTimesAdapter extends RecyclerView.Adapter<CustomTimesAdapter.CustomTimeHolder> {
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

        public void selectAll() {
            Assert.assertTrue(!mSelectionCallback.hasActionMode());

            Stream.of(mCustomTimeWrappers)
                    .filter(customTimeWrapper -> !customTimeWrapper.mSelected)
                    .forEach(CustomTimeWrapper::toggleSelect);
        }

        @Override
        public CustomTimeHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater layoutInflater = LayoutInflater.from(mShowCustomTimesFragment.getActivity());
            View showCustomTimesRow = layoutInflater.inflate(R.layout.row_show_custom_times, parent, false);

            TextView timesRowName = showCustomTimesRow.findViewById(R.id.times_row_name);

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

            DomainFactory.getDomainFactory(mShowCustomTimesFragment.getActivity()).setCustomTimeCurrent(mShowCustomTimesFragment.getActivity(), mDataId, SaveService.Source.GUI, selectedCustomTimeIds);
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

                customTimeWrapper.toggleSelect();
            }
        }
    }

    private class CustomTimeWrapper {
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

        void toggleSelect() {
            mSelected = !mSelected;

            if (mSelected) {
                mSelectionCallback.incrementSelected();
            } else {
                mSelectionCallback.decrementSelected();
            }

            int position = mCustomTimesAdapter.mCustomTimeWrappers.indexOf(this);
            Assert.assertTrue(position >= 0);

            mCustomTimesAdapter.notifyItemChanged(position);
        }
    }

    public interface CustomTimesListListener {
        void onCreateCustomTimesActionMode(ActionMode actionMode);
        void onDestroyCustomTimesActionMode();
        void setCustomTimesSelectAllVisibility(boolean selectAllVisible);
    }
}
