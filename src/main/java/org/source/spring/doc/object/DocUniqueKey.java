package org.source.spring.doc.object;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class DocUniqueKey {
    @NotEmpty
    private String name;
    @NotEmpty
    private String parentName;
}