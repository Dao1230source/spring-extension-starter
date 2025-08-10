package org.source.spring.doc.data;

import jdk.javadoc.doclet.DocletEnvironment;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.util.CollectionUtils;

import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Data
public class DocVariableData extends DocData {
    private String typeKind;
    private String typeName;

    public <E extends VariableElement> DocVariableData(Integer sorted, DocletEnvironment env, E variableElement, String parentId) {
        super(sorted, env, variableElement, parentId);
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