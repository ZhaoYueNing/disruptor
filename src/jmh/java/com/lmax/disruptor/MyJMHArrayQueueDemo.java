package com.lmax.disruptor;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import com.lmax.disruptor.util.Constants;
import com.lmax.disruptor.util.DaemonThreadFactory;
import com.lmax.disruptor.util.SimpleEvent;

/**
 * @author zhaoyuening
 */
@Fork(1)
@Warmup(iterations = 1, time = 1)
@Measurement(iterations = 2, time = 1)
@State(Scope.Thread)
public class MyJMHArrayQueueDemo {
    private BlockingQueue<SimpleEvent> queue;
    private volatile boolean consumerRunning;
    private SimpleEvent simpleEvent;

    @Setup
    public void setup(final Blackhole bh ) throws InterruptedException {
        queue = new ArrayBlockingQueue<>(Constants.RINGBUFFER_SIZE);

        final CountDownLatch consumerStartedLatch = new CountDownLatch(1);
        final Thread eventHandler = DaemonThreadFactory.INSTANCE.newThread(() ->
        {
            consumerStartedLatch.countDown();
            while (consumerRunning) {
                SimpleEvent event = queue.poll();
                if (event != null) {
                    bh.consume(event);
                }
            }
        });
        consumerRunning = true;
        eventHandler.start();
        consumerStartedLatch.await();

        simpleEvent = new SimpleEvent();
        simpleEvent.setValue(0);
    }


    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public void producing() throws InterruptedException {
        if (!queue.offer(simpleEvent, 1, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Queue full, benchmark should not experience backpressure");
        }
    }

    @TearDown
    public void tearDown() {
        consumerRunning = false;
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(MyJMHArrayQueueDemo.class.getSimpleName())
                .build();
        new Runner(opt).run();
    }
}
