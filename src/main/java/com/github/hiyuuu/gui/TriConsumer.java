package com.github.hiyuuu.gui;

@FunctionalInterface
interface TriConsumer<A,B,C> {
    void accept(A a, B b, C c);
}