package org.source.spring.doc.data;

import com.sun.source.doctree.DocTree;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeMirror;

@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Data
public class DocVariableReturnData extends DocVariableData {

    public DocVariableReturnData(Integer sorted, ExecutableElement element, String parentName) {
        super(sorted, DocTree.Kind.RETURN.tagName, null, null, parentName);
        TypeMirror returnType = element.getReturnType();
        this.setTypeKind(returnType.getKind().name());
        this.setTypeName(returnType.toString());
    }
}