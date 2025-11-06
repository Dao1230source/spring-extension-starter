package org.source.spring.doc;

import jakarta.validation.constraints.NotEmpty;
import jdk.javadoc.doclet.Doclet;
import lombok.Data;
import lombok.Getter;

import javax.tools.DocumentationTool;
import javax.tools.ToolProvider;
import java.net.URL;
import java.util.*;

@Data
public class MyOptions {
    /**
     * 项目下有多个模块
     */
    @NotEmpty(message = "项目目录(projectDir)不能为空")
    private String projectDir;
    @NotEmpty(message = "类名称集合(classNames)不能为空")
    private Set<@NotEmpty String> classNames = HashSet.newHashSet(16);
    /**
     * 应用是指项目下的某个springboot可部署模块
     */
    @NotEmpty(message = "应用标题(appTitle)不能为空")
    private String appTitle;
    private String appText;

    private Set<String> subpackages = HashSet.newHashSet(16);
    private String appDir;
    private AccessKindEnum accessKind = AccessKindEnum.PUBLIC;
    private Class<? extends Doclet> docletClass;

    /**
     * {@code  DocData.name} 是否使用class的simpleName
     */
    private boolean docUseSimpleName = false;

    @Getter
    public enum AccessKindEnum {
        PUBLIC("-public"), PACKAGE("-package"), PROTECTED("-protected"), PRIVATE("-private");
        private final String option;

        AccessKindEnum(String option) {
            this.option = option;
        }
    }

    public static DocumentationTool.DocumentationTask getTask(MyOptions myOptions) {
        if (Objects.isNull(myOptions.getAppDir())) {
            myOptions.setAppDir(obtainAppDir(List.copyOf(myOptions.getClassNames()).get(0)));
        }
        String appDir = myOptions.getAppDir();
        List<String> options = new ArrayList<>(List.of(
                "-encoding", "utf-8",
                "-docletpath", appDir + "/target/classes",
                "-sourcepath", appDir + "/src/main/java")
        );
        options.add(myOptions.getAccessKind().getOption());
        options.addAll(List.of("-doclet", myOptions.getDocletClass().getName()));
        Arrays.stream(OptionEnum.values()).forEach(k -> k.prepare(myOptions, options));
        myOptions.getSubpackages().forEach(p -> options.addAll(List.of("-subpackages", p)));
        DocumentationTool tool = ToolProvider.getSystemDocumentationTool();
        return tool.getTask(null, null, null, null, options, null);
    }

    public static String obtainAppDir(String className) {
        try {
            Class<?> cls = Class.forName(className);
            URL resource = cls.getResource("");
            if (Objects.isNull(resource)) {
                return null;
            }
            String classPath = resource.getPath();
            String targetClassDir = "/target/classes/";
            int idx = classPath.indexOf(targetClassDir);
            if (idx > 0) {
                return classPath.substring(1, idx);
            }
            return null;
        } catch (ClassNotFoundException e) {
            return null;
        }
    }
}
