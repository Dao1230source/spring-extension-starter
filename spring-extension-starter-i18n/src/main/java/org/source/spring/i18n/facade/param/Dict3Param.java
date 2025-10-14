package org.source.spring.i18n.facade.param;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.source.spring.i18n.facade.data.Dict;
import org.source.utility.constant.Constants;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Data
public class Dict3Param extends Dict2Param {
    @NotEmpty(message = "关键字(key)不能为空")
    protected String key;

    public Dict3Param(String scope, String group, String key) {
        this.scope = scope;
        this.group = group;
        this.key = key;
    }

    public Dict3Param(Dict dict) {
        this.scope = dict.getScope();
        this.group = dict.getGroup();
        this.key = dict.getKey();
    }

    public static String uniqueKey3(Dict3Param param) {
        return String.join(Constants.UNDERSCORE, param.getScope(), param.getGroup(), param.getKey());
    }

    public static Set<String> uniqueKeys3(Collection<Dict3Param> params) {
        return params.stream().map(Dict3Param::uniqueKey3).collect(Collectors.toSet());
    }
}
