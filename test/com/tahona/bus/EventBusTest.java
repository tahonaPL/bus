package com.tahona.bus;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class EventBusTest {

    private final EventBus eventBus = new EventBus();

    @Before
    public void setup() {
    }

    @Test
    public void test() {
        eventBus.subscribe(this);

        final MyEvent event = new MyEvent();
        eventBus.inform(event);

        Assert.assertEquals(event.getExecutedCount(), 1L);
    }

    class Cs {

        @Subscribe
        private void executeEvent(final MyEvent event) {
            event.assertTrue();
        }
    }

    class Te extends Cs {
        @Subscribe
        private void executeEvent(final MyEvent event) {
            event.assertTrue();
        }
    }

    @Test
    public void testUnsubscribe() {
        final Te subscriber = new Te();
        eventBus.subscribe(subscriber);
        eventBus.unsubscribe(subscriber);

        final MyEvent event = new MyEvent();
        eventBus.inform(event);

        Assert.assertEquals(0, event.getExecutedCount());
    }

    @Test
    public void testMulti() {
        eventBus.subscribe(this);
        eventBus.subscribe(new Te());

        final MyEvent event = new MyEvent();
        eventBus.inform(event);

        Assert.assertEquals(event.getExecutedCount(), 3L);
    }

    @Test
    public void testMultiContext() {
        eventBus.subscribe("some", this);
        eventBus.subscribe(new Te());

        final MyEvent event = new MyEvent();
        eventBus.inform(event);

        Assert.assertEquals(event.getExecutedCount(), 2L);
    }

    @Test
    public void shouldInvokeOnlyFromContext() {
        eventBus.subscribe("some", this);
        eventBus.subscribe(new Te());

        final MyEvent event = new MyEvent();
        eventBus.inform("some", event);

        Assert.assertEquals(event.getExecutedCount(), 1L);
    }

    @Test
    public void shouldClearOnlyFromContext() {
        eventBus.subscribe("some", this);
        eventBus.subscribe(new Te());
        eventBus.clear("some");

        final MyEvent event = new MyEvent();
        eventBus.informAll(event);

        Assert.assertEquals(event.getExecutedCount(), 2L);
    }

    private static class MyEvent extends Event {
        private long i = 0;

        public void assertTrue() {
            i++;
        }

        public long getExecutedCount() {
            return this.i;
        }

    }

    @Subscribe
    private void executeEvent(final MyEvent event) {
        event.assertTrue();
    }
}
