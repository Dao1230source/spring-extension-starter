package org.source.spring.doc.doclet;

import jdk.javadoc.doclet.DocletEnvironment;
import lombok.extern.slf4j.Slf4j;
import org.source.spring.doc.DocDataContainer;
import org.source.spring.doc.data.*;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import java.util.List;

@Slf4j
public class RequestDoclet extends AbstractDoclet {

    @Override
    protected void processDoc(DocletEnvironment env, DocDataContainer docDataContainer, DocData appDocData) {
        this.obtainScannedResult(env).forEach(type -> {
            RequestClassDocData requestClassDocData = new RequestClassDocData(env, type, appDocData.getId(), this.myOptions.isDocUseSimpleName());
            docDataContainer.add(requestClassDocData);
            List<ExecutableElement> executableElementList = type.getEnclosedElements().stream()
                    .filter(t -> ElementKind.METHOD.equals(t.getKind())).map(ExecutableElement.class::cast).toList();
            executableElementList.forEach(method -> {
                MethodDocData methodDocData = new MethodDocData(env, method, requestClassDocData.getId());
                docDataContainer.add(methodDocData);
                RequestDocData requestDocData = new RequestDocData(methodDocData, requestClassDocData.getId());
                docDataContainer.add(requestDocData);
                method.getParameters().forEach(p -> {
                    ParamDocData paramDocData = new ParamDocData(env, p, methodDocData.getId(), method);
                    docDataContainer.addWithAnnotation(paramDocData, p);
                });
                docDataContainer.add(VariableDocData.methodResult(method, methodDocData.getId()));
            });
        });
    }
}
