package org.source.spring.doc.data;

import jdk.javadoc.doclet.DocletEnvironment;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.lang.model.element.TypeElement;
import java.util.ArrayList;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class ClassDocData extends DocData implements Path {
    private List<String> paths;
    private List<String> requestMethods;
    private List<MethodDocData> methodDocDataList = new ArrayList<>(16);

    public static ClassDocData of(DocletEnvironment env, TypeElement type, boolean simpleName) {
        ClassDocData classDocData = new ClassDocData();
        String key = simpleName ? type.getSimpleName().toString() : type.getQualifiedName().toString();
        classDocData.setKey(key);
        classDocData.processRequestMapping(type);
        classDocData.processComment(env, type);
        classDocData.processParentId(null);
        return classDocData;
    }
}
