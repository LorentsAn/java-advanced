package info.kgeorgiy.ja.lorents.arrayset;

import java.util.*;

public class ArraySet<T> extends AbstractSet<T> implements SortedSet<T> {

    private final List<T> data;
    private final Comparator<? super T> comparator;

    public ArraySet() {
        this.data = Collections.emptyList();
        this.comparator = null;
    }

    public ArraySet(Comparator<? super T> comparator) {
        this.data = Collections.emptyList();
        this.comparator = comparator;
    }

    public ArraySet(Collection<? extends T> data) {
        this(data, null);
    }

    public ArraySet(Collection<? extends T> data, Comparator<? super T> comparator) {
        TreeSet<T> treeSet = new TreeSet<>(comparator);
        treeSet.addAll(data);
        this.data = new ArrayList<>(treeSet);
        this.comparator = comparator;
    }

    private ArraySet(List<T> data, Comparator<? super T> comparator) {
        this.data = data;
        this.comparator = comparator;
    }

    private SortedSet<T> subSet(T fromElement, T toElement, boolean includeToElement) {
        if (isItNotComparable(fromElement, toElement)) {
            throw new IllegalArgumentException("Wrong arguments");
        }
        int leftIndex = getIndex(fromElement, 0);
        int rightIndex = includeToElement ? getIndex(toElement, 1) : getIndex(toElement, 0);
        return new ArraySet<>(data.subList(leftIndex, rightIndex), comparator);
    }

    @Override
    public Iterator<T> iterator() {
        return Collections.unmodifiableList(data).iterator();
    }

    @Override
    public int size() {
        return data.size();
    }

    @Override
    public Comparator<? super T> comparator() {
        return comparator;
    }

    @Override
    public SortedSet<T> subSet(T fromElement, T toElement) {
        return subSet(fromElement, toElement, false);
    }

    @Override
    public SortedSet<T> headSet(T toElement) {
        return (isEmpty() || isItNotComparable(first(), toElement))
                ? new ArraySet<>(Collections.emptyList(), comparator)
                : subSet(first(), toElement, false);
    }

    @Override
    public SortedSet<T> tailSet(T fromElement) {
        return (isEmpty() || isItNotComparable(fromElement, last())) ? new ArraySet<>(Collections.emptyList(), comparator)
                : subSet(fromElement, last(), true);
    }

    @Override
    public T first() {
        return getOutsideElement(0);
    }

    @Override
    public T last() {
        return getOutsideElement(data.size() - 1);
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean contains(Object e) {
        return Collections.binarySearch(data, (T) e, comparator) >= 0;
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    private int getIndex(T element, int include) {
        int index = Collections.binarySearch(data, element, comparator);
        index = index < 0 ? index * (-1) - 1 : index;
        return index + include;
    }

    @SuppressWarnings("unchecked")
    private boolean isItNotComparable(T fromElement, T toElement) {
        if (comparator != null) {
            return comparator.compare(fromElement, toElement) > 0;
        } else {
            if (fromElement instanceof Comparable && toElement instanceof Comparable) {
                return ((Comparable<T>) fromElement).compareTo(toElement) > 0;
            }
        }
        return true;
    }

    private T getOutsideElement(int index) {
        if (size() == 0) {
            throw new NoSuchElementException();
        }
        return data.get(index);
    }
}

