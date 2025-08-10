package org.source.spring.doc.data;

import jdk.javadoc.doclet.DocletEnvironment;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.lang.model.element.TypeElement;

@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Data
public class DocClassVariableData extends DocClassData {

    public <E extends TypeElement> DocClassVariableData(Integer sorted, DocletEnvironment env, E type, String parentId) {
        super(sorted, env, type, parentId);
    }
}
