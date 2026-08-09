package org.source.spring.object;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import org.jspecify.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * object、relation、body等字段都是中间字段，不会保存到 body 表的 data 字段
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@Data
public class BodyData {

    private String name;

    /*
     * object
     */
    private String objectId;
    private @Nullable String spaceId;
    private @Nullable Integer type;
    /*
     * relation
     */
    private @Nullable String parentObjectId;
    private @Nullable String sort;
    private @Nullable Integer relationType;

    /*
     * body
     */
    private @Nullable String createUser;
    private @Nullable LocalDateTime createTime;
    private @Nullable String updateUser;
    private @Nullable LocalDateTime updateTime;

    @Override
    public boolean equals(@Nullable Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        BodyData that = (BodyData) o;
        return Objects.equals(getName(), that.getName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName());
    }
}
