package org.source.spring.doc.data;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.DeclaredType;
import java.util.HashMap;
import java.util.Map;

@EqualsAndHashCode(callSuper = true)
@Data
public class AnnotationDocData extends VariableDocData {

    private Map<String, String> annotationAttributes = HashMap.newHashMap(16);

    public void processAnnotation(AnnotationMirror anno) {
        DeclaredType annotationType = anno.getAnnotationType();
        this.setTypeKind(annotationType.getKind().name());
        this.setTypeName(annotationType.toString());
        anno.getElementValues().forEach((methodSymbol, attribute) ->
                annotationAttributes.put(methodSymbol.getSimpleName().toString(), attribute.getValue().toString()));
    }

}
