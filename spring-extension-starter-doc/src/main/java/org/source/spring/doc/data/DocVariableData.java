package org.source.spring.doc.data;

import jdk.javadoc.doclet.DocletEnvironment;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.util.CollectionUtils;

import javax.lang.model.element.Element;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Data
public class DocVariableData extends DocData {
    private String typeKind;
    private String typeName;

    public DocVariableData(Integer sorted, String name, String title, String text, String parentName) {
        super(sorted, name, title, text, parentName);
    }

    public <E extends Element> DocVariableData(Integer sorted, DocletEnvironment env, E element, String parentName) {
        super(sorted, env, element, parentName);
    }

    public <E extends VariableElement> DocVariableData(Integer sorted, DocletEnvironment env, E variableElement, String parentName) {
        super(sorted, env, variableElement, parentName);
        this.processVariable(variableElement);
    }

    /**
     * @param variableElement variableElement
     */
    protected void processVariable(VariableElement variableElement) {
        TypeMirror type = variableElement.asType();
        this.setTypeKind(type.getKind().name());
        String name = type.toString();
        if (!CollectionUtils.isEmpty(type.getAnnotationMirrors())) {
            name = name.replaceAll("@.*\\)\\s", "");
        }
        this.setTypeName(name);
    }

    public boolean baseType() {
        TypeKind type = TypeKind.valueOf(this.typeKind);
        return type.isPrimitive() || type.equals(TypeKind.VOID) || String.class.getName().equals(typeName);
    }

    public boolean notBaseType() {
        return !baseType();
    }

    @Override
    public <D extends DocData> void merge(D docData) {
        super.merge(docData);
        if (docData instanceof DocVariableData docVariableData) {
            this.typeKind = docVariableData.getTypeKind();
            this.typeName = docVariableData.getTypeName();
        }
    }
}