package org.source.spring.doc.data;

import com.sun.source.doctree.DocCommentTree;
import com.sun.source.doctree.DocTree;
import com.sun.source.doctree.ParamTree;
import com.sun.source.util.DocTrees;
import jdk.javadoc.doclet.DocletEnvironment;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import java.util.List;
import java.util.Objects;

@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Data
public class ParamDocData extends VariableDocData {

    public <E extends VariableElement> ParamDocData(DocletEnvironment env, E variableElement, String parentId, ExecutableElement method) {
        super(env, variableElement, parentId);
        this.processComment(env, method);
    }

    protected <E extends ExecutableElement> void processComment(DocletEnvironment env, E element) {
        DocTrees trees = env.getDocTrees();
        DocCommentTree docCommentTree = trees.getDocCommentTree(element);
        if (Objects.isNull(docCommentTree)) {
            return;
        }
        List<? extends DocTree> docTrees = docCommentTree.getBlockTags();
        docTrees.stream().filter(k -> DocTree.Kind.PARAM.equals(k.getKind()))
                .map(ParamTree.class::cast).filter(k -> k.getName().toString().equals(this.getName())).findFirst()
                .ifPresent(p -> super.processComment(p.getDescription()));
    }

}
