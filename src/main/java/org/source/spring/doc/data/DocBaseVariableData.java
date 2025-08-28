package org.source.spring.doc.data;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.source.spring.doc.object.enums.DocRelationTypeEnum;

/**
 * 基础变量
 * <pre>
 * 包含基础类型的方法参数，类的属性
 * 可以和sql表字段对应
 * </pre>
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class DocBaseVariableData extends DocData {

    public <V extends DocVariableData> DocBaseVariableData(V paramData, String parentName) {
        super(0, paramData.getName(), paramData.getTitle(), paramData.getText(), parentName);
        this.setRelationType(DocRelationTypeEnum.BASE_VARIABLE.getType());
    }
}