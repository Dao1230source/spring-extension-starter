package org.source.spring.object;

import org.source.utility.tree.DefaultNode;

public interface ViewData {
    DefaultNode<String, ViewItemData> getViewData();
}
