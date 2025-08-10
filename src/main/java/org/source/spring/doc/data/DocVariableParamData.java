package org.source.spring.doc.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sun.source.doctree.DocCommentTree;
import com.sun.source.doctree.DocTree;
import com.sun.source.doctree.ParamTree;
import com.sun.source.doctree.TextTree;
import com.sun.source.util.DocTrees;
import jdk.javadoc.doclet.DocletEnvironment;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import java.util.List;
import java.util.Objects;

@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Data
public class DocVariableParamData extends DocVariableData {
    /**
     * jackson序列化无法解析，注解忽略
     */
    @JsonIgnore
    private ExecutableElement method;

    public <E extends VariableElement> DocVariableParamData(Integer sorted, DocletEnvironment env, E variableElement, String parentName,
                                                            ExecutableElement method) {
        super(sorted, env, variableElement, parentName);
        this.method = method;
        this.processComment(this.obtainCommentLines(env, method));
    }

    @Override
    protected <E extends Element> List<String> obtainCommentLines(DocletEnvironment env, E element) {
        return List.of();
    }

    protected List<String> obtainCommentLines(DocletEnvironment env, ExecutableElement method) {
        DocTrees trees = env.getDocTrees();
        DocCommentTree docCommentTree = trees.getDocCommentTree(method);
        if (Objects.isNull(docCommentTree)) {
            return List.of();
        }
        List<? extends DocTree> docTrees = docCommentTree.getBlockTags();
        return docTrees.stream().filter(k -> DocTree.Kind.PARAM.equals(k.getKind()))
                .map(ParamTree.class::cast).filter(k -> k.getName().toString().equals(this.getName())).findFirst()
                .map(ParamTree::getDescription).orElse(List.of())
                .stream().map(TextTree.class::cast).map(TextTree::getBody).toList();
    }

}