package org.source.spring.doc.doclet;

import jdk.javadoc.doclet.DocletEnvironment;
import lombok.extern.slf4j.Slf4j;
import org.source.spring.doc.DocDataContainer;
import org.source.spring.doc.data.DocClassData;
import org.source.spring.doc.data.DocClassVariableData;
import org.source.spring.doc.data.DocData;
import org.source.spring.doc.data.DocVariableFieldData;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.VariableElement;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j
public class VariableDoclet extends AbstractDoclet {

    @Override
    protected void processDoc(DocletEnvironment env, DocDataContainer docDataContainer, DocData appDocData) {
        AtomicInteger typeSorted = new AtomicInteger(0);
        this.obtainScannedResult(env).forEach(type -> {
            DocClassVariableData classDocData = new DocClassVariableData(typeSorted.getAndIncrement(), env, type,
                    appDocData.getFullName(), this.myOptions.isDocUseSimpleName());
            docDataContainer.add(classDocData);
            List<VariableElement> variableElementList = type.getEnclosedElements().stream()
                    .filter(t -> ElementKind.FIELD.equals(t.getKind())).map(VariableElement.class::cast).toList();
            AtomicInteger variableSorted = new AtomicInteger(0);
            variableElementList.forEach(f -> {
                DocVariableFieldData fieldDocData = new DocVariableFieldData(variableSorted.getAndIncrement(), env, f, classDocData.getFullName());
                docDataContainer.addVariableData(fieldDocData, f, appDocData);
            });
        });
    }

    /**
     * variable 的父类也按 variable处理
     *
     * @param docDataList docDataContainer
     * @return classNames
     */
    @Override
    protected Set<String> obtainExtraSuperClsNames(List<DocData> docDataList) {
        return Set.of();
    }

    @Override
    protected Set<String> obtainExtraVariableClsNames(List<DocData> docDataList) {
        Set<String> clsNames = docDataList.stream()
                .filter(DocClassData.class::isInstance).map(DocClassData.class::cast)
                .map(DocClassData::obtainSuperClassNames).flatMap(Collection::stream).collect(Collectors.toSet());
        clsNames.addAll(super.obtainExtraVariableClsNames(docDataList));
        return clsNames;
    }
}
