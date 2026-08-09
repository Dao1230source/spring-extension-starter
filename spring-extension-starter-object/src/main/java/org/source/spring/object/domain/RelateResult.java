package org.source.spring.object.domain;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.source.spring.object.definer.entity.RelationDefiner;
import org.source.spring.object.enums.RelateTypeEnum;

@EqualsAndHashCode(callSuper = true)
@Data
public class RelateResult<R extends RelationDefiner> extends Relations {
    private boolean canRelate;
    private String relateResult;

    private R relation;

    public boolean isAdd() {
        return RelateTypeEnum.ADD.getType().equals(this.getRelateType());
    }

    public boolean isDelete() {
        return RelateTypeEnum.DELETE.getType().equals(this.getRelateType());
    }

    public static <R extends RelationDefiner> RelateResult<R> of(Relations r) {
        RelateResult<R> result = new RelateResult<>();
        result.setObjectId(r.getObjectId());
        result.setParentObjectId(r.getParentObjectId());
        result.setType(r.getType());
        result.setSort(r.getSort());
        result.setRelateType(r.getRelateType());
        return result;
    }
}
