package net.sf.skey.view;

import net.sf.osql.model.Table;
import net.sf.osql.view.AbstractTable;
import net.sf.osql.view.IDialect;
import net.sf.xsltiny.TransformersBuilder;
import org.w3c.dom.Document;

import javax.xml.transform.Transformer;
import javax.xml.xpath.XPathExpressionException;
import java.util.Map;

public class SkDialect implements IDialect<SkTable> {
    private static final String INSERTPERMS = "checkinsertperms", UPDATEPERMS = "checkupdateperms",
        UPDATEVIEW="updateview", SELECTVIEW="selectview", GRANTS="grants";
    private final Map<String, Transformer> transformers;
    private final TransformersBuilder.DocumentData documentData;
    private final String init;

    public SkDialect(Map<String, Transformer> transformers, TransformersBuilder.DocumentData documentData, String init) {
        this.transformers = transformers;
        this.documentData = documentData;
        this.init = init;
    }

    private class SkTableImpl extends AbstractTable implements SkTable {

        protected SkTableImpl(Table table, Document document) {
            super(table, document);
        }

        @Override
        public String getInsertionPerms() {
            return show(INSERTPERMS);
        }

        @Override
        public String getUpdatePerms() {
            return show(UPDATEPERMS);
        }

        @Override
        public String getSelectView() {
            return show(SELECTVIEW);
        }

        @Override
        public String getUpdateView() {
            return show(UPDATEVIEW);
        }

        @Override
        public String getGrants() {
            return show(GRANTS);
        }

        @Override
        protected Map<String, Transformer> getTransformers() {
            return transformers;
        }
    }

    public String getSqlDialect() {
        try {
            return documentData.getProperties().get("dialect");
        } catch (XPathExpressionException e) {
            throw new Error("Internal Error");
        }
    }

    public String getInit() {
        return init;
    }

    @Override
    public SkTable render(Table table, Document document) {
        return new SkTableImpl(table, document);
    }
}
