package org.source.spring.doc.data;

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
public class DocClassRequestData extends DocClassData implements Path {

    private List<String> paths;
    private List<String> requestMethods;
    private boolean docUseSimpleName;

    public <E extends TypeElement> DocClassRequestData(Integer sorted, DocletEnvironment env, E element, String parentId, boolean docUseSimpleName) {
        super(sorted, env, element, parentId);
        this.docUseSimpleName = docUseSimpleName;
        this.processName(this.obtainName(element), parentId);
        this.processRequestMapping(element);
    }

    @Override
    protected <E extends Element> String obtainName(E element) {
        if (docUseSimpleName) {
            return element.getSimpleName().toString();
        } else {
            return ((TypeElement) element).getQualifiedName().toString();
        }
    }

    @Override
    public <D extends DocData> void merge(D docData) {
        super.merge(docData);
        if (docData instanceof DocClassRequestData classRequestDocData) {
            if (CollectionUtils.isEmpty(this.paths) && !CollectionUtils.isEmpty(classRequestDocData.getPaths())) {
                this.paths = List.copyOf(classRequestDocData.getPaths());
            }
            if (CollectionUtils.isEmpty(this.requestMethods) && !CollectionUtils.isEmpty(classRequestDocData.getRequestMethods())) {
                this.requestMethods = List.copyOf(classRequestDocData.getRequestMethods());
            }
        }
    }
}