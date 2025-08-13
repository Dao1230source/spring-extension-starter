package org.source.spring.doc;

import lombok.Data;
import org.source.spring.doc.data.DocBaseVariableData;
import org.source.spring.doc.data.DocData;
import org.source.spring.doc.data.DocVariableAnnotationData;
import org.source.spring.doc.data.DocVariableData;
import org.source.spring.doc.object.AbstractDocProcessor;
import org.source.spring.object.ObjectElement;
import org.source.spring.utility.SpringUtil;
import org.source.utility.tree.define.AbstractNode;

import javax.lang.model.element.VariableElement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

@Data
public class DocDataContainer {
    private List<DocData> docDataList = new ArrayList<>(16);

    public DocData obtainAppDocData(AbstractDocProcessor<?, ?, ?, ?> docProcessor, MyOptions myOptions) {
        String appName = Objects.requireNonNullElse(SpringUtil.getEnvironment().getProperty("spring.application.name"), "springboot");
        return docProcessor.handleValueDataTree().get(n -> n.getId().equals(appName))
                .map(AbstractNode::getElement).map(ObjectElement::getValue)
                .orElseGet(() -> {
                    DocData docData = new DocData(0, appName, myOptions.getAppTitle(), myOptions.getAppText());
                    docDataList.add(docData);
                    return docData;
                });
    }

    public <V2 extends DocVariableData, V1 extends VariableElement> void addVariableData(V2 variableData, V1 element, DocData appDocData) {
        docDataList.add(variableData);
        docDataList.addAll(DocVariableAnnotationData.obtainAnnotationDocDataList(element, variableData.getFullName()));
        if (variableData.baseType()) {
            docDataList.add(new DocBaseVariableData(variableData, appDocData.getFullName()));
        }
    }

    public <D extends DocData> void add(D docData) {
        this.docDataList.add(docData);
    }

    public <D extends DocData> void add(Collection<D> docData) {
        this.docDataList.addAll(docData);
    }

    public void forEach(Consumer<DocData> consumer) {
        this.docDataList.forEach(consumer);
    }

    public void clear() {
        this.docDataList.clear();
    }
}