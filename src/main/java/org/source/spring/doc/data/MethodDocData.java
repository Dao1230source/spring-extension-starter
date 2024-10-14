package org.source.spring.doc.data;

import jdk.javadoc.doclet.DocletEnvironment;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.source.utility.constant.Constants;
import org.source.utility.tree.DefaultNode;
import org.springframework.util.CollectionUtils;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Data
public class MethodDocData extends DocData implements Path {
    private List<String> paths;
    private List<String> requestMethods;

    private List<VariableDocData> params = new ArrayList<>();
    private VariableDocData returnValue;

    public MethodDocData(DocletEnvironment env, ExecutableElement method, String parentId) {
        this.setName(methodName(method));
        this.processParentId(parentId);
        this.processMapping(method);
        this.processComment(env, method);
    }

    protected static String methodName(ExecutableElement method) {
        StringBuilder sb = new StringBuilder();
        sb.append(method.getSimpleName());
        List<? extends VariableElement> parameters = method.getParameters();
        if (!CollectionUtils.isEmpty(parameters)) {
            sb.append("(");
            sb.append(parameters.stream().map(VariableElement::getSimpleName).collect(Collectors.joining(Constants.COMMA)));
            sb.append(")");
        }
        return sb.toString();
    }

    public static boolean instanceOf(DefaultNode<String, DocData> docDataNode) {
        return docDataNode.getElement() instanceof MethodDocData;
    }

}
