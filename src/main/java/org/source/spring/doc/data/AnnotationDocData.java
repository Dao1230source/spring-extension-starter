package org.source.spring.doc.data;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.source.spring.doc.enums.DocRelationTypeEnum;
import org.springframework.util.CollectionUtils;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.type.DeclaredType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Data
public class AnnotationDocData extends VariableDocData {

    private Map<String, String> annotationAttributes = HashMap.newHashMap(16);

    public AnnotationDocData(AnnotationMirror anno, String parentId) {
        this.setRelationType(DocRelationTypeEnum.BELONG.getType());
        DeclaredType annotationType = anno.getAnnotationType();
        this.setName(annotationType.asElement().getSimpleName().toString());
        this.processParentId(parentId);
        this.setTypeKind(annotationType.getKind().name());
        this.setTypeName(annotationType.toString());
        anno.getElementValues().forEach((methodSymbol, attribute) ->
                annotationAttributes.put(methodSymbol.getSimpleName().toString(), attribute.getValue().toString()));
    }

    public static <E extends Element> List<AnnotationDocData> obtainAnnotationDocDataList(E element, String parentId) {
        List<? extends AnnotationMirror> annotationMirrors = element.getAnnotationMirrors();
        if (CollectionUtils.isEmpty(annotationMirrors)) {
            return List.of();
        }
        return element.getAnnotationMirrors().stream().map(a -> new AnnotationDocData(a, parentId)).toList();
    }

}
