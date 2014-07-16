package com.babblery.yield;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.SynchronousQueue;
import java.util.function.Consumer;

/**
 * A generator that produces values asynchronously.
 *
 * @param <T> The type of the values.
 */
public class Generator<T> implements Iterable<T> {
    private Consumer<Yielder<T>> consumer;
    private Executor executor;

    public Generator(Consumer<Yielder<T>> consumer, Executor executor) {
        this.consumer = consumer;
        this.executor = executor;
    }

    public Generator(Consumer<Yielder<T>> consumer) {
        this(consumer, Executors.newSingleThreadExecutor());
    }

    /**
     * Construct a new generator.
     *
     * @param consumer The consumer that yields values.
     * @param <T>      The value type.
     * @return The iterable generator.
     */
    public static <T> Generator<T> on(Consumer<Yielder<T>> consumer) {
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
    public static <T> Generator<T> on(Consumer<Yielder<T>> consumer, Executor executor) {
        return new Generator<>(consumer, executor);
    }

    @Override
    public Iterator<T> iterator() {
        return new Iterator<T>() {
            private boolean run = false;
            private SynchronousQueue<T> queue = new SynchronousQueue<>();
            private T result = null;
            private boolean done = false;

            private void start() {
                if (!run && !done) {
                    executor.execute(() -> {
                        consumer.accept(new Yielder<>(queue));
                        done = true;
                    });
                    run = true;
                }
            }

            @Override
            public boolean hasNext() {
                if (done && result == null) {
                    return false;
                }

                start();
                try {
                    return (result = queue.take()) != null;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    return false;
                }
            }

            @Override
            public T next() {
                if (done && result == null) {
                    throw new NoSuchElementException();
                }

                start();
                if (result == null) {
                    try {
                        return queue.take();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        return null;
                    }
                } else {
                    T result = this.result;
                    this.result = null;
                    return result;
                }
            }
        };
    }

    public static class Yielder<T> {
        private SynchronousQueue<T> queue;

        public Yielder(SynchronousQueue<T> queue) {
            this.queue = queue;
        }

        /**
         * Yield a value.
         *
         * @param value The value to yield. Must not be null.
         */
        public void value(T value) {
            try {
                queue.put(value);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
