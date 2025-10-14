package org.source.spring.i18n.facade.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Objects;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class Dict implements Serializable {
    private String scope;
    private String group;
    private String key;
    private String value;

    public <E extends Dict> Dict(E e) {
        this.key = e.getKey();
        this.value = e.getValue();
        this.scope = e.getScope();
        this.group = e.getGroup();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Dict dict = (Dict) o;
        return Objects.equals(scope, dict.scope) && Objects.equals(group, dict.group)
                && Objects.equals(key, dict.key) && Objects.equals(value, dict.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(scope, group, key, value);
    }
}
