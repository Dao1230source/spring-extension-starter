package org.source.spring.doc.data;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.util.CollectionUtils;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.type.DeclaredType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Data
public class DocVariableAnnotationData extends DocVariableData {

    private Map<String, String> annotationAttributes = HashMap.newHashMap(16);

    public DocVariableAnnotationData(Integer sorted, AnnotationMirror anno, String parentName) {
        super(sorted, null, anno.getAnnotationType().asElement(), parentName);
        DeclaredType annotationType = anno.getAnnotationType();
        this.setTypeKind(annotationType.getKind().name());
        this.setTypeName(annotationType.toString());
        anno.getElementValues().forEach((methodSymbol, attribute) ->
                annotationAttributes.put(methodSymbol.getSimpleName().toString(), attribute.getValue().toString()));
    }

    public static <E extends Element> List<DocVariableAnnotationData> obtainAnnotationDocDataList(E element, String parentId) {
        List<? extends AnnotationMirror> annotationMirrors = element.getAnnotationMirrors();
        if (CollectionUtils.isEmpty(annotationMirrors)) {
            return List.of();
        }
        AtomicInteger annotationSorted = new AtomicInteger(0);
        return element.getAnnotationMirrors().stream().map(a -> new DocVariableAnnotationData(annotationSorted.getAndIncrement(), a, parentId)).toList();
    }

}