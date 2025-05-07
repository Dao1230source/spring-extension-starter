package org.source.spring.doc.processor;

import lombok.Getter;
import org.source.spring.doc.data.DocData;
import org.source.spring.object.entity.ObjectBodyEntityIdentity;
import org.source.spring.object.entity.ObjectEntityIdentity;
import org.source.spring.object.entity.RelationEntityIdentity;
import org.source.spring.object.processor.AbstractObjectProcessor;

@Getter
public abstract class AbstractDocProcessor<O extends ObjectEntityIdentity, R extends RelationEntityIdentity, B extends ObjectBodyEntityIdentity>
        extends AbstractObjectProcessor<O, R, B, DocData> {
}
