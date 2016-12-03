package com.krystianwsul.checkme.backend;

class Notification {
    public final String to;
    public final Object data = new Data();

    Notification(String to) {
        this.to = to;
    }

    private static class Data {
        public boolean refresh = true;
    }
}
