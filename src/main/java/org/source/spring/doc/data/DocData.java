package org.source.spring.doc.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sun.source.doctree.DocCommentTree;
import com.sun.source.doctree.TextTree;
import com.sun.source.util.DocTrees;
import jdk.javadoc.doclet.DocletEnvironment;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.source.spring.object.ValueElementData;
import org.source.utility.constant.Constants;

import javax.lang.model.element.Element;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@EqualsAndHashCode(callSuper = true)
@Data
public class DocData extends ValueElementData {
    @JsonIgnore
    private String id;
    @JsonIgnore
    private String parentId;

    private String key;
    private String title;
    private String text;

    public void processParentId(String parentId) {
        this.parentId = parentId;
        if (Objects.nonNull(this.parentId)) {
            this.id = this.parentId + Constants.COLON + this.key;
        } else {
            this.id = this.key;
        }
    }

    public <E extends Element> void processComment(DocletEnvironment env, E element) {
        DocTrees trees = env.getDocTrees();
        DocCommentTree docCommentTree = trees.getDocCommentTree(element);
        if (Objects.nonNull(docCommentTree)) {
            String comment = docCommentTree.getFullBody().stream().map(TextTree.class::cast).map(TextTree::getBody)
                    .collect(Collectors.joining(Constants.NEWLINE));
            List<String> commentLines = Arrays.asList(comment.split(Constants.NEWLINE));
            this.setTitle(commentLines.get(0));
            if (commentLines.size() > 1) {
                this.setText(String.join(Constants.NEWLINE, commentLines.subList(1, commentLines.size())));
            }
        }
    }
}
