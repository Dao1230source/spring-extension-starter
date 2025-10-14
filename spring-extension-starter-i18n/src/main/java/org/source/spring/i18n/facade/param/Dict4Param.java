package org.source.spring.i18n.facade.param;

import jakarta.validation.constraints.NotEmpty;
import lombok.*;
import org.source.spring.i18n.facade.data.Dict;

@Builder
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Data
public class Dict4Param extends Dict3Param {
    @NotEmpty(message = "关键值(value)不能为空")
    protected String value;

    public Dict4Param(String code, String name, String value, String lang) {
        super(code, name, lang);
        this.value = value;
    }

    public Dict4Param(Dict dict) {
        super(dict.getScope(), dict.getGroup(), dict.getKey());
        this.value = dict.getValue();
    }

}
