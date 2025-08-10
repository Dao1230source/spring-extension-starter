package org.source.spring.doc.doclet;

import jdk.javadoc.doclet.DocletEnvironment;
import lombok.extern.slf4j.Slf4j;
import org.source.spring.doc.DocDataContainer;
import org.source.spring.doc.data.*;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class RequestDoclet extends AbstractDoclet {

    @Override
    protected void processDoc(DocletEnvironment env, DocDataContainer docDataContainer, DocData appDocData) {
        AtomicInteger typeSorted = new AtomicInteger(0);
        this.obtainScannedResult(env).forEach(type -> {
            DocClassRequestData requestClassDocData = new DocClassRequestData(typeSorted.getAndIncrement(), env, type, appDocData.getFullName(), this.myOptions.isDocUseSimpleName());
            docDataContainer.add(requestClassDocData);
            List<ExecutableElement> executableElementList = type.getEnclosedElements().stream()
                    .filter(t -> ElementKind.METHOD.equals(t.getKind())).map(ExecutableElement.class::cast).toList();
            AtomicInteger methodSorted = new AtomicInteger(0);
            executableElementList.forEach(method -> {
                DocMethodData docMethodData = new DocMethodData(methodSorted.getAndIncrement(), env, method, requestClassDocData.getFullName());
                docDataContainer.add(docMethodData);
                DocRequestData docRequestData = new DocRequestData(methodSorted.getAndIncrement(), docMethodData, requestClassDocData.getFullName());
                docDataContainer.add(docRequestData);
                AtomicInteger paramSorted = new AtomicInteger(0);
                method.getParameters().forEach(p -> {
                    DocVariableParamData paramDocData = new DocVariableParamData(paramSorted.getAndIncrement(), env, p, docMethodData.getFullName(), method);
                    docDataContainer.add(paramDocData);
                    docDataContainer.add(DocVariableAnnotationData.obtainAnnotationDocDataList(p, paramDocData.getFullName()));
                });
                docDataContainer.add(new DocVariableReturnData(methodSorted.getAndIncrement(), method, docMethodData.getFullName()));
            });
        });
    }
}
