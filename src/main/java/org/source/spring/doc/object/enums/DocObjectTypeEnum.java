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
    METHOD(10001, "方法", AbstractDocProcessor.class, MethodDocData.class),
    PARAM(10002, "参数", AbstractDocProcessor.class, ParamDocData.class),
    FIELD(10003, "字段", AbstractDocProcessor.class, FieldDocData.class),
    VARIABLE(10004, "变量", AbstractDocProcessor.class, VariableDocData.class),
    ANNOTATION(10005, "注解", AbstractDocProcessor.class, AnnotationDocData.class),
    REQUEST(10006, "接口", AbstractDocProcessor.class, RequestDocData.class),
    /**
     * 类
     */
    CLASS(10007, "类", AbstractDocProcessor.class, ClassDocData.class),
    CLASS_REQUEST(10008, "接口请求类", AbstractDocProcessor.class, RequestClassDocData.class),
    CLASS_VARIABLE(10009, "变量类", AbstractDocProcessor.class, VariableClassDocData.class),
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