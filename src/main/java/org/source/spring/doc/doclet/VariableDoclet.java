package org.source.spring.doc.doclet;

import jdk.javadoc.doclet.DocletEnvironment;
import lombok.extern.slf4j.Slf4j;
import org.source.spring.doc.DocDataContainer;
import org.source.spring.doc.data.ClassDocData;
import org.source.spring.doc.data.DocData;
import org.source.spring.doc.data.FieldDocData;
import org.source.spring.doc.data.VariableClassDocData;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.VariableElement;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class VariableDoclet extends AbstractDoclet {

    @Override
    protected void processDoc(DocletEnvironment env, DocDataContainer docDataContainer, DocData appDocData) {
        this.obtainScannedResult(env).forEach(type -> {
            VariableClassDocData classDocData = new VariableClassDocData(env, type, appDocData.getId());
            docDataContainer.add(classDocData);
            List<VariableElement> variableElementList = type.getEnclosedElements().stream()
                    .filter(t -> ElementKind.FIELD.equals(t.getKind())).map(VariableElement.class::cast)
                    .toList();
            variableElementList.forEach(f -> {
                FieldDocData fieldDocData = new FieldDocData(env, f, classDocData.getId());
                docDataContainer.addWithAnnotation(fieldDocData, f);
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
                .filter(ClassDocData.class::isInstance).map(ClassDocData.class::cast)
                .map(ClassDocData::obtainSuperClassNames).flatMap(Collection::stream).collect(Collectors.toSet());
        clsNames.addAll(super.obtainExtraVariableClsNames(docDataList));
        return clsNames;
    }
}
