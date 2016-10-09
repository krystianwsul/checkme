package com.krystianwsul.checkme.gui.tasks;


import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.krystianwsul.checkme.R;
import com.krystianwsul.checkme.firebase.FriendListLoader;
import com.krystianwsul.checkme.firebase.UserData;
import com.krystianwsul.checkme.gui.AbstractDialogFragment;
import com.krystianwsul.checkme.gui.MainActivity;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.List;

public class FriendPickerFragment extends AbstractDialogFragment implements LoaderManager.LoaderCallbacks<List<UserData>> {
    private static final String SHOW_DELETE_KEY = "showDelete";
    private static final String CHOSEN_EMAILS_KEY = "chosenEmails";

    private ProgressBar mFriendPickerProgress;
    private RecyclerView mFriendPickerRecycler;

    private List<String> mChosenEmails;

    @NonNull
    public static FriendPickerFragment newInstance(boolean showDelete, @NonNull ArrayList<String> chosenEmails) {
        FriendPickerFragment parentPickerFragment = new FriendPickerFragment();

        Bundle args = new Bundle();
        args.putBoolean(SHOW_DELETE_KEY, showDelete);
        args.putStringArrayList(CHOSEN_EMAILS_KEY, chosenEmails);
        parentPickerFragment.setArguments(args);

        return parentPickerFragment;
    }

    public FriendPickerFragment() {

    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        Assert.assertTrue(context instanceof Listener);
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
        Assert.assertTrue(args.containsKey(CHOSEN_EMAILS_KEY));

        boolean showDelete = args.getBoolean(SHOW_DELETE_KEY);

        mChosenEmails = args.getStringArrayList(CHOSEN_EMAILS_KEY);
        Assert.assertTrue(mChosenEmails != null);

        if (showDelete)
            builder.neutralText(R.string.delete)
                    .onNeutral((dialog, which) -> ((Listener) getActivity()).onFriendDeleted());

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

        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public Loader<List<UserData>> onCreateLoader(int id, Bundle args) {
        UserData userData = MainActivity.getUserData();
        Assert.assertTrue(userData != null);

        return new FriendListLoader(getActivity(), userData);
    }

    @Override
    public void onLoadFinished(Loader<List<UserData>> loader, @Nullable List<UserData> data) {
        if (data == null) {
            Toast.makeText(getActivity(), R.string.connectionError, Toast.LENGTH_SHORT).show();

            dismissAllowingStateLoss();

            ((Listener) getActivity()).onFriendCancel();
        } else {
            mFriendPickerProgress.setVisibility(View.GONE);
            mFriendPickerRecycler.setVisibility(View.VISIBLE);

            mFriendPickerRecycler.setAdapter(new FriendListAdapter(data));
        }
    }

    @Override
    public void onLoaderReset(Loader<List<UserData>> loader) {

    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);

        ((Listener) getActivity()).onFriendCancel();
    }

    public interface Listener {
        void onFriendSelected(@NonNull UserData userData);

        void onFriendDeleted();

        void onFriendCancel();
    }

    public class FriendListAdapter extends RecyclerView.Adapter<FriendListAdapter.FriendHolder> {
        @NonNull
        private final List<UserData> mUserDatas;

        FriendListAdapter(@NonNull List<UserData> userDatas) {
            mUserDatas = Stream.of(userDatas)
                    .filter(userData -> !mChosenEmails.contains(userData.email))
                    .collect(Collectors.toList());
        }

        @Override
        public int getItemCount() {
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
            UserData userData = mUserDatas.get(position);
            Assert.assertTrue(userData != null);

            friendHolder.mFriendName.setText(userData.displayName);
            friendHolder.mFriendEmail.setText(userData.email);

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
                UserData userData = mUserDatas.get(getAdapterPosition());
                Assert.assertTrue(userData != null);

                dismiss();

                ((Listener) getActivity()).onFriendSelected(userData);
            }
        }
    }
}
