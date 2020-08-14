package com.github.cszxyang.devtools.concurrent;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

public class ParallelTaskProcessorTest {

    private final Logger logger = LoggerFactory.getLogger(ParallelTaskProcessorTest.class);

    @Test
    public void testProcAsyncExp() {
        long start = System.currentTimeMillis();
        ParallelTaskProcessor.ResultHolder<List<Integer>, Integer, String> fstResultHolder = new ParallelTaskProcessor.ResultHolder<>();
        try {
            fstResultHolder = ParallelTaskProcessor.procAsync(
                    () -> {
                        sleepSeconds(9);
                        return Arrays.asList(1, 2, 3);
                    },
                    () -> {
                        sleepSeconds(3);
                        return 5 / 0;
                    },
                    () -> {
                        sleepSeconds(15);
                        return "awesome";
                    }
            );
        } catch (Exception e) {
            logger.info("exp encountered, e: {}", (Object) e.getStackTrace());
        } finally {
            long cost = System.currentTimeMillis() - start;
            System.out.println(cost);
            Assert.assertTrue(cost > 15);
            Assert.assertNull(fstResultHolder.getFirst());
            Assert.assertNull(fstResultHolder.getSecond());
            Assert.assertNull(fstResultHolder.getThird());
        }
    }

    @Test
    public void testProcAsync() {
        long start = System.currentTimeMillis();
        ParallelTaskProcessor.ResultHolder<List<Integer>, Integer, String> fstResultHolder = ParallelTaskProcessor.procAsync(
                () -> {
                    sleepSeconds(7);
                    return Arrays.asList(1, 2, 3);
                },
                () -> {
                    sleepSeconds(3);
                    return 1;
                },
                () -> {
                    sleepSeconds(15);
                    return "awesome";
                }
        );
        long cost = System.currentTimeMillis() - start;
        Assert.assertTrue(cost > 15);
        Assert.assertArrayEquals(new Integer[]{1, 2, 3}, fstResultHolder.getFirst().toArray(new Integer[0]));
        Assert.assertEquals(1, (int) fstResultHolder.getSecond());
        Assert.assertEquals("awesome", fstResultHolder.getThird());
    }

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