package com.krystianwsul.checkme.gui.friends;


import android.graphics.Color;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.google.common.collect.Sets;
import com.krystianwsul.checkme.R;
import com.krystianwsul.checkme.domainmodel.DomainFactory;
import com.krystianwsul.checkme.gui.AbstractFragment;
import com.krystianwsul.checkme.gui.FabUser;
import com.krystianwsul.checkme.gui.SelectionCallback;
import com.krystianwsul.checkme.gui.tasks.FriendPickerFragment;
import com.krystianwsul.checkme.loaders.ShowProjectLoader;
import com.krystianwsul.checkme.persistencemodel.SaveService;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class UserListFragment extends AbstractFragment implements FabUser {
    private static final String PROJECT_ID_KEY = "projectId";

    private static final String SAVE_STATE_KEY = "saveState";

    private static final String FRIEND_PICKER_TAG = "friendPicker";

    private ProgressBar mFriendListProgress;
    private RecyclerView mFriendListRecycler;
    private TextView mEmptyText;

    @Nullable
    private String mProjectId;

    private FriendListAdapter mFriendListAdapter;

    @Nullable
    private ShowProjectLoader.Data mData;

    @Nullable
    private SaveState mSaveState;

    private final SelectionCallback mSelectionCallback = new SelectionCallback() {
        @Override
        protected void unselect() {
            mFriendListAdapter.unselect();
        }

        @Override
        protected void onMenuClick(MenuItem menuItem) {
            ArrayList<String> selectedUserDataEmails = mFriendListAdapter.getSelected();
            Assert.assertTrue(selectedUserDataEmails != null);
            Assert.assertTrue(!selectedUserDataEmails.isEmpty());

            switch (menuItem.getItemId()) {
                case R.id.action_custom_times_delete:
                    mFriendListAdapter.removeSelected();

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
        }

        @Override
        protected void onSecondToLastRemoved() {

        }

        @Override
        protected void onOtherRemoved() {

        }
    };

    @Nullable
    private FloatingActionButton mFriendListFab;

    @NonNull
    public static UserListFragment newInstance() {
        return new UserListFragment();
    }

    public UserListFragment() {

    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        if (args != null) {
            Assert.assertTrue(args.containsKey(PROJECT_ID_KEY));

            mProjectId = args.getString(PROJECT_ID_KEY);
            Assert.assertTrue(!TextUtils.isEmpty(mProjectId));
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_friend_list, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        RelativeLayout friendListLayout = (RelativeLayout) getView();
        Assert.assertTrue(friendListLayout != null);

        mFriendListProgress = friendListLayout.findViewById(R.id.friend_list_progress);
        Assert.assertTrue(mFriendListProgress != null);

        mFriendListRecycler = friendListLayout.findViewById(R.id.friend_list_recycler);
        Assert.assertTrue(mFriendListRecycler != null);

        mFriendListRecycler.setLayoutManager(new LinearLayoutManager(getActivity()));

        mEmptyText = friendListLayout.findViewById(R.id.empty_text);
        Assert.assertTrue(mEmptyText != null);

        if (savedInstanceState != null && savedInstanceState.containsKey(SAVE_STATE_KEY)) {
            mSaveState = savedInstanceState.getParcelable(SAVE_STATE_KEY);
            Assert.assertTrue(mSaveState != null);
        }
    }

    private void initializeFriendPickerFragment(@NonNull FriendPickerFragment friendPickerFragment) {
        Assert.assertTrue(mData != null);

        Set<String> userIds = Stream.of(mFriendListAdapter.mUserDataWrappers)
                .map(userDataWrapper -> userDataWrapper.mUserListData.getId())
                .collect(Collectors.toSet());

        List<FriendPickerFragment.FriendData> friendDatas = Stream.of(mData.getFriendDatas().values())
                .filterNot(friendData -> userIds.contains(friendData.getId()))
                .map(friendData -> new FriendPickerFragment.FriendData(friendData.getId(), friendData.getName(), friendData.getEmail()))
                .collect(Collectors.toList());

        friendPickerFragment.initialize(friendDatas, friendId -> {
            Assert.assertTrue(mData.getFriendDatas().containsKey(friendId));
            Assert.assertTrue(Stream.of(mFriendListAdapter.mUserDataWrappers)
                    .noneMatch(userDataWrapper -> userDataWrapper.mUserListData.getId().equals(friendId)));

            ShowProjectLoader.UserListData friendData = mData.getFriendDatas().get(friendId);
            Assert.assertTrue(friendData != null);

            int position = mFriendListAdapter.getItemCount();

            mFriendListAdapter.mUserDataWrappers.add(new UserDataWrapper(friendData, new HashSet<>()));
            mFriendListAdapter.notifyItemChanged(position);

            if (mData.getUserListDatas().isEmpty()) {
                mEmptyText.setVisibility(View.GONE);
                mFriendListRecycler.setVisibility(View.VISIBLE);
            }
        });
    }

    public void initialize(@Nullable String projectId, @NonNull ShowProjectLoader.Data data) {
        mProjectId = projectId;
        mData = data;

        if (mFriendListAdapter != null)
            mSaveState = mFriendListAdapter.getSaveState();
        else if (mSaveState == null)
            mSaveState = new SaveState(new HashSet<>(), new HashSet<>(), new HashSet<>());

        mFriendListAdapter = new FriendListAdapter(data.getUserListDatas(), mSaveState);
        mFriendListRecycler.setAdapter(mFriendListAdapter);

        mSelectionCallback.setSelected(mFriendListAdapter.getSelected().size());

        mFriendListProgress.setVisibility(View.GONE);

        updateFabVisibility();

        if (mFriendListAdapter.mUserDataWrappers.isEmpty()) {
            mFriendListRecycler.setVisibility(View.GONE);
            mEmptyText.setVisibility(View.VISIBLE);
            mEmptyText.setText(R.string.friends_empty);
        } else {
            mFriendListRecycler.setVisibility(View.VISIBLE);
            mEmptyText.setVisibility(View.GONE);
        }

        FriendPickerFragment friendPickerFragment = (FriendPickerFragment) getChildFragmentManager().findFragmentByTag(FRIEND_PICKER_TAG);
        if (friendPickerFragment != null)
            initializeFriendPickerFragment(friendPickerFragment);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (mFriendListAdapter != null)
            outState.putParcelable(SAVE_STATE_KEY, mFriendListAdapter.getSaveState());
    }

    @SuppressWarnings("RedundantIfStatement")
    public boolean dataChanged() {
        if (mData == null)
            return false;

        SaveState saveState = mFriendListAdapter.getSaveState();

        if (!saveState.mAddedIds.isEmpty())
            return true;

        if (!saveState.mRemovedIds.isEmpty())
            return true;

        return false;
    }

    public void save(@NonNull String name) {
        Assert.assertTrue(!TextUtils.isEmpty(name));
        Assert.assertTrue(mData != null);
        Assert.assertTrue(mFriendListAdapter != null);

        SaveState saveState = mFriendListAdapter.getSaveState();

        if (TextUtils.isEmpty(mProjectId)) {
            Assert.assertTrue(saveState.mRemovedIds.isEmpty());

            DomainFactory.getDomainFactory().createProject(getActivity(), mData.getDataId(), SaveService.Source.GUI, name, saveState.mAddedIds);
        } else {
            DomainFactory.getDomainFactory().updateProject(getActivity(), mData.getDataId(), SaveService.Source.GUI, mProjectId, name, saveState.mAddedIds, saveState.mRemovedIds);
        }
    }

    @Override
    public void setFab(@NonNull FloatingActionButton floatingActionButton) {
        Assert.assertTrue(mFriendListFab == null);

        mFriendListFab = floatingActionButton;

        mFriendListFab.setOnClickListener(v -> {
            FriendPickerFragment friendPickerFragment = FriendPickerFragment.newInstance();
            initializeFriendPickerFragment(friendPickerFragment);
            friendPickerFragment.show(getChildFragmentManager(), FRIEND_PICKER_TAG);
        });

        updateFabVisibility();
    }

    private void updateFabVisibility() {
        if (mFriendListFab == null)
            return;

        if (mData != null && !mSelectionCallback.hasActionMode()) {
            mFriendListFab.show();
        } else {
            mFriendListFab.hide();
        }
    }

    @Override
    public void clearFab() {
        Assert.assertTrue(mFriendListFab != null);

        mFriendListFab.setOnClickListener(null);

        mFriendListFab = null;
    }

    class FriendListAdapter extends RecyclerView.Adapter<FriendListAdapter.FriendHolder> {
        @NonNull
        private final List<UserDataWrapper> mUserDataWrappers;

        FriendListAdapter(@NonNull Collection<ShowProjectLoader.UserListData> userListDatas, @NonNull SaveState saveState) {
            Assert.assertTrue(mData != null);

            Map<String, ShowProjectLoader.UserListData> userListMap = Stream.of(userListDatas)
                    .collect(Collectors.toMap(userListData -> userListData.getId(), userListData -> userListData));

            Stream.of(saveState.mRemovedIds)
                    .forEach(userListMap::remove);

            if (!saveState.mAddedIds.isEmpty()) {
                userListMap.putAll(Stream.of(mData.getFriendDatas().values())
                        .filter(friendData -> saveState.mAddedIds.contains(friendData.getId()))
                        .collect(Collectors.toMap(userListData -> userListData.getId(), userListData -> userListData)));
            }

            mUserDataWrappers = Stream.of(userListMap.values())
                    .sorted((lhs, rhs) -> lhs.getId().compareTo(rhs.getId()))
                    .map(userListData -> new UserDataWrapper(userListData, saveState.mSelectedIds))
                    .collect(Collectors.toList());
        }

        @Override
        public int getItemCount() {
            return mUserDataWrappers.size();
        }

        void unselect() {
            Stream.of(mUserDataWrappers)
                    .filter(userDataWrapper -> userDataWrapper.mSelected)
                    .forEach(userDataWrapper -> {
                        userDataWrapper.mSelected = false;
                        notifyItemChanged(mUserDataWrappers.indexOf(userDataWrapper));
                    });
        }

        @Override
        public FriendHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater layoutInflater = LayoutInflater.from(getActivity());
            View friendRow = layoutInflater.inflate(R.layout.row_friend, parent, false);

            TextView friendName = friendRow.findViewById(R.id.friend_name);
            Assert.assertTrue(friendName != null);

            TextView friendEmail = friendRow.findViewById(R.id.friend_email);
            Assert.assertTrue(friendEmail != null);

            return new FriendHolder(friendRow, friendName, friendEmail);
        }

        @Override
        public void onBindViewHolder(final FriendHolder friendHolder, int position) {
            UserDataWrapper userDataWrapper = mUserDataWrappers.get(position);
            Assert.assertTrue(userDataWrapper != null);

            friendHolder.mFriendName.setText(userDataWrapper.mUserListData.getName());
            friendHolder.mFriendEmail.setText(userDataWrapper.mUserListData.getEmail());

            if (userDataWrapper.mSelected)
                friendHolder.mFriendRow.setBackgroundColor(ContextCompat.getColor(getActivity(), R.color.selected));
            else
                friendHolder.mFriendRow.setBackgroundColor(Color.TRANSPARENT);

            friendHolder.mFriendRow.setOnLongClickListener(v -> {
                friendHolder.onLongClick();
                return true;
            });

            friendHolder.mFriendRow.setOnClickListener(v -> {
                if (mSelectionCallback.hasActionMode())
                    friendHolder.onLongClick();
                else
                    friendHolder.onRowClick();
            });
        }

        ArrayList<String> getSelected() {
            return new ArrayList<>(Stream.of(mUserDataWrappers)
                    .filter(userDataWrapper -> userDataWrapper.mSelected)
                    .map(userDataWrapper -> userDataWrapper.mUserListData.getEmail())
                    .collect(Collectors.toList()));
        }

        void removeSelected() {
            List<UserDataWrapper> selectedUserDataWrappers = Stream.of(mUserDataWrappers)
                    .filter(userDataWrapper -> userDataWrapper.mSelected)
                    .collect(Collectors.toList());

            for (UserDataWrapper userDataWrapper : selectedUserDataWrappers) {
                int position = mUserDataWrappers.indexOf(userDataWrapper);
                mUserDataWrappers.remove(position);
                notifyItemRemoved(position);
            }
        }

        @NonNull
        SaveState getSaveState() {
            Assert.assertTrue(mData != null);

            Set<String> oldUserIds = Stream.of(mData.getUserListDatas())
                    .map(userListData -> userListData.getId())
                    .collect(Collectors.toSet());

            Set<String> newUserIds = Stream.of(mUserDataWrappers)
                    .map(userDataWrapper -> userDataWrapper.mUserListData.getId())
                    .collect(Collectors.toSet());

            Set<String> addedIds = Sets.difference(newUserIds, oldUserIds);
            Set<String> removedIds = Sets.difference(oldUserIds, newUserIds);

            Set<String> selectedIds = Stream.of(mUserDataWrappers)
                    .filter(userDataWrapper -> userDataWrapper.mSelected)
                    .map(userDataWrapper -> userDataWrapper.mUserListData.getId())
                    .collect(Collectors.toSet());

            return new SaveState(addedIds, removedIds, selectedIds);
        }

        class FriendHolder extends RecyclerView.ViewHolder {
            final View mFriendRow;
            final TextView mFriendName;
            final TextView mFriendEmail;

            FriendHolder(@NonNull View friendRow, @NonNull TextView friendName, @NonNull TextView friendEmail) {
                super(friendRow);

                mFriendRow = friendRow;
                mFriendName = friendName;
                mFriendEmail = friendEmail;
            }

            void onRowClick() {
                UserDataWrapper userDataWrapper = mUserDataWrappers.get(getAdapterPosition());
                Assert.assertTrue(userDataWrapper != null);
            }

            void onLongClick() {
                int position = getAdapterPosition();

                UserDataWrapper userDataWrapper = mUserDataWrappers.get(position);
                Assert.assertTrue(userDataWrapper != null);

                userDataWrapper.toggleSelect();
            }
        }
    }

    private class UserDataWrapper {
        @NonNull
        final ShowProjectLoader.UserListData mUserListData;

        boolean mSelected = false;

        UserDataWrapper(@NonNull ShowProjectLoader.UserListData userListData, @NonNull Set<String> selectedIds) {
            mUserListData = userListData;

            mSelected = selectedIds.contains(mUserListData.getId());
        }

        void toggleSelect() {
            mSelected = !mSelected;

            if (mSelected) {
                mSelectionCallback.incrementSelected();
            } else {
                mSelectionCallback.decrementSelected();
            }

            int position = mFriendListAdapter.mUserDataWrappers.indexOf(this);
            Assert.assertTrue(position >= 0);

            mFriendListAdapter.notifyItemChanged(position);
        }
    }

    private static class SaveState implements Parcelable {
        @NonNull
        final Set<String> mAddedIds;

        @NonNull
        final Set<String> mRemovedIds;

        @NonNull
        final Set<String> mSelectedIds;

        SaveState(@NonNull Set<String> addedIds, @NonNull Set<String> removedIds, @NonNull Set<String> selectedIds) {
            mAddedIds = addedIds;
            mRemovedIds = removedIds;
            mSelectedIds = selectedIds;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeStringList(new ArrayList<>(mAddedIds));
            dest.writeStringList(new ArrayList<>(mRemovedIds));
            dest.writeStringList(new ArrayList<>(mSelectedIds));
        }

        @Override
        public int describeContents() {
            return 0;
        }

        public static final Creator<SaveState> CREATOR = new Creator<SaveState>() {
            @Override
            public SaveState createFromParcel(Parcel in) {
                List<String> addedIds = in.createStringArrayList();
                Assert.assertTrue(addedIds != null);

                List<String> removedIds = in.createStringArrayList();
                Assert.assertTrue(removedIds != null);

                List<String> selectedIds = in.createStringArrayList();
                Assert.assertTrue(selectedIds != null);

                return new SaveState(new HashSet<>(addedIds), new HashSet<>(removedIds), new HashSet<>(selectedIds));
            }

            @Override
            public SaveState[] newArray(int size) {
                return new SaveState[size];
            }
        };
    }
}
