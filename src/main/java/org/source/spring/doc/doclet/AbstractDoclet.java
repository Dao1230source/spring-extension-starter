package org.source.spring.doc.doclet;

import jdk.javadoc.doclet.Doclet;
import jdk.javadoc.doclet.DocletEnvironment;
import jdk.javadoc.doclet.Reporter;
import lombok.extern.slf4j.Slf4j;
import org.source.spring.doc.DocDataContainer;
import org.source.spring.doc.MyOption;
import org.source.spring.doc.MyOptions;
import org.source.spring.doc.OptionEnum;
import org.source.spring.doc.data.ClassDocData;
import org.source.spring.doc.data.DocData;
import org.source.spring.doc.data.VariableDocData;
import org.source.spring.doc.object.AbstractDocProcessor;
import org.source.spring.utility.SpringUtil;
import org.source.utility.utils.Streams;
import org.springframework.util.CollectionUtils;

import javax.lang.model.SourceVersion;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public abstract class AbstractDoclet implements Doclet {
    protected final MyOptions myOptions = new MyOptions();

    @Override
    public boolean run(DocletEnvironment env) {
        try {
            // doc结果数据容器
            AbstractDocProcessor<?, ?, ?, ?> docProcessor = this.obtainProcessor();
            DocDataContainer docDataContainer = new DocDataContainer();
            // 应用的doc对象
            DocData appDocData = docDataContainer.obtainAppDocData(docProcessor, this.myOptions);
            this.processDoc(env, docDataContainer, appDocData);
            docProcessor.merge(docDataContainer.getDocDataList());
            // 计算class的父类和接口
            this.processExtraSuper(this.obtainExtraSuperClsNames(docDataContainer.getDocDataList()));
            // 计算变量是非基础类型即手动创建的类
            this.processExtraVariable(this.obtainExtraVariableClsNames(docDataContainer.getDocDataList()));
            return true;
        } catch (Exception e) {
            log.error("Doclet.process exception", e);
            return false;
        }
    }

    protected AbstractDocProcessor<?, ?, ?, ?> obtainProcessor() {
        try {
            return SpringUtil.getBean(AbstractDocProcessor.class);
        } catch (Exception e) {
            log.error("cannot getBean for AbstractDocProcessor.class");
            throw e;
        }
    }

    protected List<TypeElement> obtainScannedResult(DocletEnvironment env) {
        return env.getIncludedElements().stream()
                .filter(TypeElement.class::isInstance).map(TypeElement.class::cast)
                .filter(k -> ElementKind.CLASS.equals(k.getKind()) || ElementKind.INTERFACE.equals(k.getKind()))
                .filter(k -> myOptions.getClassNames().contains(k.getQualifiedName().toString()))
                .toList();
    }

    protected abstract void processDoc(DocletEnvironment env, DocDataContainer docDataContainer, DocData appDocData);

    protected Set<String> obtainExtraSuperClsNames(List<DocData> docDataList) {
        return docDataList.stream()
                .filter(ClassDocData.class::isInstance).map(ClassDocData.class::cast)
                .map(ClassDocData::obtainSuperClassNames).flatMap(Collection::stream).collect(Collectors.toSet());
    }

    protected void processExtraSuper(Set<String> extraSuperClsNames) {
        MyOptions options = handleMyOptionsByClassNames(extraSuperClsNames, this.myOptions);
        if (Objects.nonNull(options)) {
            options.setDocletClass(RequestDoclet.class);
            options.setAccessKind(MyOptions.AccessKindEnum.PUBLIC);
            MyOptions.getTask(options).call();
        }
    }

    protected Set<String> obtainExtraVariableClsNames(List<DocData> docDataList) {
        return docDataList.stream()
                .filter(VariableDocData.class::isInstance).map(VariableDocData.class::cast)
                .filter(VariableDocData::notBaseType).map(VariableDocData::getTypeName)
                .collect(Collectors.toSet());
    }

    protected void processExtraVariable(Set<String> extraVariableClsNames) {
        MyOptions options = handleMyOptionsByClassNames(extraVariableClsNames, this.myOptions);
        if (Objects.nonNull(options)) {
            options.setDocletClass(VariableDoclet.class);
            options.setAccessKind(MyOptions.AccessKindEnum.PRIVATE);
            MyOptions.getTask(options).call();
        }
    }

    public static MyOptions handleMyOptionsByClassNames(Set<String> extraClsNames, MyOptions myOptions) {
        List<String> clsNames = Streams.of(extraClsNames)
                .filter(k -> {
                    String appDir = MyOptions.obtainAppDir(k);
                    return Objects.nonNull(appDir) && appDir.startsWith(myOptions.getProjectDir());
                }).toList();
        if (!CollectionUtils.isEmpty(clsNames)) {
            MyOptions options = new MyOptions();
            options.setProjectDir(myOptions.getProjectDir());
            options.setAppTitle(myOptions.getAppTitle());
            options.setAppText(myOptions.getAppText());
            options.setAppDir(MyOptions.obtainAppDir(clsNames.get(0)));
            options.getClassNames().addAll(clsNames);
            options.setDocletClass(myOptions.getDocletClass());
            options.setAccessKind(myOptions.getAccessKind());
            options.setDocUseSimpleName(false);
            return options;
        }
        return null;
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
