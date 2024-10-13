package org.source.spring.doc.data;

import jdk.javadoc.doclet.DocletEnvironment;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.lang.model.element.ExecutableElement;
import java.util.ArrayList;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class MethodDocData extends DocData implements Path {
    private List<String> paths;
    private List<String> requestMethods;

    private List<VariableDocData> params = new ArrayList<>();
    private VariableDocData returnValue;

    public static MethodDocData of(DocletEnvironment env, ExecutableElement method, String viewId) {
        MethodDocData methodDocData = new MethodDocData();
        methodDocData.setKey(method.toString());
        methodDocData.processParentId(viewId);
        methodDocData.processMapping(method);
        methodDocData.processComment(env, method);
        return methodDocData;
    }

}
