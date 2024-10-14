package org.source.spring.doc.doclet;

import jdk.javadoc.doclet.DocletEnvironment;
import lombok.extern.slf4j.Slf4j;
import org.source.spring.doc.data.ClassDocData;
import org.source.spring.doc.data.DocData;
import org.source.spring.doc.data.FieldDocData;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class VariableDoclet extends AbstractDoclet {

    @Override
    protected List<DocData> processDoc(DocletEnvironment env) {
        List<TypeElement> typeElementList = env.getIncludedElements().stream()
                .filter(k -> ElementKind.CLASS.equals(k.getKind())).map(TypeElement.class::cast)
                .filter(k -> myOptions.getClassNames().contains(k.getQualifiedName().toString()))
                .toList();
        List<DocData> docDataList = new ArrayList<>(16);
        typeElementList.forEach(type -> {
            ClassDocData classDocData = ClassDocData.of(env, type, false);
            docDataList.add(classDocData);
            List<VariableElement> variableElementList = type.getEnclosedElements().stream()
                    .filter(t -> ElementKind.FIELD.equals(t.getKind())).map(VariableElement.class::cast)
                    .toList();
            List<FieldDocData> fieldDocDataList = variableElementList.stream().map(f -> FieldDocData.of(env, f, type)).toList();
            docDataList.addAll(fieldDocDataList);
        });
        return docDataList;
    }
}
