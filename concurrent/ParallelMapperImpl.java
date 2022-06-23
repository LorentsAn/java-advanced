package info.kgeorgiy.ja.lorents.concurrent;

import info.kgeorgiy.java.advanced.mapper.ParallelMapper;
import java.util.*;
import java.util.function.Function;

public class ParallelMapperImpl implements ParallelMapper {

    // :NOTE: явные типы слева
    private final Queue<Runnable> tasks = new ArrayDeque<>();
    private final List<Thread> workers = new ArrayList<>();

    /**
     * Constructor {@link ParallelMapperImpl}.
     * @param threads number of {@link Thread} used.
     */
    public ParallelMapperImpl(int threads) {
        if (!(threads > 0)) {
            throw new IllegalArgumentException("Argument must be greater than zero");
        }
        for (int i = 0; i < threads; i++) {
            final Thread thread = new Thread(() -> {
                try {
                    while (!Thread.interrupted()) {
                        runTaskFromQueue();
                    }
                } catch (InterruptedException e) {
                    // ignore
                } finally {
                    Thread.currentThread().interrupt();
                }
            });
            workers.add(thread);
            thread.start();
        }
    }

    private void runTaskFromQueue() throws InterruptedException {
        Runnable task;
        synchronized (tasks) {
            while (tasks.isEmpty()) {
                tasks.wait();
            }
            task = tasks.poll();
            // :NOTE: redundant
            tasks.notify();
        }
        task.run();
    }

    private class Task<R> {

        private final List<R> ans;
        private int count = 0;
        private final int size;

        public Task(final int size) {
            this.size = size;
            this.ans = new ArrayList<>(Collections.nCopies(size, null));
        }

        private boolean isDone() {
            return count >= size;
        }

        synchronized void increase() {
            count++;
        }

        synchronized void set(int index, final R value) {
            ans.set(index, value);
            increase();
            if (isDone()) {
                notify();
            }

        }

        synchronized List<R> getAnswer() throws InterruptedException {
            while (!isDone()) {
                wait();
            }
            return ans;
        }
    }

    @Override
    public <T, R> List<R> map(Function<? super T, ? extends R> f, List<? extends T> args) throws InterruptedException {

        final int size = args.size();
        Task<R> task = new Task<>(size);

        for (int i = 0; i < size; i++) {
            final int index = i;
            Runnable tmp = () -> {
                task.set(index, f.apply(args.get(index)));
            };
            synchronized (tasks) {
                tasks.add(tmp);
                tasks.notify();
            }
        }
        return task.getAnswer();
    }



    @Override
    public void close() {
        for (Thread thread : workers) {
            thread.interrupt();
        }
        for (Thread worker : workers) {
            try {
                worker.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}

