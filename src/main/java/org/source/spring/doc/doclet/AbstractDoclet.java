package org.source.spring.doc.doclet;

import jdk.javadoc.doclet.Doclet;
import jdk.javadoc.doclet.DocletEnvironment;
import jdk.javadoc.doclet.Reporter;
import lombok.extern.slf4j.Slf4j;
import org.source.spring.doc.DocTool;
import org.source.spring.doc.MyOption;
import org.source.spring.doc.MyOptions;
import org.source.spring.doc.OptionEnum;
import org.source.spring.doc.data.DocData;
import org.source.spring.doc.processor.DocProcessor;
import org.source.spring.utility.SpringUtil;
import org.springframework.util.CollectionUtils;

import javax.lang.model.SourceVersion;
import javax.tools.Diagnostic;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public abstract class AbstractDoclet implements Doclet {
    protected final MyOptions myOptions = new MyOptions();
    protected final Set<String> extraClsNames = HashSet.newHashSet(16);

    @Override
    public boolean run(DocletEnvironment env) {
        try {
            DocProcessor docProcessor = this.obtainProcessor();
            List<DocData> docDataList = this.processDoc(env);
            this.processExtraClasses();
            docProcessor.save(docDataList);
            return true;
        } catch (Exception e) {
            log.error("Doclet.process exception", e);
            return false;
        }
    }

    public DocProcessor obtainProcessor() {
        try {
            return SpringUtil.getBean(DocProcessor.class);
        } catch (Exception e) {
            log.error("cannot getBean for DocProcessor.class");
            throw e;
        }
    }

    protected abstract List<DocData> processDoc(DocletEnvironment env);

    protected void processExtraClasses() {
        if (!CollectionUtils.isEmpty(extraClsNames)) {
            MyOptions myOptions1 = new MyOptions();
            myOptions1.setAppDir(this.myOptions.getAppDir());
            myOptions1.setClassNames(extraClsNames);
            myOptions1.setDocletClass(VariableDoclet.class);
            myOptions1.setAccessKind(MyOptions.AccessKindEnum.PRIVATE);
            DocTool.getTask(myOptions1).call();
        }
    }

    @Override
    public void init(Locale locale, Reporter reporter) {
        reporter.print(Diagnostic.Kind.NOTE, this.getName());
        reporter.print(Diagnostic.Kind.NOTE, "Doclet using locale: " + locale);
    }

    @Override
    public Set<? extends Option> getSupportedOptions() {
        return Arrays.stream(OptionEnum.values()).map(k -> new MyOption(k) {

            @Override
            public boolean process(String option, List<String> arguments) {
                return k.process(myOptions, option, arguments);
            }
        }).collect(Collectors.toSet());
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latest();
    }

    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }
}
