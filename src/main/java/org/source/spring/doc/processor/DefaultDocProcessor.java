package org.source.spring.doc.processor;

import lombok.Getter;
import org.source.spring.doc.data.DocData;
import org.source.utility.tree.DefaultNode;
import org.source.utility.tree.Tree;

@Getter
public class DefaultDocProcessor implements DocProcessor {

    private final Tree<String, DocData, DefaultNode<String, DocData>> docTree = DefaultNode.buildTree();
}
