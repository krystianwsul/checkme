package com.krystianwsul.checkme.backend;

class Notification {
    private final String to;
    private final Object data = new Data();

    Notification(String to) {
        this.to = to;
    }

    private static class Data {
        public final boolean refresh = true;
    }
}
