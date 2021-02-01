package com.lmax.disruptor.examples.demo;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.EventTranslator;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.EventHandlerGroup;
import com.lmax.disruptor.dsl.ProducerType;
import com.lmax.disruptor.util.DaemonThreadFactory;

/**
 * @author zhaoyuening
 */
public class DisruptorDemo {

    public static class LongEvent
    {
        private long val;

        public LongEvent()
        {
        }

        public long getVal()
        {
            return val;
        }

        public void setVal(final long val)
        {
            this.val = val;
        }
    }

    public static class LongEventHandler implements EventHandler<LongEvent>
    {
        private String handlerName;
        private Integer module;

        public LongEventHandler(final String handlerName, final Integer module)
        {
            this.handlerName = handlerName;
            this.module = module;
        }

        @Override
        public void onEvent(final LongEvent event, final long sequence, final boolean endOfBatch) throws Exception
        {
            if (event.getVal() % module == 0)
            {
                System.out.println(handlerName + ": " + event.getVal());
            }
        }
    }

    public static void main(String[] args) throws InterruptedException
    {
        final int bufferSize = 32;
        Disruptor<LongEvent> disruptor = new Disruptor<>(LongEvent::new, bufferSize, DaemonThreadFactory.INSTANCE, ProducerType.SINGLE, new BlockingWaitStrategy());
        disruptor
                .handleEventsWith(new LongEventHandler("Handler 5", 5))
                .then(new LongEventHandler("Handler 3", 3));

        RingBuffer<LongEvent> ringBuffer = disruptor.start();

        System.out.println("start");
        for (int i = 0; i < 1000; i++) {
            long next = ringBuffer.next();
            LongEvent longEvent = ringBuffer.get(next);
            longEvent.setVal(i);
            ringBuffer.publish(next);
        }
        System.out.println("end");

        Thread.sleep(10000L);
    }
}
