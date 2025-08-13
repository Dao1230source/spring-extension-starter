package org.source.spring.doc.object.enums;

import lombok.Getter;
import org.source.spring.doc.data.*;
import org.source.spring.doc.object.AbstractDocProcessor;
import org.source.spring.object.enums.ObjectTypeDefiner;

@Getter
public enum DocObjectTypeEnum implements ObjectTypeDefiner {
    /**
     * 接口文档
     */
    DOC(10000, "doc", AbstractDocProcessor.class, DocData.class),
    DOC_BASE_VARIABLE(10000, "doc", AbstractDocProcessor.class, DocBaseVariableData.class),
    CLASS(10010, "类", AbstractDocProcessor.class, DocClassData.class),
    CLASS_REQUEST(10011, "接口请求类", AbstractDocProcessor.class, DocClassRequestData.class),
    CLASS_VARIABLE(10012, "变量类", AbstractDocProcessor.class, DocClassVariableData.class),
    METHOD(10020, "方法", AbstractDocProcessor.class, DocMethodData.class),
    VARIABLE(10030, "变量", AbstractDocProcessor.class, DocVariableData.class),
    VARIABLE_PARAM(10031, "参数", AbstractDocProcessor.class, DocVariableParamData.class),
    VARIABLE_FIELD(10032, "字段", AbstractDocProcessor.class, DocVariableFieldData.class),
    VARIABLE_RETURN(10033, "返回值", AbstractDocProcessor.class, DocVariableReturnData.class),
    ANNOTATION(10040, "注解", AbstractDocProcessor.class, DocVariableAnnotationData.class),
    REQUEST(10050, "接口", AbstractDocProcessor.class, DocRequestData.class),
    ;
    private final Integer type;
    private final String desc;
    @SuppressWarnings("rawtypes")
    private final Class<? extends AbstractDocProcessor> objectProcessor;
    private final Class<? extends DocData> valueClass;

    @SuppressWarnings("rawtypes")
    DocObjectTypeEnum(Integer type,
                      String desc,
                      Class<? extends AbstractDocProcessor> objectProcessor,
                      Class<? extends DocData> valueClass) {
        this.type = type;
        this.desc = desc;
        this.valueClass = valueClass;
        this.objectProcessor = objectProcessor;
    }
}