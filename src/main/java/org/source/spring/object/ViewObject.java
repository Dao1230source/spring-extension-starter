package org.source.spring.object;

import org.source.utility.tree.identity.AbstractNode;

public interface ViewObject<E extends ViewObjectItem, N extends AbstractNode<String, E, N>> {
    /**
     * 视图数据
     *
     * @return {@code N extends AbstractNode<I, E, N>} 节点类型
     */
    N getViewData();
}
