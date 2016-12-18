package com.krystianwsul.checkme.gui.tasks;


import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.krystianwsul.checkme.R;
import com.krystianwsul.checkme.gui.AbstractDialogFragment;
import com.krystianwsul.checkme.loaders.CreateTaskLoader;

import junit.framework.Assert;

import java.util.List;

public class FriendPickerFragment extends AbstractDialogFragment {
    private static final String SHOW_DELETE_KEY = "showDelete";

    private ProgressBar mFriendPickerProgress;
    private RecyclerView mFriendPickerRecycler;

    @Nullable
    private List<CreateTaskLoader.UserData> mUserDatas;

    private Listener mListener;

    @NonNull
    public static FriendPickerFragment newInstance(boolean showDelete) {
        FriendPickerFragment parentPickerFragment = new FriendPickerFragment();

        Bundle args = new Bundle();
        args.putBoolean(SHOW_DELETE_KEY, showDelete);
        parentPickerFragment.setArguments(args);

        return parentPickerFragment;
    }

    public FriendPickerFragment() {

    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        MaterialDialog.Builder builder = new MaterialDialog.Builder(getActivity())
                .title(R.string.friend_dialog_title)
                .customView(R.layout.fragment_friend_picker, false)
                .negativeText(android.R.string.cancel)
                .onNegative((dialog, which) -> dialog.cancel());

        Bundle args = getArguments();
        Assert.assertTrue(args != null);
        Assert.assertTrue(args.containsKey(SHOW_DELETE_KEY));

        boolean showDelete = args.getBoolean(SHOW_DELETE_KEY);

        if (showDelete)
            builder.neutralText(R.string.delete)
                    .onNeutral((dialog, which) -> mListener.onFriendDeleted());

        MaterialDialog materialDialog = builder.build();

        LinearLayout linearLayout = (LinearLayout) materialDialog.getCustomView();
        Assert.assertTrue(linearLayout != null);

        mFriendPickerProgress = (ProgressBar) linearLayout.findViewById(R.id.friend_picker_progress);
        Assert.assertTrue(mFriendPickerProgress != null);

        mFriendPickerRecycler = (RecyclerView) linearLayout.findViewById(R.id.friend_picker_recycler);
        Assert.assertTrue(mFriendPickerRecycler != null);

        mFriendPickerRecycler.setLayoutManager(new LinearLayoutManager(getActivity()));

        return materialDialog;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mFriendPickerProgress.setVisibility(View.GONE);
        mFriendPickerRecycler.setVisibility(View.VISIBLE);

        if (mUserDatas != null)
            initialize();
    }

    public void initialize(@NonNull List<CreateTaskLoader.UserData> userDatas, @NonNull Listener listener) {
        mUserDatas = userDatas;
        mListener = listener;

        if (getActivity() != null)
            initialize();
    }

    private void initialize() {
        Assert.assertTrue(getActivity() != null);
        Assert.assertTrue(mUserDatas != null);
        Assert.assertTrue(mListener != null);

        mFriendPickerRecycler.setAdapter(new FriendListAdapter());
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);

        mListener.onFriendCancel();
    }

    public interface Listener {
        void onFriendSelected(@NonNull String friendId);

        void onFriendDeleted();

        void onFriendCancel();
    }

    public class FriendListAdapter extends RecyclerView.Adapter<FriendListAdapter.FriendHolder> {
        @Override
        public int getItemCount() {
            Assert.assertTrue(mUserDatas != null);

            return mUserDatas.size();
        }

        @Override
        public FriendListAdapter.FriendHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater layoutInflater = LayoutInflater.from(getActivity());
            View friendRow = layoutInflater.inflate(R.layout.row_friend, parent, false);

            TextView friendName = (TextView) friendRow.findViewById(R.id.friend_name);
            Assert.assertTrue(friendName != null);

            TextView friendEmail = (TextView) friendRow.findViewById(R.id.friend_email);
            Assert.assertTrue(friendEmail != null);

            return new FriendListAdapter.FriendHolder(friendRow, friendName, friendEmail);
        }

        @Override
        public void onBindViewHolder(final FriendListAdapter.FriendHolder friendHolder, int position) {
            Assert.assertTrue(mUserDatas != null);

            CreateTaskLoader.UserData userData = mUserDatas.get(position);
            Assert.assertTrue(userData != null);

            friendHolder.mFriendName.setText(userData.mName);
            friendHolder.mFriendEmail.setText(userData.mEmail);

            friendHolder.mFriendRow.setOnClickListener(v -> friendHolder.onRowClick());
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
                Assert.assertTrue(mUserDatas != null);

                CreateTaskLoader.UserData userData = mUserDatas.get(getAdapterPosition());
                Assert.assertTrue(userData != null);

                dismiss();

                mListener.onFriendSelected(userData.mId);
            }
        }
    }
}
