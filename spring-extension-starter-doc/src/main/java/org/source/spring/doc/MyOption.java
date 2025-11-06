package org.source.spring.doc;

import jdk.javadoc.doclet.Doclet;

import java.util.List;

public abstract class MyOption implements Doclet.Option {
    private final String name;
    private final int argCount;
    private final String desc;
    private final String parameters;

    protected MyOption(String name, int argCount, String desc, String parameters) {
        this.name = name;
        this.argCount = argCount;
        this.desc = desc;
        this.parameters = parameters;
    }

    protected MyOption(OptionEnum option) {
        this(option.getName(), option.getArgCount(), option.getDesc(), option.getParameters());
    }

    @Override
    public int getArgumentCount() {
        return argCount;
    }

    @Override
    public String getDescription() {
        return desc;
    }

    @Override
    public Kind getKind() {
        return Kind.STANDARD;
    }

    @Override
    public List<String> getNames() {
        return List.of(name);
    }

    @Override
    public String getParameters() {
        return parameters;
    }
}
