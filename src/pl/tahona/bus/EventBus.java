package pl.tahona.bus;

import com.google.common.collect.ArrayListMultimap;
import pl.tahona.di.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class EventBus {

    private static final String DEFAULT = "default";

    private final Map<String, EventBusContext> contextMap = new ConcurrentHashMap<String, EventBusContext>();

    private static class EventBusContext {
        EventBusContext(final String name) {
            this.name = name;
        }

        private final String name;

        private final Map<Class<? extends Event>, List<Object>> eventMapWithEventSubscribers = new ConcurrentHashMap<Class<? extends Event>, List<Object>>();
        private final Map<Object, Map<Class<? extends Event>, Collection<Method>>> subscriberAndMethods = new ConcurrentHashMap<Object, Map<Class<? extends Event>, Collection<Method>>>();

        private void put(final Object subscriber, final Map<Class<? extends Event>, Collection<Method>> methodList) {
            subscriberAndMethods.put(subscriber, methodList);
        }

        public void remove(final Object subscriber) {
            subscriberAndMethods.remove(subscriber);

            for (final Class<? extends Event> eventClass : eventMapWithEventSubscribers.keySet()) {
                final List<Object> list = eventMapWithEventSubscribers.get(eventClass);
                list.remove(subscriber);
            }
        }

        private Set<Object> getSubscribers() {
            return subscriberAndMethods.keySet();
        }

        private void invokeEventSubscriberInformMethod(final Event event) {
            if (eventMapWithEventSubscribers.containsKey(event.getClass())) {
                // TODO method execution witn @EventAction

                for (final Object subscriber : eventMapWithEventSubscribers.get(event.getClass())) {
                    final boolean isInstanceOf = subscriber instanceof EventSubscriber;
                    if (isInstanceOf) {
                        ((EventSubscriber) subscriber).inform(event);
                    }
                }
            }
        }

        private void invokeSubscribedMethods(final Event event) {
            if (isRegisteredForMethods(event)) {
                for (final Object object : getSubscribers()) {
                    final Map<Class<? extends Event>, Collection<Method>> map2 = subscriberAndMethods.get(object);

                    if (map2 != null) {
                        final Collection<Method> methodsToInvoke = map2.get(event.getClass());

                        if (methodsToInvoke != null && !methodsToInvoke.isEmpty()) {
                            for (final Method method : methodsToInvoke) {
                                ReflectionUtils.invokeMethodWith(object, method, event);
                            }
                        }
                    }
                }
            }
        }

        private boolean isRegisteredForMethods(final Event event) {
            final Collection<Map<Class<? extends Event>, Collection<Method>>> values = subscriberAndMethods.values();
            for (final Map<Class<? extends Event>, Collection<Method>> map : values) {
                if (map.containsKey(event.getClass())) {
                    return true;
                }
            }
            return false;
        }

        public void clear() {
            eventMapWithEventSubscribers.clear();
            subscriberAndMethods.clear();
        }
    }

    public void subscribe(final Object subscriber) {
        subscribe(DEFAULT, subscriber);
    }

    public void subscribe(final String contextName, final Object subscriber) {
        final List<Method> annotatedMethods = ReflectionUtils.getMethods(subscriber, Subscribe.class, true);
        final ArrayListMultimap<Class<? extends Event>, Method> eventMethodsMap = ArrayListMultimap.create();

        for (final Method method : annotatedMethods) {
            final Class<?>[] parameterTypes = method.getParameterTypes();
            final Class<? extends Event> eventClass = (Class<? extends Event>) parameterTypes[0];
            eventMethodsMap.put(eventClass, method);
        }

        final Map<Class<? extends Event>, Collection<Method>> asMap = eventMethodsMap.asMap();
        getOrCreateContext(contextName).put(subscriber, asMap);
    }

    private EventBusContext getOrCreateContext(final String contextName) {
        EventBusContext eventBusContext = contextMap.get(contextName);
        if (eventBusContext == null) {
            final EventBusContext newContext = new EventBusContext(contextName);
            contextMap.put(contextName, newContext);
            eventBusContext = newContext;
        }
        return eventBusContext;
    }

    public void unsubscribe(final Object subscriber) {
        unsubscribe(DEFAULT, subscriber);
    }

    public void unsubscribe(final String contextName, final Object subscriber) {
        getOrCreateContext(contextName).remove(subscriber);
    }

    public void inform(final Event event) {
        inform(DEFAULT, event);
    }

    public void informAll(final Event event) {
        final Collection<EventBusContext> values = contextMap.values();
        for (final EventBusContext context : values) {
            context.invokeSubscribedMethods(event);
            context.invokeEventSubscriberInformMethod(event);
        }
    }

    public void inform(final String contextName, final Event event) {
        final EventBusContext context = getOrCreateContext(contextName);
        context.invokeSubscribedMethods(event);
        context.invokeEventSubscriberInformMethod(event);
    }

    public void clear(final String contextName) {
        final EventBusContext context = getOrCreateContext(contextName);
        context.clear();
    }

    public void clear() {
        final Collection<EventBusContext> values = contextMap.values();
        for (final EventBusContext c : values) {
            c.clear();
        }
    }
}
