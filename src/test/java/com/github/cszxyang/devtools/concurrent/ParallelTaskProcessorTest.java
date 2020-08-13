package com.github.cszxyang.devtools.concurrent;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class ParallelTaskProcessorTest {

    @Test
    public void test() {
        List<Supplier<Void>> suppliers = new ArrayList<>();
        long start = System.currentTimeMillis();
        suppliers.add(() -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return null;
        });
        suppliers.add(() -> {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return null;
        });
        suppliers.add(() -> {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return null;
        });
        ParallelTaskProcessor.supplyAsync(suppliers, false);
        long cost = System.currentTimeMillis() - start;
        Assert.assertTrue(cost > 5000);
    }
}