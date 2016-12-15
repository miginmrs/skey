package net.sf.skey.controller;

import java.io.InputStream;
import java.io.PrintStream;

public class Argument {
    public final InputStream in;
    public final PrintStream out;
    public final String mode;
    public final String dialect;
    public  final boolean xml;
    public final String user;
    public final String db;

    public Argument(InputStream in, PrintStream out, String mode, String dialect, boolean xml, String user, String db) {
        this.in = in;
        this.out = out;
        this.mode = mode;
        this.dialect = dialect;
        this.xml = xml;
        this.user =  user;
        this.db = db;
    }
}
