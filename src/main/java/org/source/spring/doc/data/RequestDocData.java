package org.source.spring.doc.data;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.source.spring.object.ObjectSummary;
import org.source.spring.object.tree.ObjectNode;
import org.source.utility.constant.Constants;
import org.source.utility.tree.identity.AbstractNode;

import java.util.Objects;

@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Data
public class RequestDocData extends DocData {
    private String methodId;
    private ObjectNode<String, ObjectSummary> requestData;

    public RequestDocData(MethodDocData methodDocData, String parentId) {
        this.setMethodId(methodDocData.getId());
        this.setName(methodDocData.getName() + Constants.COLON + "request");
        this.setTitle("接口请求");
        this.processParentId(parentId);
    }

    public static ObjectNode<String, ObjectSummary> transfer2Summary(ObjectNode<String, DocData> objectNode) {
        if (Objects.isNull(objectNode)) {
            return null;
        }
        return AbstractNode.cast(objectNode, k -> {
            ObjectSummary objectSummary = new ObjectSummary();
            objectSummary.setObjectId(k.getObjectId());
            return objectSummary;
        }, ObjectSummary::setParentObjectId);
    }
}
