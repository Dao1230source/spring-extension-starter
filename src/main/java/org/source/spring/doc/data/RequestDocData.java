package org.source.spring.doc.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.source.spring.object.data.ObjectFullData;
import org.source.spring.object.tree.ObjectNode;
import org.source.utility.constant.Constants;

@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Data
public class RequestDocData extends DocData {
    private String methodId;
    @JsonIgnore
    @EqualsAndHashCode.Exclude
    private ObjectNode<String, ObjectFullData<DocData>> methodNode;

    public RequestDocData(MethodDocData methodDocData, String parentId) {
        this.setMethodId(methodDocData.getId());
        this.setName(methodDocData.getName() + Constants.COLON + "request");
        this.setTitle("接口请求");
        this.processParentId(parentId);
    }
}
