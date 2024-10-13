package org.source.spring.doc.data;

import jdk.javadoc.doclet.DocletEnvironment;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;

@EqualsAndHashCode(callSuper = true)
@Data
public class FieldDocData extends VariableDocData {

    public static FieldDocData of(DocletEnvironment env, VariableElement variableElement, TypeElement cls) {
        FieldDocData paramDocData = new FieldDocData();
        paramDocData.processVariable(env, variableElement, cls);
        return paramDocData;
    }

}
