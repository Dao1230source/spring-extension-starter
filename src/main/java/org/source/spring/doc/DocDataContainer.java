package org.source.spring.doc;

import lombok.Data;
import org.source.spring.doc.data.AnnotationDocData;
import org.source.spring.doc.data.DocData;
import org.source.spring.doc.object.processor.AbstractDocProcessor;
import org.source.spring.object.data.ObjectFullData;
import org.source.spring.utility.SpringUtil;
import org.source.utility.tree.identity.AbstractNode;

import javax.lang.model.element.Element;
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
        return docProcessor.getObjectTree().get(n -> n.getId().equals(appName))
                .map(AbstractNode::getElement).map(ObjectFullData::getValue)
                .orElseGet(() -> {
                    DocData docData = new DocData(appName, myOptions.getAppTitle(), myOptions.getAppText());
                    docDataList.add(docData);
                    return docData;
                });
    }

    public <D extends DocData> void add(D docData) {
        this.docDataList.add(docData);
    }

    public <D extends DocData> void add(Collection<D> docData) {
        this.docDataList.addAll(docData);
    }

    public <E extends Element> void addWithAnnotation(DocData docData, E element) {
        this.docDataList.add(docData);
        this.docDataList.addAll(AnnotationDocData.obtainAnnotationDocDataList(element, docData.getId()));
    }

    public void forEach(Consumer<DocData> consumer) {
        this.docDataList.forEach(consumer);
    }

    public void clear() {
        this.docDataList.clear();
    }
}
