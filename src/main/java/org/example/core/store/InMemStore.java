package org.example.core.store;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

class InMemStore<T> implements Store<T> {
	private final Map<String, T> map = new HashMap<>();

	@Override
	public Optional<T> get(String key) {
		return Optional.ofNullable(map.get(key));
	}

	@Override
	public void put(String key, T value) {
		map.put(key, value);
	}

	@Override
	public T remove(String key) {
		return map.remove(key);
	}

	@Override
	public boolean contains(String key) {
		return map.containsKey(key);
	}
}
