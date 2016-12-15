package net.sf.skey.controller;

@FunctionalInterface
public interface Action<In, Out> {
    Out doAction(In argument) throws Exception;
}
