package org.source.spring.object.processor;

import jakarta.validation.constraints.NotEmpty;
import org.source.spring.object.AbstractValue;
import org.source.spring.object.entity.ObjectBodyEntityDefiner;

import java.util.Collection;
import java.util.List;

public interface ObjectBodyDbProcessorDefiner<B extends ObjectBodyEntityDefiner, V extends AbstractValue, K> {

    /**
     * object body
     */
    B newObjectBodyEntity();

    List<B> findObjectBodies(@NotEmpty Collection<String> objectIds);

    /**
     * 通常 object body 的唯一键是 objectId，但实际业务新增时中可能会使用其他唯一键来查询是否已存在数据
     *
     * @param ks ks
     * @return list
     */
    List<B> findObjectBodiesByKeys(@NotEmpty Collection<K> ks);

    K objectBodyToKey(B b);

    K valueToKey(V v);

    K valueToParentKey(V v);

    void saveObjectBodies(@NotEmpty Collection<B> objectBodies);

    void removeObjectBodies(@NotEmpty Collection<String> objectIds);

}