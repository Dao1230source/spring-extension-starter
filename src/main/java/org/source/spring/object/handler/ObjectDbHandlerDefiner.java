package org.source.spring.object.handler;

import jakarta.validation.constraints.NotEmpty;
import org.source.spring.object.entity.ObjectEntityDefiner;

import java.util.Collection;
import java.util.List;

public interface ObjectDbHandlerDefiner<O extends ObjectEntityDefiner> {
    /**
     * object
     */
    O newObjectEntity();

    List<O> findObjects(@NotEmpty Collection<String> objectIds);

    void saveObjects(@NotEmpty Collection<O> objects);

    /**
     * 逻辑删除
     * 逻辑删除时只需设置 object.deleted = true 即可，relation和objectBody无需处理
     *
     * @param objectIds objectIds
     */
    void deleteObjects(@NotEmpty Collection<String> objectIds);

    /**
     * 物理删除
     *
     * @param objectIds objectIds
     */
    void removeObjects(@NotEmpty Collection<String> objectIds);
}