package org.source.spring.object.definer.handler;

import org.source.spring.object.definer.entity.ObjectBodyDefiner;

import java.util.Collection;
import java.util.List;

public interface ObjectBodyHandler<B extends ObjectBodyDefiner> {

    /**
     * object body
     */
    B newObjectBodyEntity();

    List<B> findObjectBodies(Collection<String> objectIds);

    /**
     * 通常 object body 的唯一键是 objectId，但实际业务新增时中可能会使用其他唯一键来查询是否已存在数据
     * 这里单独为key设置一个泛型感觉增加了复杂度，考虑到绝大多数情况下都是String，而且即使有其他形式的key也可转换成String，就不单独设置了
     *
     * @param ks ks
     * @return list
     */
    List<B> findObjectBodyByDataIds(Collection<String> ks);

    void saveObjectBodies(Collection<B> objectBodies);

    void removeObjectBodies(Collection<String> objectIds);

}