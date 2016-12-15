package net.sf.skey.view;

import net.sf.osql.view.AbstractRender;
import net.sf.osql.view.Database;
import net.sf.xsltiny.TransformersBuilder;

import javax.naming.NameNotFoundException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class SkRender extends AbstractRender<SkTable,SkDbView,SkDialect> {

    private final String user;
    private final String db;

    public SkRender(String mode, String user, String db) {
        super(mode);
        this.user = user;
        this.db = db;
    }

    @Override
    protected SkDbView provideDbView(Database database, String name) {
        try {
            return new SkDbView(database, getDialect(name));
        } catch (NameNotFoundException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    protected SkDialect provideDialect(String name) throws IOException, TransformerException {
        URL url = ClassLoader.getSystemResource("net/sf/skey/dialect/" + name + ".xml");
        if(url == null)
            throw new IOException();
        TransformersBuilder.DocumentData documentData = TransformersBuilder.loadDocument(url);
        String init;
        try {
            Map<String, String> properties = documentData.getProperties();
            properties.put("user",user);
            documentData.setProperties(properties);
            String alias = properties.get("dbalias");
            init = properties.get("init");
            if(alias == null || init == null) {
                throw new TransformerException("Dialect must define properties `dbalias` and `init`");
            }
            init = init.replace(alias, db);
        } catch (XPathExpressionException e) {
            throw new TransformerException("Unable to handle context properties", e);
        }
        return new SkDialect(transformer.getTransformers(documentData), documentData, init);
    }

    @Override
    protected Set<String> getRegistredDialects() throws IOException, URISyntaxException {
        return getRegistredDialectsFromSystem("net/sf/skey/dialect");
    }

}
