package com.example.krystianwsul.organizator.loaders;

public class LoaderData {
    private static int sDataId = 1;

    public final int DataId;

    private static int getNextId() {
        return sDataId++;
    }

    public LoaderData() {
        DataId = getNextId();
    }
}
