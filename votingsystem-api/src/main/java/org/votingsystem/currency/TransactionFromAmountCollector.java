package org.votingsystem.currency;

import org.votingsystem.model.currency.Transaction;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class TransactionFromAmountCollector implements Collector<Transaction, BigDecimal[], BigDecimal> {

    @Override public Supplier<BigDecimal[]> supplier() {
        //return () -> new BigDecimal[]{BigDecimal.ZERO};//problems with spring-loaded
        return new Supplier<BigDecimal[]>() {
            @Override public BigDecimal[] get() {
                return new BigDecimal[]{BigDecimal.ZERO};
            }
        };
    }

    @Override public BiConsumer<BigDecimal[], Transaction> accumulator() {
        //return (a, t) -> {a[0] = a[0].add(t.getAmount());};//problems with spring-loaded
        return new BiConsumer<BigDecimal[], Transaction>() {
            @Override public void accept(BigDecimal[] bigDecimals, Transaction transaction) {
                bigDecimals[0] = bigDecimals[0].add(transaction.getAmount());
            }
        };
    }

    //to join two accumulators together into one. It is used when collector is executed in parallel
    @Override public BinaryOperator<BigDecimal[]> combiner() {
        //return (a, b) -> { a[0] = a[0].add(b[0]); return a; };//problems with spring-loaded
        return new BinaryOperator<BigDecimal[]>(){
            @Override public BigDecimal[] apply(BigDecimal[] bigDecimals, BigDecimal[] bigDecimals2) {
                bigDecimals[0] = bigDecimals[0].add(bigDecimals2[0]);
                return bigDecimals;
            }
        };
    }

    @Override public Function<BigDecimal[], BigDecimal> finisher() {
        //return a -> a[0];//problems with spring-loaded
        return new Function<BigDecimal[], BigDecimal>(){
            @Override public BigDecimal apply(BigDecimal[] bigDecimals) {
                return bigDecimals[0];
            }
        };
    }

    @Override public Set<Characteristics> characteristics() {
        return Collections.emptySet();
    }

}
