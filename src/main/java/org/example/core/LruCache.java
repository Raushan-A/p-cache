package org.example.core;

import java.util.LinkedList;
import java.util.Optional;

import org.example.core.store.Store;

public class LruCache<T> implements Cache<T> {
	private final Store<T> store;
	private final int size;
	private final LinkedList<String> list = new LinkedList<>();

	public LruCache(Store<T> store, int size) {
		this.store = store;
		this.size = size;
	}

	@Override
	public Optional<T> get(String key) {
		Optional<T> optionalValue = store.get(key);
		optionalValue.ifPresent(v -> {
			moveToFront(key);
		});

		return optionalValue;
	}

	@Override
	public void put(String key, T value) {
		if (store.contains(key)) {
			moveToFront(key);
		} else {
			list.addFirst(key);
		}

		if (list.size() > size) {
			removeLeastElement();
		}

		store.put(key, value);
	}

	@Override
	public T remove(String key) {
		list.remove(key);
		return store.remove(key);
	}

	@Override
	public boolean contains(String key) {
		return store.contains(key);
	}

	private void removeLeastElement() {
		String removed = list.removeLast();
		store.remove(removed);
	}

	private void moveToFront(String element) {
		list.remove(element);
		list.addFirst(element);
	}
}
