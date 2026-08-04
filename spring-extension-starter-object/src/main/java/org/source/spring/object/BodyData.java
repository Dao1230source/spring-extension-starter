package org.source.spring.object;

import lombok.Data;
import org.jspecify.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.Objects;

@Data
public class BodyData {

    private String name;

    private String objectId;
    /**
     * relation
     */
    private @Nullable String parentObjectId;
    private String sorted;
    private @Nullable Integer relationType;

    private String createUser;
    private LocalDateTime createTime;
    private String updateUser;
    private LocalDateTime updateTime;

    @Override
    public boolean equals(@Nullable Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        BodyData that = (BodyData) o;
        return Objects.equals(getName(), that.getName()) && Objects.equals(getSorted(), that.getSorted());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName(), getSorted());
    }
}
