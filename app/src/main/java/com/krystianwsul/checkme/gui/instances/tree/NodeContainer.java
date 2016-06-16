package com.krystianwsul.checkme.gui.instances.tree;

public interface NodeContainer {
    int displayedSize();

    Node getNode(int position);

    int getPosition(Node node);

    boolean expanded();
}
