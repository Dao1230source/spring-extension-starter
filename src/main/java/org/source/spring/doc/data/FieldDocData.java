package org.source.spring.doc.data;

import jdk.javadoc.doclet.DocletEnvironment;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.lang.model.element.VariableElement;

@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Data
public class FieldDocData extends VariableDocData {

    public FieldDocData(DocletEnvironment env, VariableElement variableElement, String parentId) {
        super(env, variableElement, parentId);
    }
}
