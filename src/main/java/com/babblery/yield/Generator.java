package com.babblery.yield;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.SynchronousQueue;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * A generator that produces values asynchronously.
 *
 * @param <T> The type of the values.
 */
public class Generator<T> implements Iterable<T> {
    private Consumer<Yield<T>> consumer;
    private ExecutorService executor;

    public Generator(Consumer<Yield<T>> consumer, ExecutorService executor) {
        this.consumer = consumer;
        this.executor = executor;
    }

    public Generator(Consumer<Yield<T>> consumer) {
        this(consumer, Executors.newSingleThreadExecutor());
    }

    /**
     * Construct a new generator.
     *
     * @param consumer The consumer that yields values.
     * @param <T>      The value type.
     * @return The iterable generator.
     */
    public static <T> Generator<T> on(Consumer<Yield<T>> consumer) {
        return new Generator<>(consumer);
    }

    /**
     * Construct a new generator.
     *
     * @param consumer The consumer that yields values.
     * @param executor The executor that manages threads.
     * @param <T>      The value type.
     * @return The iterable generator.
     */
    public static <T> Generator<T> on(Consumer<Yield<T>> consumer, ExecutorService executor) {
        return new Generator<>(consumer, executor);
    }

    @Override
    public Iterator<T> iterator() {
        return new Iterator<T>() {
            private SynchronousQueue<Message<T>> queue = new SynchronousQueue<>();
            private Message<T> result = null;
            private Future<?> task = null;

            {
                task = executor.submit(() -> {
                    try {
                        try {
                            consumer.accept(new Yield<>(queue));
                        } catch (BreakException ignored) {
                        }

                        queue.put(new End<>());
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                });
            }

            private Message<T> getResult() throws InterruptedException {
                if (result != null) {
                    return result;
                } else if (task == null || task.isDone()) {
                    return new End<>();
                } else {
                    return result = queue.take();
                }
            }

            @Override
            public boolean hasNext() {
                try {
                    return getResult() instanceof Value;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    return false;
                }
            }

            @Override
            public T next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }

                try {
                    Message<T> newResult = getResult();
                    result = null;
                    return newResult.getValue();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    return null;
                }
            }

            @Override
            protected void finalize() throws Throwable {
                super.finalize();
                if (task != null) {
                    task.cancel(true);
                }
            }
        };
    }

    public Stream<T> stream() {
        return StreamSupport.stream(spliterator(), false);
    }

    public List<T> toList() {
        List<T> list = new ArrayList<>();
        for (T t : this) {
            list.add(t);
        }
        return list;
    }

    private static interface Message<T> {
        public T getValue();
    }

    private static class Value<T> implements Message<T> {
        private T value;

        private Value(T value) {
            this.value = value;
        }

        @Override
        public T getValue() {
            return value;
        }
    }

    private static class End<T> implements Message<T> {
        @Override
        public T getValue() {
            return null;
        }
    }

    public static class Yield<T> {
        private SynchronousQueue<Message<T>> queue;

        public Yield(SynchronousQueue<Message<T>> queue) {
            this.queue = queue;
        }

        /**
         * Yield a value.
         *
         * @param value The value to yield.
         */
        public void value(T value) {
            try {
                queue.put(new Value<>(value));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        /**
         * Stop generation immediately.
         */
        public void end() {
            throw new BreakException();
        }
    }
}
