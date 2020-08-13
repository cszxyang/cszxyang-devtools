package com.github.cszxyang.devtools.concurrent;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

public class ParallelTaskProcessorTest {

    @Test
    public void testProcAsync4NonDistinctRes() {
        List<Supplier<Integer>> suppliers = prepareSuppliers();
        long start = System.currentTimeMillis();
        Collection<Integer> integers = ParallelTaskProcessor.procAsync(suppliers, Boolean.FALSE);
        long cost = System.currentTimeMillis() - start;
        Assert.assertTrue(cost > 5000);
        Assert.assertArrayEquals(new Integer[]{2, 3, 2}, integers.toArray(new Integer[0]));
    }

    @Test
    public void testProcAsync4DistinctRes() {
        List<Supplier<Integer>> suppliers = prepareSuppliers();
        long start = System.currentTimeMillis();
        Collection<Integer> integers = ParallelTaskProcessor.procAsync(suppliers, Boolean.TRUE);
        long cost = System.currentTimeMillis() - start;
        Assert.assertTrue(cost > 5000);
        Assert.assertArrayEquals(new Integer[]{2, 3}, integers.toArray(new Integer[0]));
    }

    private List<Supplier<Integer>> prepareSuppliers() {
        List<Supplier<Integer>> suppliers = new ArrayList<>();
        suppliers.add(() -> {
            sleepSeconds(1);
            return 2;
        });
        suppliers.add(() -> {
            sleepSeconds(2);
            return 3;
        });
        suppliers.add(() -> {
            sleepSeconds(5);
            return 2;
        });
        return suppliers;
    }

    private void sleepSeconds(int seconds) {
        try {
            Thread.sleep(seconds * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}