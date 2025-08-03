package org.source.spring.doc.data;

import jdk.javadoc.doclet.DocletEnvironment;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.source.utility.utils.Streams;
import org.springframework.util.CollectionUtils;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Data
public class ClassDocData extends DocData {

    private String superClass;
    private List<String> interfaceClasses;

    protected <E extends TypeElement> void processSuperClass(E type) {
        this.superClass = type.getSuperclass().toString();
        this.interfaceClasses = Streams.of(type.getInterfaces()).map(TypeMirror::toString).toList();
    }

    protected <E extends TypeElement> ClassDocData(DocletEnvironment env, E type, String parentId) {
        super(env, type, parentId);
        this.processSuperClass(type);
    }

    @Override
    protected <E extends Element> void processName(E element) {
        this.setName(((TypeElement) element).getQualifiedName().toString());
    }

    public List<String> obtainSuperClassNames() {
        List<String> superClassNames = new ArrayList<>(8);
        if (Objects.nonNull(this.superClass)) {
            superClassNames.add(this.superClass);
        }
        if (!CollectionUtils.isEmpty(this.interfaceClasses)) {
            superClassNames.addAll(this.interfaceClasses);
        }
        return superClassNames;
    }
}
