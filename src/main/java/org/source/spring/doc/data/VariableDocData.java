package org.source.spring.doc.data;

import com.sun.source.doctree.DocTree;
import jdk.javadoc.doclet.DocletEnvironment;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.source.spring.object.tree.ObjectNode;
import org.springframework.util.CollectionUtils;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Data
public class VariableDocData extends DocData {
    private String typeKind;
    private String typeName;

    public <E extends VariableElement> VariableDocData(DocletEnvironment env, E variableElement, String parentId) {
        super(env, variableElement, parentId);
        this.processVariable(variableElement);
    }

    public VariableDocData(ExecutableElement method, String parentId) {
        TypeMirror returnType = method.getReturnType();
        this.setName(DocTree.Kind.RETURN.tagName);
        this.setTypeKind(returnType.getKind().name());
        this.setTypeName(returnType.toString());
        this.processParentId(parentId);
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
        if (docData instanceof VariableDocData variableDocData) {
            this.typeKind = variableDocData.getTypeKind();
            this.typeName = variableDocData.getTypeName();
        }
    }

    public static boolean instanceOf(ObjectNode<String, DocData> docDataNode) {
        return docDataNode.getElement() instanceof VariableDocData;
    }
}
