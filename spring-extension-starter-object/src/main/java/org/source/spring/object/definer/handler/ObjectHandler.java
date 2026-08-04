package org.source.spring.object.definer.handler;

import org.source.spring.object.definer.entity.ObjectDefiner;

import java.util.Collection;
import java.util.List;

public interface ObjectHandler<O extends ObjectDefiner> {
    /**
     * object
     */
    O newObject();

    List<O> find(Collection<String> objectIds);

    void save(Collection<O> objects);

    /**
     * 逻辑删除
     * 逻辑删除时只需设置 object.deleted = true 即可，relation和objectBody无需处理
     *
     * @param objectIds objectIds
     */
    void delete(Collection<String> objectIds);

    /**
     * 物理删除
     *
     * @param objectIds objectIds
     */
    void remove(Collection<String> objectIds);
}