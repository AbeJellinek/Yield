package com.babblery.yield;

import java.util.Iterator;
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
    private Consumer<Yielder<T>> consumer;
    private ExecutorService executor;

    public Generator(Consumer<Yielder<T>> consumer, ExecutorService executor) {
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
    public static <T> Generator<T> on(Consumer<Yielder<T>> consumer, ExecutorService executor) {
        return new Generator<>(consumer, executor);
    }

    @Override
    public Iterator<T> iterator() {
        return new Iterator<T>() {
            private boolean run = false;
            private SynchronousQueue<T> queue = new SynchronousQueue<>();
            private T result = null;
            private Future<?> task = null;

            private void start() {
                if (!run && task == null) {
                    task = executor.submit(() -> {
                        consumer.accept(new Yielder<>(queue));
                        task = null;
                    });
                    run = true;
                }
            }

            @Override
            public boolean hasNext() {
                start();

                if (task == null && result == null) {
                    return false;
                }

                try {
                    return (result = queue.take()) != null;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    return false;
                }
            }

            @Override
            public T next() {
                start();

                if (task == null && result == null) {
                    throw new NoSuchElementException();
                }

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
