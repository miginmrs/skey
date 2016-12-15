package net.sf.osql;

import org.junit.Test;
import org.xml.sax.SAXException;

import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;
import java.io.*;
import java.util.*;

import static net.sf.skey.Main.main;
import static org.junit.Assert.assertEquals;

public class SkeyTest {
    private static final String CONTEXT_PATH = "net/sf/osql/osql.xml";
    private static final String DIALECT_PATH = "net/sf/osql/dialect/mysql.xml";
    @Test
    public void test() throws TransformerException, SAXException, XPathExpressionException, IOException {
        Properties properties = new Properties();
        try(InputStream is = new FileInputStream("test.properties")){
            properties.load(is);
        }
        assertEquals("Please fill test.properties file with your mysql configuration","true", properties.getProperty("updated"));
        String cmd = properties.getProperty("mysql")
                + " -u" + properties.getProperty("user")
                + " -p" + properties.getProperty("password");
        List<String> args = new LinkedList<>(Arrays.asList("-i base.osql -o base.sql -d mysk -m soft -u s_view -s -b".split(" ")));
        args.add(properties.getProperty("database"));
        main(args.toArray(new String[0]));
        String str = new Scanner(new FileInputStream("base.sql"), "UTF-8").useDelimiter("\\Z").next();
        byte[] bytes = str.getBytes();
        Process process = Runtime.getRuntime().exec(cmd);
        try{
            process.getOutputStream().write(bytes);
            process.getOutputStream().close();
        } catch (Exception e) {
            InputStream is = process.getErrorStream();
            byte[] buff = new byte[1024];
            int r;
            StringWriter writer = new StringWriter();
            while((r = is.read(buff)) != -1) {
                writer.append(new String(buff, 0, r));
            }
            throw new AssertionError(writer);
        }
    }
}
