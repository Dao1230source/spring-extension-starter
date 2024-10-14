package org.source.spring.doc.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.source.spring.object.ViewData;
import org.source.spring.object.ViewItemData;
import org.source.utility.constant.Constants;
import org.source.utility.tree.DefaultNode;
import org.source.utility.tree.identity.AbstractNode;

import java.util.Objects;

@EqualsAndHashCode(callSuper = true)
@Data
public class RequestDocData extends DocData implements ViewData {
    private String methodId;
    @JsonIgnore
    private DefaultNode<String, DocData> requestData;

    public static RequestDocData of(String clsId, String methodId) {
        RequestDocData requestDocData = new RequestDocData();
        requestDocData.setMethodId(methodId);
        requestDocData.setKey(methodId + Constants.COLON + "request");
        requestDocData.setTitle("接口请求");
        requestDocData.setId(requestDocData.getKey());
        requestDocData.setParentId(clsId);
        return requestDocData;
    }

    @JsonProperty("viewData")
    @Override
    public DefaultNode<String, ViewItemData> getViewData() {
        if (Objects.isNull(requestData)) {
            return null;
        }
        return AbstractNode.cast(requestData, k -> ViewItemData.builder().objectId(k.getObjectId()).build(),
                ViewItemData::setParentObjectId);
    }
}
