package org.source.spring.doc.object.handler;

import org.apache.commons.lang3.StringUtils;
import org.source.spring.doc.data.DocData;
import org.source.spring.doc.object.entity.DocEntityDefiner;
import org.source.spring.object.handler.ObjectBodyDbHandlerDefiner;
import org.source.utility.constant.Constants;

public abstract class AbstractDocDbHandler<B extends DocEntityDefiner, K> implements ObjectBodyDbHandlerDefiner<B, DocData, K> {

    public abstract K generateKey(String name, String parentName);

    @Override
    public K objectBodyToKey(B b) {
        return this.generateKey(b.getName(), b.getParentName());
    }

    @Override
    public K valueToKey(DocData docData) {
        return this.generateKey(docData.getFullName(), docData.getParentName());
    }

    @Override
    public K valueToParentKey(DocData docData) {
        String parentName = docData.getParentName();
        if (StringUtils.isBlank(parentName)) {
            return null;
        }
        int i = parentName.lastIndexOf(Constants.COLON);
        if (i < 0) {
            return this.generateKey(parentName, DocData.PARENT_NAME_DEFAULT);
        }
        return this.generateKey(parentName, parentName.substring(0, i));
    }
}
