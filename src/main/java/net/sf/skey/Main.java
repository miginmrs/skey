package net.sf.skey;

import net.sf.osql.model.Table;
import net.sf.osql.view.*;
import net.sf.skey.controller.Argument;
import net.sf.skey.view.SkDbView;
import net.sf.skey.view.SkDialect;
import net.sf.skey.view.SkRender;
import net.sf.skey.view.SkTable;
import net.sf.xsltiny.TransformersBuilder;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPathExpressionException;
import java.io.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

import static net.sf.skey.controller.CmdUtils.getArgument;
import static net.sf.skey.parser.ParseUtils.*;

public class Main {


    public static void main(String[] args) throws IOException, XPathExpressionException, SAXException, TransformerConfigurationException {
        getArgument(args).apply((Argument argument) -> {
            assert argument != null;
            PrintStream out = argument.out;
            // parse file to get list of model tables
            Map<String, List<Table>> map = getTables(new Scanner(argument.in, "UTF-8").useDelimiter("\\Z").next());
            List<Table> tabs = map.get(TABLES);
            List<Table> all = map.get(ALL);
            // database of xml representation of tables
            Database db = Render.loadDatabase(tabs);
            if(argument.dialect == null) {
                out.println(db.getTables());
                out.flush();
                return null;
            }
            // load Skey render with defined mode
            SkRender render = new SkRender(argument.mode, argument.user, argument.db);
            // load the dialect
            SkDialect skDialect = render.getDialect(argument.dialect);
            // create a view
            SkDbView skview = new SkDbView(db, skDialect);
            // get skey representation of tables
            List<SkTable> skTables = tabs.stream().map(skview::render)
                    .filter(sktable->sktable.getTable().paramsMap.get("output") != null)
                    .collect(Collectors.toList());
            // add permissions to xml representation of tables
            skTables.forEach(skTable -> addPermissions(skTable, db));
            if(argument.xml) {
                System.out.println(db.getTables());
            }

            SqlViewer sqlViewer = new SqlViewer(argument.mode);
            IDatabase<DbView> sqldb = sqlViewer.adoptDatabase(db);
            DbView sqlview = sqldb.use(skDialect.getSqlDialect());
            List<TableView> tableViews = tabs.stream().map(sqlview::render).collect(Collectors.toList());

            // output
            out.println(skDialect.getInit());
            new HashMap<Table, TableView>(){{
                tableViews.forEach(tv->put(tv.getTable(), tv));
                for(Table table : all) {
                    TableView tv = get(table);
                    if(tv != null) {
                        out.println(tv.showDefinition());
                        out.println(tv.showTriggers());
                        out.println(tv.showInsertions());
                    } else {
                        out.println(table.sqldata);
                    }
                }
            }};
            tableViews.stream().filter(table -> table.getTable().from != null).map(TableView::showConstraints).forEach(out::println);
            tableViews.stream().filter(table -> table.getTable().from != null).map(TableView::showITable).forEach(out::println);
            skTables.stream().map(SkTable::getUpdateView).forEach(out::println);
            skTables.stream().map(SkTable::getSelectView).forEach(out::println);
            skTables.stream().map(SkTable::getGrants).forEach(out::println);
            out.flush();
            return null;
        });
    }
}
