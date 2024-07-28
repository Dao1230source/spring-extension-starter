package org.source.spring.i18n.processor;

import org.source.spring.i18n.facade.data.Dict;
import org.source.spring.i18n.facade.param.Dict2Param;
import org.source.spring.i18n.facade.param.Dict3Param;
import org.source.spring.i18n.facade.param.Dict4Param;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public interface Processor<E extends Dict> {

    /**
     * 查找 by key
     *
     * @param param Dict3Param
     * @return value
     */
    Optional<E> findByKey(Dict3Param param);

    /**
     * 查找 by key
     *
     * @param params {@literal Collection<Dict3Param>}
     * @return value
     */
    default List<E> findByKeys(Collection<Dict3Param> params) {
        return params.stream().map(this::findByKey).filter(Optional::isPresent).map(Optional::get).toList();
    }

    /**
     * 查找 by group
     *
     * @param param Dict2Param
     * @return key-value
     */
    List<E> findByGroup(Dict2Param param);

    /**
     * 查找 by group
     *
     * @param params {@literal Collection<Dict2Param>}
     * @return {@literal Map<Dict2Param, List<E>>}
     */
    default Map<Dict2Param, List<E>> findByGroups(Collection<Dict2Param> params) {
        return params.stream().map(this::findByGroup)
                .flatMap(Collection::stream)
                .collect(Collectors.groupingBy(Dict2Param::new));
    }

    /**
     * 所有的语言
     *
     * @return findAllScopes
     */
    List<String> findAllScopes();

    /**
     * 新增
     *
     * @param param DictParam
     */
    int save(Dict4Param param);

    /**
     * 批量新增
     *
     * @param params {@literal Collection<DictParam>}
     */
    default int saveBatch(Collection<Dict4Param> params) {
        params.forEach(this::save);
        return params.size();
    }

    /**
     * remove by key
     *
     * @param param Dict3Param
     */
    int removeByKey(Dict3Param param);

    /**
     * remove by key
     *
     * @param params {@literal Collection<Dict3Param>}
     */
    default int removeByKeys(Collection<Dict3Param> params) {
        params.forEach(this::removeByKey);
        return -1;
    }

    /**
     * remove by group
     *
     * @param param Dict2Param
     */
    int removeByGroup(Dict2Param param);

    /**
     * remove by group
     *
     * @param params {@literal Collection<Dict2Param>}
     */
    default int removeByGroups(Collection<Dict2Param> params) {
        params.forEach(this::removeByGroup);
        return -1;
    }

}
