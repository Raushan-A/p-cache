package org.example.core.store;

import java.util.Optional;

public interface Store<T> {
	Optional<T> get(String key);
	void put(String key, T value);
	T remove(String key);
	boolean contains(String key);
}
