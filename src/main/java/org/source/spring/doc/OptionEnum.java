package org.source.spring.doc;

import lombok.Getter;
import org.springframework.util.ClassUtils;

import java.util.List;

@Getter
public enum OptionEnum {
    CLASS_NAMES("-classNames", 1, "java class name", "") {
        @Override
        public void prepare(MyOptions myOptions, List<String> options) {
            myOptions.getClassNames().forEach(n -> {
                options.addAll(List.of(this.getName(), n));
                myOptions.getSubpackages().add(ClassUtils.getPackageName(n));
            });
        }

        @Override
        public boolean process(MyOptions myOptions, String option, List<String> arguments) {
            return myOptions.getClassNames().add(arguments.get(0));
        }
    },
    PROJECT_DIR("-projectDir", 1, "project directory", "") {
        @Override
        public void prepare(MyOptions myOptions, List<String> options) {
            options.addAll(List.of(this.getName(), myOptions.getProjectDir()));
        }

        @Override
        public boolean process(MyOptions myOptions, String option, List<String> arguments) {
            myOptions.setProjectDir(arguments.get(0));
            return true;
        }
    },
    USE_SIMPLE_NAME("-useSimpleName", 1, "DocData use simpleName", "") {
        @Override
        public void prepare(MyOptions myOptions, List<String> options) {
            options.addAll(List.of(this.getName(), String.valueOf(myOptions.isDocUseSimpleName())));
        }

        @Override
        public boolean process(MyOptions myOptions, String option, List<String> arguments) {
            myOptions.setDocUseSimpleName(Boolean.parseBoolean(arguments.get(0)));
            return true;
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
