package org.source.spring.doc.data;

import com.sun.source.doctree.DocCommentTree;
import com.sun.source.doctree.DocTree;
import com.sun.source.doctree.ParamTree;
import com.sun.source.util.DocTrees;
import jdk.javadoc.doclet.DocletEnvironment;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@EqualsAndHashCode(callSuper = true)
@Data
public class ParamDocData extends VariableDocData {
    @Override
    public <E extends Element> void processComment(DocletEnvironment env, E element) {
        if (element instanceof ExecutableElement method) {
            List<? extends DocTree> docTrees = blockTags(env, method);
            Optional<ParamTree> first = docTrees.stream().filter(k -> DocTree.Kind.PARAM.equals(k.getKind()))
                    .map(ParamTree.class::cast).filter(k -> k.getName().toString().equals(this.getKey())).findFirst();
            first.ifPresent(p -> this.setTitle(p.getDescription().toString()));
        }
    }

    public static ParamDocData of(DocletEnvironment env, VariableElement variableElement, ExecutableElement method) {
        ParamDocData paramDocData = new ParamDocData();
        paramDocData.processVariable(env, variableElement, method);
        return paramDocData;
    }

    public static List<DocTree> blockTags(DocletEnvironment env, Element method) {
        DocTrees trees = env.getDocTrees();
        DocCommentTree docCommentTree = trees.getDocCommentTree(method);
        if (Objects.nonNull(docCommentTree)) {
            return List.copyOf(docCommentTree.getBlockTags());
        }
        return List.of();
    }

}
