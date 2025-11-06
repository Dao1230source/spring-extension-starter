package org.source.spring.doc.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.source.spring.object.ObjectNode;
import org.source.utility.constant.Constants;

@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Data
public class DocRequestData extends DocData {
    private String methodId;
    @JsonIgnore
    @EqualsAndHashCode.Exclude
    private ObjectNode<DocData> methodNode;

    public DocRequestData(Integer sorted, DocMethodData docMethodData, String parentName) {
        super(sorted, docMethodData.getName() + Constants.COLON + "request", "接口请求", null, parentName);
        this.setMethodId(docMethodData.getFullName());
    }
}