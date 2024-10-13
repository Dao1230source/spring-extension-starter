package org.source.spring.doc;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import javax.tools.DocumentationTool;
import javax.tools.ToolProvider;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@UtilityClass
@Slf4j
public class DocTool {

    public static DocumentationTool.DocumentationTask getTask(MyOptions myOptions) {
        String appDir = myOptions.getAppDir();
        if (Objects.isNull(appDir)) {
            appDir = System.getProperty("user.dir");
        }
        List<String> options = new java.util.ArrayList<>(List.of(
                "-encoding", "utf-8",
                "-docletpath", appDir + "\\target\\classes",
                "-sourcepath", appDir + "\\src\\main\\java"));
        options.add(myOptions.getAccessKind().getOption());
        options.addAll(List.of("-doclet", myOptions.getDocletClass().getName()));
        Arrays.stream(OptionEnum.values()).forEach(k -> k.prepare(myOptions, options));
        myOptions.getSubpackages().forEach(p -> options.addAll(List.of("-subpackages", p)));
        DocumentationTool tool = ToolProvider.getSystemDocumentationTool();
        return tool.getTask(null, null, null, null, options, null);
    }

}
