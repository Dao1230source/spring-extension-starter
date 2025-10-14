package org.source.spring.i18n.facade.param;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class Dict1Param {
    @NotEmpty(message = "范围(scope)不能为空")
    protected String scope;

    public static String key(@NotNull Dict1Param param) {
        return param.getScope();
    }
}
