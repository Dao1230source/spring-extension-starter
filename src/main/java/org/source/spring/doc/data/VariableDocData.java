package org.source.spring.doc.data;

import com.sun.source.doctree.DocTree;
import jdk.javadoc.doclet.DocletEnvironment;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.util.CollectionUtils;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class VariableDocData extends DocData {
    private String typeKind;
    private String typeName;
    private List<AnnotationDocData> annotationList;

    public List<AnnotationDocData> processAnnotation(VariableElement variableElement) {
        return variableElement.getAnnotationMirrors().stream().map(k -> {
            AnnotationDocData annotationDocData = new AnnotationDocData();
            annotationDocData.processAnnotation(k);
            return annotationDocData;
        }).toList();
    }

    /**
     * @param variableElement variableElement
     */
    public void processVariable(VariableElement variableElement) {
        this.setKey(variableElement.getSimpleName().toString());
        TypeMirror type = variableElement.asType();
        this.setTypeKind(type.getKind().name());
        String name = type.toString();
        if (!CollectionUtils.isEmpty(type.getAnnotationMirrors())) {
            name = name.replaceAll("@.*\\)\\s", "");
        }
        this.setTypeName(name);
    }

    public void processVariable(DocletEnvironment env, VariableElement variableElement, ExecutableElement method, String methodId) {
        this.processVariable(variableElement);
        this.processComment(env, method);
        this.setAnnotationList(this.processAnnotation(variableElement));
        this.processParentId(methodId);
    }

    public void processVariable(DocletEnvironment env, VariableElement variableElement, TypeElement cls) {
        this.processVariable(variableElement);
        this.processComment(env, variableElement);
        this.setAnnotationList(this.processAnnotation(variableElement));
        this.processParentId(cls.getQualifiedName().toString());
    }

    public boolean isBaseType() {
        TypeKind type = TypeKind.valueOf(this.typeKind);
        return type.isPrimitive() || type.equals(TypeKind.VOID) || String.class.getName().equals(typeName);
    }

    public boolean notBaseType() {
        return !isBaseType();
    }


    public static VariableDocData methodReturn(ExecutableElement method, String parentId) {
        VariableDocData variableDocData = new VariableDocData();
        TypeMirror returnType = method.getReturnType();
        variableDocData.setKey(DocTree.Kind.RETURN.tagName);
        variableDocData.setTypeKind(returnType.getKind().name());
        variableDocData.setTypeName(returnType.toString());
        variableDocData.processParentId(parentId);
        return variableDocData;
    }

}
