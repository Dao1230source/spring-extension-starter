package org.source.spring.doc;

import jdk.javadoc.doclet.Doclet;
import lombok.Data;
import lombok.Getter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
public class MyOptions {
    private String appDir;
    private List<String> subpackages = new ArrayList<>();
    private Set<String> classNames = HashSet.newHashSet(16);

    private AccessKindEnum accessKind = AccessKindEnum.PUBLIC;
    private Class<? extends Doclet> docletClass;

    @Getter
    public enum AccessKindEnum {
        PUBLIC("-public"), PACKAGE("-package"), PROTECTED("-protected"), PRIVATE("-private");
        private final String option;

        AccessKindEnum(String option) {
            this.option = option;
        }
    }
}
