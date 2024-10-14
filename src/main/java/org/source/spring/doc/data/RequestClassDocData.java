package org.source.spring.doc.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jdk.javadoc.doclet.DocletEnvironment;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.util.CollectionUtils;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import java.util.List;

/**
 * 接口请求类，如Controller
 */
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Data
public class RequestClassDocData extends ClassDocData implements Path {

    private List<String> paths;
    private List<String> requestMethods;

    @JsonIgnore
    private boolean docUseSimpleName;

    public <E extends TypeElement> RequestClassDocData(DocletEnvironment env, E type, String parentId, boolean docUseSimpleName) {
        super(env, type, parentId);
        this.docUseSimpleName = docUseSimpleName;
        this.processRequestMapping(type);
        this.processName(type);
        this.processParentId(parentId);
    }

    @Override
    protected <E extends Element> void processName(E element) {
        if (docUseSimpleName) {
            this.setName(element.getSimpleName().toString());
        } else {
            this.setName(((TypeElement) element).getQualifiedName().toString());
        }
    }

    @Override
    public <D extends DocData> void merge(D docData) {
        super.merge(docData);
        if (docData instanceof RequestClassDocData requestClassDocData) {
            if (CollectionUtils.isEmpty(this.paths) && !CollectionUtils.isEmpty(requestClassDocData.getPaths())) {
                this.paths = List.copyOf(requestClassDocData.getPaths());
            }
            if (CollectionUtils.isEmpty(this.requestMethods) && !CollectionUtils.isEmpty(requestClassDocData.getRequestMethods())) {
                this.requestMethods = List.copyOf(requestClassDocData.getRequestMethods());
            }
        }
    }
}
