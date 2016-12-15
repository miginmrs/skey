package net.sf.skey.controller;

import org.apache.commons.cli.*;

import java.io.*;
import java.nio.channels.Channel;
import java.util.Arrays;
import java.util.Scanner;
import java.util.function.Function;

public class CmdUtils {
    public static Function<Action<Argument, Object>, Object> getArgument(String[] args){
        CommandLineParser parser = new BasicParser();
        HelpFormatter formatter = new HelpFormatter(){
            @Override
            public void printHelp(String cmdLineSyntax, Options options) {
                PrintWriter pw = new PrintWriter(System.err);
                this.printHelp(pw, 80, cmdLineSyntax, null, options, 1, 3, null, false);
                pw.flush();
            }
        };
        Options options = new Options();
        options.addOption(new Option("i", "input", true, "input file path"));
        options.addOption(new Option("o", "output", true, "output file path"));
        options.addOption(new Option("m", "mode", true, "database integrity mode (soft|hard)"));
        options.addOption(new Option("u", "user", true, "sql limited access username"));
        options.addOption(new Option("b", "database", true, "sql database name"));
        options.addOption(new Option("d", "dialect", true, "sql output dialect"));
        options.addOption(new Option("h", "help", false, "show this help"));
        options.addOption(new Option("x", "xml", false, "output xml rather than sql"));
        options.addOption(new Option("s", "stack", false, "output full stack trace"));
        CommandLine cmd;
        try {
            cmd = parser.parse(options, args);
            if(!cmd.hasOption('h')) {
                if(!cmd.hasOption('m')) {
                    throw new ParseException("Mode option required");
                }
                if(!Arrays.asList(new String[]{"soft", "hard"}).contains(cmd.getOptionValue('m'))) {
                    throw new ParseException("Bad mode option value");
                }
                if(cmd.hasOption('d')) {
                    if(!cmd.hasOption('u')) {
                        throw new ParseException("User name is required when dialect option is present");
                    }
                    if(!cmd.hasOption('b')) {
                        throw new ParseException("Database name is required when dialect option is present");
                    }
                    if(cmd.hasOption('x') && !cmd.hasOption('o')) {
                        throw new ParseException("When xml and dialect are the two present the output option is required");
                    }
                } else if(!cmd.hasOption('x')) {
                        throw new ParseException("Must either specify the dialect or choose to output xml");
                }
            }
        } catch (ParseException e) {
            System.err.println(e.getMessage());
            formatter.printHelp("skey", options);
            System.exit(1);
            throw new AssertionError();
        }

        if(cmd.hasOption('h')) {
            formatter.printHelp("skey", options, false);
            System.exit(0);
        }
		
		boolean stacktrace = cmd.hasOption('s');

        class Handler {
            private <In, Out> Function<Action<In, Out>, Out> apply(In param) {
                return action -> { try {
                    return action.doAction(param);
                } catch (Exception e) {
					if(stacktrace) e.printStackTrace();
					else System.err.println(e.getMessage());
                    formatter.printHelp("skey", options);
                    System.exit(1);
                    return null;
                }};
            }
        }

        Handler handler = new Handler();

        return handler.apply(handler.<Object, Argument>apply(null).apply(arg -> {
            InputStream in;
            String dialect = cmd.getOptionValue('d');
            if(cmd.hasOption('i')) {
                String path = cmd.getOptionValue('i');
                in = new FileInputStream(path);
            } else {
                in = System.in;
            }
            Process process = Runtime.getRuntime().exec("cpp -P", null, new File(dialect==null?".":dialect));
            OutputStream out = process.getOutputStream();
            out.write(new Scanner(in, "UTF-8").useDelimiter("\\Z").next().getBytes());
            out.close();
            PrintStream printStream = cmd.hasOption('o') ? new PrintStream(new FileOutputStream(cmd.getOptionValue('o'))){
                public void print(String s) {
                    super.print(s.replaceAll("(?<=\n|\\s ) ", "\t"));
                }
            } : System.out;
            return new Argument(process.getInputStream(), printStream, cmd.getOptionValue('m'),  dialect,
                    cmd.hasOption('x'), cmd.getOptionValue('u'), cmd.getOptionValue('b'));
        }));
    }
}
