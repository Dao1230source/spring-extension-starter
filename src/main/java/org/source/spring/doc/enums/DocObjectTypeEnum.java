package org.source.spring.doc.enums;

import lombok.Getter;
import org.source.spring.doc.data.*;
import org.source.spring.doc.processor.AbstractDocProcessor;
import org.source.spring.object.AbstractValue;
import org.source.spring.object.data.ObjectFullData;
import org.source.spring.object.entity.ObjectBodyEntityIdentity;
import org.source.spring.object.enums.ObjectTypeIdentity;
import org.source.utility.utils.Enums;
import org.source.utility.utils.Jsons;

@Getter
public enum DocObjectTypeEnum implements ObjectTypeIdentity {
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
    private final Class<? extends AbstractValue> valueClass;

    @SuppressWarnings("rawtypes")
    DocObjectTypeEnum(Integer type,
                      String desc,
                      Class<? extends AbstractDocProcessor> objectProcessor,
                      Class<? extends AbstractValue> valueClass) {
        this.type = type;
        this.desc = desc;
        this.valueClass = valueClass;
        this.objectProcessor = objectProcessor;
    }

    public static DocObjectTypeEnum getByType(Integer type) {
        return Enums.getEnum(DocObjectTypeEnum.class, DocObjectTypeEnum::getType, type);
    }

    public static DocObjectTypeEnum getByValueClass(Class<? extends AbstractValue> clz) {
        return Enums.getEnum(DocObjectTypeEnum.class, DocObjectTypeEnum::getValueClass, clz);
    }

    @Override
    public <V extends AbstractValue> ObjectFullData<V> parse(ObjectBodyEntityIdentity entity) {
        ObjectFullData<V> fullData = new ObjectFullData<>();
        fullData.setType(this.getType());
        fullData.setName(entity.getName());
        fullData.setObjectId(entity.getObjectId());
        @SuppressWarnings("unchecked")
        V value = (V) Jsons.obj(entity.getValue(), this.getValueClass());
        fullData.setValue(value);
        return fullData;
    }

    public static <V extends AbstractValue> V toObjectValue(Integer type, ObjectBodyEntityIdentity entity) {
        DocObjectTypeEnum anEnum = getByType(type);
        @SuppressWarnings("unchecked")
        V v = (V) Jsons.obj(entity.getValue(), anEnum.getValueClass());
        v.setObjectId(entity.getObjectId());
        return v;
    }
}