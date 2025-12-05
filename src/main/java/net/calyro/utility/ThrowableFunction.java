package net.calyro.utility;

public interface ThrowableFunction<T, V, E extends Throwable> {

    V apply(T t) throws E;

}
