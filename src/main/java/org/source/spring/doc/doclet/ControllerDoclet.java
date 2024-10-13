package org.source.spring.doc.doclet;

import jdk.javadoc.doclet.DocletEnvironment;
import lombok.extern.slf4j.Slf4j;
import org.source.spring.doc.data.*;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RestController;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
public class ControllerDoclet extends AbstractDoclet {

    @Override
    protected List<DocData> processDoc(DocletEnvironment env) {
        List<TypeElement> typeElementList = env.getIncludedElements().stream()
                .filter(k -> ElementKind.CLASS.equals(k.getKind())).map(TypeElement.class::cast)
                .filter(k -> myOptions.getClassNames().contains(k.getQualifiedName().toString()))
                .filter(k -> Objects.nonNull(k.getAnnotation(RestController.class))
                        || Objects.nonNull(k.getAnnotation(Controller.class)))
                .toList();
        List<DocData> docDataList = new ArrayList<>(16);
        typeElementList.forEach(type -> {
            ClassDocData classDocData = ClassDocData.of(env, type);
            docDataList.add(classDocData);
            List<ExecutableElement> executableElementList = type.getEnclosedElements().stream()
                    .filter(t -> ElementKind.METHOD.equals(t.getKind())).map(ExecutableElement.class::cast).toList();
            executableElementList.forEach(method -> {
                RequestDocData requestDocData = RequestDocData.of(classDocData.getId());
                docDataList.add(requestDocData);
                MethodDocData methodDocData = MethodDocData.of(env, method, requestDocData.getId());
                docDataList.add(methodDocData);
                List<ParamDocData> params = method.getParameters().stream().map(p -> ParamDocData.of(env, p, method)).toList();
                docDataList.addAll(params);
                this.extraClsNames.addAll(params.stream().filter(VariableDocData::notBaseType)
                        .map(VariableDocData::getTypeName).collect(Collectors.toSet()));
                VariableDocData returnValue = VariableDocData.methodReturn(method, methodDocData.getId());
                if (returnValue.notBaseType()) {
                    this.extraClsNames.add(returnValue.getTypeName());
                }
                docDataList.add(returnValue);
            });
        });
        return docDataList;
    }
}
