package org.source.spring.doc.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.source.spring.object.ViewData;
import org.source.spring.object.ViewItemData;
import org.source.utility.tree.DefaultNode;
import org.source.utility.tree.identity.AbstractNode;

import java.util.Objects;

@EqualsAndHashCode(callSuper = true)
@Data
public class RequestDocData extends DocData implements ViewData {
    @JsonIgnore
    private DefaultNode<String, DocData> requestView;

    public static RequestDocData of(String clsId) {
        RequestDocData viewDocData = new RequestDocData();
        viewDocData.setKey("methodView");
        viewDocData.setTitle("方法视图");
        viewDocData.processParentId(clsId);
        return viewDocData;
    }

    @JsonProperty("viewData")
    @Override
    public DefaultNode<String, ViewItemData> getViewData() {
        if (Objects.isNull(requestView)) {
            return null;
        }
        return AbstractNode.cast(requestView, k -> ViewItemData.builder().objectId(k.getObjectId()).build(),
                ViewItemData::setParentObjectId);
    }
}
