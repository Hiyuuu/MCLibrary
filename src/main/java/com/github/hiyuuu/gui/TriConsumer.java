package com.github.hiyuuu.gui;

@Deprecated
@FunctionalInterface
interface TriConsumer<A,B,C> {
    void accept(A a, B b, C c);
}