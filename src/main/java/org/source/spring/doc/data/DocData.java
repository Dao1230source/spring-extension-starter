package org.source.spring.doc.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sun.source.doctree.DocCommentTree;
import com.sun.source.doctree.DocTree;
import com.sun.source.doctree.TextTree;
import com.sun.source.util.DocTrees;
import jdk.javadoc.doclet.DocletEnvironment;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.source.spring.doc.object.enums.DocRelationTypeEnum;
import org.source.spring.doc.object.AbstractDocProcessor;
import org.source.spring.object.AbstractValue;
import org.source.utility.constant.Constants;

import javax.lang.model.element.Element;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Data
public class DocData extends AbstractValue {
    private String name;
    private String parentName;
    private String fullName;
    private String title;
    private String text;

    /**
     * 默认的与其他数据的关系
     */
    @JsonIgnore
    @EqualsAndHashCode.Exclude
    private Integer relationType = DocRelationTypeEnum.BELONG.getType();

    public <E extends Element> DocData(DocletEnvironment env, E element, String parentId) {
        this.processName(element);
        this.processComment(env, element);
        this.processParentId(parentId);
    }

    public DocData(@NotNull String name, String title, String text) {
        this.name = name;
        this.title = title;
        this.text = text;
        this.processParentId(null);
    }

    protected <E extends Element> void processName(E element) {
        this.setName(element.getSimpleName().toString());
    }

    protected void processParentId(String parentId) {
        this.parentName = Objects.nonNull(parentId) ? parentId : AbstractDocProcessor.PARENT_NAME_DEFAULT;
    }

    protected <E extends Element> void processComment(DocletEnvironment env, E element) {
        DocTrees trees = env.getDocTrees();
        DocCommentTree docCommentTree = trees.getDocCommentTree(element);
        if (Objects.isNull(docCommentTree)) {
            return;
        }
        this.processComment(docCommentTree.getFullBody());
    }

    protected void processComment(List<? extends DocTree> commentDocList) {
        String comment = commentDocList.stream().map(TextTree.class::cast).map(TextTree::getBody)
                .collect(Collectors.joining(Constants.NEWLINE));
        List<String> commentLines = Arrays.asList(comment.split(Constants.NEWLINE));
        this.setTitle(commentLines.get(0));
        if (commentLines.size() > 1) {
            this.setText(String.join(Constants.NEWLINE, commentLines.subList(1, commentLines.size())));
        }
    }

    public <D extends DocData> void merge(D docData) {
        if (Objects.isNull(this.getTitle())) {
            this.setTitle(docData.getTitle());
        }
        if (Objects.isNull(this.getText())) {
            this.setText(docData.getText());
        }
    }

    public String getFullName() {
        if (Objects.isNull(this.fullName)) {
            this.fullName = obtainFullName(this.name, this.parentName);
        }
        return this.fullName;
    }

    @JsonIgnore
    @Override
    public @NotNull String getId() {
        return this.getFullName();
    }

    @JsonIgnore
    @Override
    public String getParentId() {
        return this.parentName;
    }

    public static String obtainFullName(String name, String parentName) {
        return AbstractDocProcessor.PARENT_NAME_DEFAULT.equals(parentName) ? name : parentName + Constants.COLON + name;
    }

}