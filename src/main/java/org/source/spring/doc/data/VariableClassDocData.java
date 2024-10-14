package org.source.spring.doc.data;

import jdk.javadoc.doclet.DocletEnvironment;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.lang.model.element.TypeElement;

@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Data
public class VariableClassDocData extends ClassDocData {

    public <E extends TypeElement> VariableClassDocData(DocletEnvironment env, E type, String parentId) {
        super(env, type, parentId);
    }
}
