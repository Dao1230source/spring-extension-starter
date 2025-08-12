package org.source.spring.doc.data;

import com.sun.source.doctree.DocCommentTree;
import com.sun.source.doctree.TextTree;
import com.sun.source.util.DocTrees;
import jdk.javadoc.doclet.DocletEnvironment;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.source.spring.doc.object.enums.DocRelationTypeEnum;
import org.source.spring.object.AbstractValue;
import org.source.spring.uid.Ids;
import org.source.utility.constant.Constants;

import javax.lang.model.element.Element;
import java.util.List;
import java.util.Objects;

@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Data
public class DocData extends AbstractValue {
    public static final String PARENT_NAME_DEFAULT = "root";
    private String title;
    private String text;
    private String parentName;
    private String fullName;

    public DocData(Integer sorted, String name, String title, String text, String parentName) {
        this.title = title;
        this.text = text;
        this.processName(name, parentName);
        this.setSorted(String.valueOf(sorted));
        this.setObjectId(Ids.stringId());
        this.setRelationType(DocRelationTypeEnum.SUP_AND_SUB.getType());
    }

    protected void processName(String name, String parentName) {
        this.setName(name);
        this.parentName = this.obtainParentName(parentName);
        this.fullName = obtainFullName(this.getName(), this.parentName);
    }

    public DocData(Integer sorted, String name, String title, String text) {
        this(sorted, name, title, text, null);
    }

    public <E extends Element> DocData(Integer sorted, DocletEnvironment env, E element, String parentName) {
        this.processName(this.obtainName(element), parentName);
        this.processComment(this.obtainCommentLines(env, element));
        this.setSorted(String.valueOf(sorted));
        this.setObjectId(Ids.stringId());
        this.setRelationType(DocRelationTypeEnum.SUP_AND_SUB.getType());
    }

    protected <E extends Element> String obtainName(E element) {
        return element.getSimpleName().toString();
    }

    protected String obtainParentName(String parentName) {
        return Objects.nonNull(parentName) ? parentName : PARENT_NAME_DEFAULT;
    }

    protected void processComment(List<String> commentLines) {
        if (commentLines.isEmpty()) {
            return;
        }
        this.setTitle(commentLines.get(0));
        if (commentLines.size() > 1) {
            this.setText(String.join(Constants.NEWLINE, commentLines.subList(1, commentLines.size())));
        }
    }

    protected <E extends Element> List<String> obtainCommentLines(DocletEnvironment env, E element) {
        if (Objects.isNull(env) || Objects.isNull(element)) {
            return List.of();
        }
        DocTrees trees = env.getDocTrees();
        DocCommentTree docCommentTree = trees.getDocCommentTree(element);
        if (Objects.isNull(docCommentTree)) {
            return List.of();
        }
        return docCommentTree.getFullBody().stream().map(TextTree.class::cast).map(TextTree::getBody).toList();
    }

    public <D extends DocData> void merge(D docData) {
        if (Objects.isNull(this.getTitle())) {
            this.setTitle(docData.getTitle());
        }
        if (Objects.isNull(this.getText())) {
            this.setText(docData.getText());
        }
    }

    public static String obtainFullName(String name, String parentName) {
        return PARENT_NAME_DEFAULT.equals(parentName) ? name : parentName + Constants.COLON + name;
    }

}