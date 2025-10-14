package org.source.spring.i18n;

import lombok.extern.slf4j.Slf4j;
import org.source.spring.i18n.facade.data.Dict;
import org.source.spring.i18n.facade.param.Dict2Param;
import org.source.spring.i18n.facade.param.Dict3Param;
import org.source.spring.i18n.facade.param.Dict4Param;
import org.source.utility.utils.Jsons;
import org.springframework.lang.Nullable;

import java.text.MessageFormat;
import java.util.*;
import java.util.stream.Collectors;

@SuppressWarnings("unchecked")
@Slf4j
public class I18nWrapper {

    private static I18nTemplate<?> staticTemplate;

    public static synchronized void setI18nTemplate(I18nTemplate<?> template) {
        I18nWrapper.staticTemplate = template;
    }

    public static <E extends Dict> @Nullable E findByKey(Dict3Param param) {
        return (E) staticTemplate.findByKey(param);
    }

    public static <E extends Dict> List<E> findByKeys(Collection<Dict3Param> params) {
        return (List<E>) staticTemplate.findByKeys(params);
    }

    public static <E extends Dict> List<E> findByGroup(Dict2Param param) {
        return (List<E>) staticTemplate.findByGroup(param);
    }

    public static <E extends Dict> Map<Dict2Param, List<E>> findByGroups(Collection<Dict2Param> params) {
        Map<Dict2Param, ? extends List<? extends Dict>> groups = staticTemplate.findByGroups(params);
        Map<Dict2Param, List<E>> map = HashMap.newHashMap(groups.size());
        groups.forEach((k, v) -> map.put(k, (List<E>) v));
        return map;
    }

    public static List<String> findAllLocales() {
        return staticTemplate.findAllLocales();
    }

    public static int save(Dict4Param param) {
        Dict byKey = staticTemplate.findByKey(param);
        if (Objects.isNull(byKey)) {
            return staticTemplate.save(param);
        }
        return 0;
    }

    public static int saveBatch(Collection<Dict4Param> params) {
        Set<Dict2Param> groups = params.stream().map(d -> (Dict2Param) d).collect(Collectors.toSet());
        Set<Dict4Param> existsDict = staticTemplate.findByGroups(groups).values().stream()
                .flatMap(Collection::stream).map(Dict4Param::new).collect(Collectors.toSet());
        Set<Dict4Param> toSaveDict = params.stream().filter(d -> !existsDict.contains(d)).collect(Collectors.toSet());
        log.info("i18n saveBatch:{}", Jsons.str(toSaveDict));
        return staticTemplate.saveBatch(toSaveDict);
    }

    public static int removeByKey(Dict3Param param) {
        return staticTemplate.removeByKey(param);
    }

    public static int removeByKeys(Collection<Dict3Param> params) {
        return staticTemplate.removeByKeys(params);
    }

    public static int removeByGroup(Dict2Param param) {
        return staticTemplate.removeByGroup(param);
    }

    public static int removeByGroups(Collection<Dict2Param> params) {
        return staticTemplate.removeByGroups(params);
    }

    public static <E extends Dict> String find(Locale locale, String group, String key) {
        E e = findByKey(new Dict3Param(locale.toLanguageTag(), group, key));
        String defaultLanguageTag = Locale.getDefault().toLanguageTag();
        if (!locale.equals(Locale.getDefault())) {
            e = findByKey(new Dict3Param(defaultLanguageTag, group, key));
        }
        if (Objects.nonNull(e)) {
            return e.getValue();
        }
        save(new Dict4Param(defaultLanguageTag, group, key, key));
        return key;
    }

    public static MessageFormat findAsMessageFormat(Locale locale, String group, String key) {
        String value = find(locale, group, key);
        return new MessageFormat(value, locale);
    }
}
