package org.source.spring.doc.data;

import jdk.javadoc.doclet.DocletEnvironment;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.source.utility.constant.Constants;
import org.springframework.util.CollectionUtils;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Data
public class DocMethodData extends DocData implements Path {
    private List<String> paths;
    private List<String> requestMethods;

    private List<DocVariableData> params = new ArrayList<>();
    private DocVariableData returnValue;

    public DocMethodData(Integer sorted, DocletEnvironment env, ExecutableElement method, String parentId) {
        super(sorted, env, method, parentId);
        this.processMapping(method);
    }

    @Override
    protected <E extends Element> String obtainName(E element) {
        return methodName((ExecutableElement) element);
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
}