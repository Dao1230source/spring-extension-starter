package org.source.spring.doc.data;

import jdk.javadoc.doclet.DocletEnvironment;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.lang.model.element.VariableElement;

@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Data
public class DocVariableFieldData extends DocVariableData {

    public DocVariableFieldData(Integer sorted, DocletEnvironment env, VariableElement variableElement, String parentId) {
        super(sorted, env, variableElement, parentId);
    }
}
