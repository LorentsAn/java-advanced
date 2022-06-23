package info.kgeorgiy.ja.lorents.concurrent;

import info.kgeorgiy.java.advanced.concurrent.ScalarIP;
import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class IterativeParallelism implements ScalarIP {

    private final ParallelMapper mapper;

    /**
     * Default constructor for ParallelMapper.
     */
    public IterativeParallelism() {
        mapper = null;
    }

    /**
     * Constructor for ParallelMapper.
     * @param mapper {@link ParallelMapper}
     */
    public IterativeParallelism(ParallelMapper mapper) {
        this.mapper = mapper;
    }

    private <T, E> void validateArgs(int threads, List<? extends T> values,
                                     Function<Stream<? extends T>, E> function) throws InterruptedException {
        if (values == null || function == null) {
            throw new InterruptedException("Arguments should not be null");
        }
        if (!(threads > 0)) {
            throw new InterruptedException("Number of threads is not greater than zero");
        }

    }

    private int changeIndex(int fromElement, int elementsInGroup, int remainder) {
        return fromElement + elementsInGroup + (remainder > 0 ? 1 : 0);
    }

    private <T, E> List<E> calculateFunction(List<T> list, int threadsNumber,
                                             Function<Stream<? extends T>, E> function) throws InterruptedException {

        validateArgs(threadsNumber, list, function);
        int numberOfThreads = Math.min(list.size(), threadsNumber);
        int elementsInGroup = list.size() / numberOfThreads;
        int remainder = list.size() % numberOfThreads;

        List<Stream<T>> listOfElements = new ArrayList<>();
        int fromElement = 0;
        int toElement = changeIndex(fromElement, elementsInGroup, remainder);
        for (int i = 0; i < numberOfThreads; ++i) {
            listOfElements.add(list.subList(fromElement, toElement).stream());
            remainder = (remainder > 0 ? --remainder : 0);
            fromElement = toElement;
            toElement = changeIndex(fromElement, elementsInGroup, remainder);
        }

        if (mapper == null) {

            List<E> result = new ArrayList<>(Collections.nCopies(numberOfThreads, null));
            Thread[] threads = new Thread[numberOfThreads];
            for (int i = 0; i < numberOfThreads; i++) {
                final int index = i;
                threads[index] = new Thread(() -> result.set(index, function.apply(listOfElements.get(index))));
                threads[index].start();
            }
            for (int i = 0; i < numberOfThreads; i++) {
                threads[i].join();
            }
            return result;

        } else {
            return mapper.map(function, listOfElements);
        }
    }

    @Override
    public <T> T maximum(int threads, List<? extends T> values, Comparator<? super T> comparator) throws InterruptedException {
        return calculateFunction(values, threads, x -> x.max(comparator).orElse(null)).stream().max(comparator).orElse(null);
    }

    @Override
    public <T> T minimum(int threads, List<? extends T> values, Comparator<? super T> comparator) throws InterruptedException {
        return maximum(threads, values, comparator.reversed());
    }

    @Override
    public <T> boolean all(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return calculateFunction(values, threads, x -> x.allMatch(predicate)).stream().allMatch(Boolean::booleanValue);
    }

    @Override
    public <T> boolean any(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return !all(threads, values, predicate.negate());
    }
}

