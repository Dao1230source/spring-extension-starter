package org.source.spring.i18n.facade.param;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.source.spring.i18n.I18nWrapper;
import org.source.spring.i18n.facade.data.Dict;
import org.source.utility.constant.Constants;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Data
public class Dict2Param extends Dict1Param {
    @NotEmpty(message = "分组(group)不能为空")
    protected String group;

    public Dict2Param(Dict dict) {
        this.scope = dict.getScope();
        this.group = dict.getGroup();
    }

    public Dict2Param(String scope, String group) {
        super(scope);
        this.group = group;
    }

    public static String uniqueKey2(Dict2Param param) {
        return String.join(Constants.UNDERSCORE, param.getScope(), param.getGroup());
    }

    public static Set<String> uniqueKeys2(Collection<Dict2Param> params) {
        return params.stream().map(Dict2Param::uniqueKey2).collect(Collectors.toSet());
    }

    public static Set<String> uniqueKeys3WhenRemove(Dict2Param param) {
        return I18nWrapper.findByGroup(param).stream().map(Dict3Param::new).map(Dict3Param::uniqueKey3).collect(Collectors.toSet());
    }

    public static Set<String> uniqueKeys3WhenRemove(Collection<Dict2Param> params) {
        return I18nWrapper.findByGroups(params).values().stream().flatMap(Collection::stream)
                .map(Dict3Param::new).map(Dict2Param::uniqueKey2).collect(Collectors.toSet());
    }
}
