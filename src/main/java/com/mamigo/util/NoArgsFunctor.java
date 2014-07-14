package com.mamigo.util;

public interface NoArgsFunctor<R,T> extends Functor<T> {
public R call(T obj);
}
