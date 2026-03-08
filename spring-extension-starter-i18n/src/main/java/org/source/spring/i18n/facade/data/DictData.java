package org.source.spring.i18n.facade.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Objects;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class DictData implements Serializable {
    private String scope;
    private String group;
    private String key;
    private String value;

    public <E extends DictData> DictData(E e) {
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
        DictData dictData = (DictData) o;
        return Objects.equals(scope, dictData.scope) && Objects.equals(group, dictData.group)
                && Objects.equals(key, dictData.key) && Objects.equals(value, dictData.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(scope, group, key, value);
    }
}
