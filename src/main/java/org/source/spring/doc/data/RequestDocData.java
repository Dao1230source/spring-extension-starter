package org.source.spring.doc.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.source.spring.object.ViewObject;
import org.source.spring.object.ViewObjectItem;
import org.source.spring.object.tree.ObjectNode;
import org.source.utility.constant.Constants;
import org.source.utility.tree.identity.AbstractNode;

import java.util.Objects;

@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Data
public class RequestDocData extends DocData implements ViewObject<ViewObjectItem, ObjectNode<String, ViewObjectItem>> {
    private String methodId;
    @JsonIgnore
    private ObjectNode<String, DocData> requestData;

    public RequestDocData(MethodDocData methodDocData, String parentId) {
        this.setMethodId(methodDocData.getId());
        this.setName(methodDocData.getName() + Constants.COLON + "request");
        this.setTitle("接口请求");
        this.processParentId(parentId);
    }

    @JsonProperty("viewData")
    @Override
    public ObjectNode<String, ViewObjectItem> getViewData() {
        if (Objects.isNull(requestData)) {
            return null;
        }
        return AbstractNode.cast(requestData, k -> ViewObjectItem.builder().objectId(k.getObjectId()).build(),
                ViewObjectItem::setParentObjectId);
    }
}
