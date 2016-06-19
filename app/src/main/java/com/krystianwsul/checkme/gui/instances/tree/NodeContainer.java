package com.krystianwsul.checkme.gui.instances.tree;

import java.util.List;

public interface NodeContainer {
    int displayedSize();
    Node getNode(int position);
    int getPosition(Node node);
    boolean expanded();

    void update();

    List<Node> getSelectedChildren();

    TreeNodeCollection getTreeNodeCollection();
}
