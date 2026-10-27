package io.github.fishstiz.fidgetz.v0.utils;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.util.*;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;

public final class CollectionUtils {
    private CollectionUtils() {
    }

    public static <E> List<E> addFirst(List<E> list, E element) {
        List<E> newList = new ArrayList<>(list.size() + 1);
        newList.add(element);
        newList.addAll(list);
        return newList;
    }

    public static <E> List<E> addLast(List<E> list, E element) {
        List<E> newList = new ArrayList<>(list.size() + 1);
        newList.addAll(list);
        newList.add(element);
        return newList;
    }

    public static <E> List<E> remove(List<E> list, E element) {
        if (list.isEmpty()) {
            return new ArrayList<>();
        }

        List<E> newList = new ArrayList<>(list.size());
        for (E e : list) {
            if (!e.equals(element)) {
                newList.add(e);
            }
        }
        return newList;
    }

    @SafeVarargs
    @SuppressWarnings("varargs")
    public static <E> List<E> addAll(Collection<E>... collections) {
        List<E> result = new ObjectArrayList<>();
        for (Collection<E> collection : collections) {
            result.addAll(collection);
        }
        return result;
    }

    public static <T extends Collection<E>, E> T addIf(T out, Collection<E> add, Predicate<E> predicate) {
        for (E e : add) {
            if (predicate.test(e)) out.add(e);
        }
        return out;
    }

    public static <E> List<E> filter(Collection<E> collection, Predicate<E> filter) {
        List<E> result = new ObjectArrayList<>(collection.size());
        for (E e : collection) {
            if (filter.test(e)) result.add(e);
        }
        return result;
    }

    public static <E, R, C extends Collection<R>> C map(
            Collection<E> collection,
            Function<E, R> mapper,
            IntFunction<C> collectionFactory
    ) {
        C result = collectionFactory.apply(collection.size());
        for (E item : collection) {
            R value = mapper.apply(item);
            result.add(value);
        }
        return result;
    }

    public static <E, R> List<R> map(Collection<E> collection, Function<E, R> mapper) {
        return map(collection, mapper, ObjectArrayList::new);
    }

    public static <E, K> Map<K, E> toMap(Collection<E> collection, Function<E, K> keyFn) {
        Map<K, E> map = new Object2ObjectOpenHashMap<>(collection.size());
        for (E item : collection) {
            map.put(keyFn.apply(item), item);
        }
        return map;
    }


    public static <K, V> List<V> lookup(Collection<K> keys, Map<K, V> source) {
        List<V> result = new ObjectArrayList<>(keys.size());
        for (K key : keys) {
            V v = source.get(key);
            if (v != null) result.add(v);
        }
        return result;
    }

    public static <K, V> List<K> reverseLookup(V value, Map<K, V> map) {
        List<K> keys = new ObjectArrayList<>();
        for (Map.Entry<K, V> entry : map.entrySet()) {
            if (Objects.equals(entry.getValue(), value)) {
                keys.add(entry.getKey());
            }
        }
        return keys;
    }

    public static <E> E first(Collection<E> collection) {
        Iterator<E> iterator = collection.iterator();
        if (!iterator.hasNext()) {
            throw new NoSuchElementException();
        }
        return iterator.next();
    }

    public static <E> E last(Collection<E> collection) {
        Iterator<E> iterator = collection.iterator();
        if (!iterator.hasNext()) {
            throw new NoSuchElementException();
        }
        E last = iterator.next();
        while (iterator.hasNext()) {
            last = iterator.next();
        }
        return last;
    }

    public static <E> List<E> reversed(Collection<E> collection) {
        List<E> result = new ArrayList<>(collection);
        Collections.reverse(result);
        return result;
    }
}
