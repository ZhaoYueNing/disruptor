package com.lmax.disruptor;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
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

import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import com.lmax.disruptor.util.Constants;
import com.lmax.disruptor.util.DaemonThreadFactory;

/**
 * @author zhaoyuening
 */
@Fork(1)
@Warmup(iterations = 1, time = 1)
@Measurement(iterations = 2, time = 1)
@State(Scope.Thread)
public class MyJMHDisruptorDemo {

    private RingBuffer<DemoEvent> ringBuffer;
    private Disruptor<DemoEvent> disruptor;

    private static class DemoEvent {
    }

    private static class DemoHandler implements EventHandler<DemoEvent> {
        private Blackhole blackhole;

        public DemoHandler(Blackhole blackhole) {
            this.blackhole = blackhole;
        }

        @Override
        public void onEvent(DemoEvent event, long sequence, boolean endOfBatch) throws Exception {
            DaemonThreadFactory.INSTANCE.newThread(() -> {
                try {
                    Thread.sleep(500L);
                    blackhole.consume(event);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }

    @Setup
    public void setup(final Blackhole blackhole) {
        disruptor = new Disruptor<>(DemoEvent::new, 1 << 5, DaemonThreadFactory.INSTANCE);
        disruptor.handleEventsWith(new DemoHandler(blackhole));
        ringBuffer = disruptor.start();
    }

    @Benchmark
    public void send() {
        long sequence = ringBuffer.next();
        ringBuffer.publish(sequence);
    }

    @TearDown
    public void tearDown() {
        disruptor.shutdown();
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(MyJMHDisruptorDemo.class.getSimpleName())
                .build();
        new Runner(opt).run();
    }

}
