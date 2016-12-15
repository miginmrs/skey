package net.sf.skey.parser;

import net.sf.osql.model.DatabaseManager;
import net.sf.osql.model.Table;
import net.sf.osql.parser.FieldParser;
import net.sf.osql.parser.Parser;
import net.sf.osql.parser.TableParser;
import net.sf.osql.parser.exceptions.TypeException;
import net.sf.osql.view.Database;
import net.sf.skey.view.SkTable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ParseUtils {
    private static final Pattern END_PATTERN = Pattern.compile("\\s*$");
    private static final String[] events = {"before:insert", "before:update"};
    public static final String TABLES = "tables";
    public static final String ALL = "all";

    public static Map<String, List<Table>> getTables(String content) {
        Parser t = new Parser(content);
        DatabaseManager dbManager = new DatabaseManager();
        TableParser tableParser = new TableParser("class", new FieldParser(true), false, dbManager);
        TableParser interfaceParser = new TableParser("@interface", new FieldParser(true), true, dbManager);
        List<Table> tabs = new LinkedList<>();
        List<Table> all = new LinkedList<>();
        while(true) {
            try {
                try {
                    Table table = tableParser.apply(t);
                    tabs.add(table);
                    all.add(table);
                } catch(TypeException te) {
                    all.add(interfaceParser.apply(t));
                }
            } catch(RuntimeException re) {
                try {
                    t.parseWithPattern(END_PATTERN);
                    break;
                } catch(Exception e) {
                    throw re;
                }
            }
        }
        return new HashMap<String, List<Table>>(){{put(TABLES, tabs); put(ALL, all);}};
    }

    public static void addPermissions(SkTable skTable, Database db){
        Table table = skTable.getTable();
        Document document = db.getTableDocument(table);
        Element triggersElement = (Element) document.getElementsByTagName("triggers").item(0);
        NodeList triggerElements = triggersElement.getElementsByTagName("trigger");
        Map<String, Element> triggers = new HashMap<>();
        String[] perms = {skTable.getInsertionPerms(), skTable.getUpdatePerms()};
        List<Integer> indexes = Arrays.stream(new Integer[]{0, 1}).filter(t -> !perms[t].isEmpty()).collect(Collectors.toList());
        int remaining = 2;
        elements:for(int i=0, n=triggerElements.getLength(); i<n; i++) {
            Element triggerElement = (Element) triggerElements.item(i);
            for(Integer index : indexes) if(events[index].equals(triggerElement.getAttribute("event"))) {
                triggers.put(events[index], triggerElement);
                if (--remaining == 0) break elements;
                break;
            }
        }
        for(int i : indexes) if(!triggers.containsKey(events[i])) {
            Element triggerElement = document.createElement("trigger");
            triggerElement.setAttribute("event", events[i]);
            triggersElement.appendChild(triggerElement);
            triggers.put(events[i], triggerElement);
        }
        for(int i : indexes) {
            Element triggerElement = triggers.get(events[i]);
            triggerElement.setTextContent(triggerElement.getTextContent()+perms[i]);
        }
    }
}
