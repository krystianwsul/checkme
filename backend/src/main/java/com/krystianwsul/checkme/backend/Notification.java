package com.krystianwsul.checkme.backend;

import java.util.List;

@SuppressWarnings("FieldCanBeLocal")
class Notification {
    private final List<String> registration_ids;
    private final Object data = new Data();
    private final String priority = "high";

    Notification(List<String> registrationIds) {
        this.registration_ids = registrationIds;
    }

    private static class Data {
        public final boolean refresh = true;
    }
}
