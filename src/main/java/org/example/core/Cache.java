package org.example.core;

import java.util.Optional;

public interface Cache<T> {
	Optional<T> get(String key);
	void put(String key, T value);
	T remove(String key);
	boolean contains(String key);
}
