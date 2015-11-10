package com.example.krystianwsul.organizatortest;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import junit.framework.Assert;

/**
 * Created by Krystian on 10/31/2015.
 */
public class ListFragmentAdapter extends FragmentPagerAdapter {
    public ListFragmentAdapter(FragmentManager fm) {
        super(fm);
    }

    public Fragment getItem(int position) {
        Assert.assertTrue(position >= 0);
        Assert.assertTrue(position <= 1);
        switch (position) {
            case 0:
                return new GroupListFragment();
            case 1:
                return new TaskListFragment();
            default:
                return null;
        }
    }

    public int getCount() {
        return 2;
    }
}
