package org.source.spring.doc;

import lombok.Getter;
import org.springframework.util.ClassUtils;

import java.util.List;

@Getter
public enum OptionEnum {
    JAVA_NAMES("-className", 1, "java class name", "") {
        @Override
        public void prepare(MyOptions myOptions, List<String> options) {
            myOptions.getClassNames().forEach(n -> {
                options.addAll(List.of(this.getName(), n));
                String packageName = ClassUtils.getPackageName(n);
                if (!myOptions.getSubpackages().contains(packageName)) {
                    myOptions.getSubpackages().add(packageName);
                }
            });
        }

        @Override
        public boolean process(MyOptions myOptions, String option, List<String> arguments) {
            return myOptions.getClassNames().add(arguments.get(0));
        }
    },
    ;
    private final String name;
    private final int argCount;
    private final String desc;
    private final String parameters;

    OptionEnum(String name, int argCount, String desc, String parameters) {
        this.name = name;
        this.argCount = argCount;
        this.desc = desc;
        this.parameters = parameters;
    }

    public abstract void prepare(MyOptions myOptions, List<String> options);

    public abstract boolean process(MyOptions myOptions, String option, List<String> arguments);
}
