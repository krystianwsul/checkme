package com.krystianwsul.checkme.gui.tasks;

import android.support.v4.app.LoaderManager;
import android.support.v7.app.AppCompatActivity;

import com.krystianwsul.checkme.loaders.CreateTaskLoader;

public abstract class CreateTaskActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<CreateTaskLoader.Data> {
}
