package org.example.core.store;

public interface StorageManager {
	static <V> Store<V> getStore(String name, Class<V> vType) {
		return new DiskStore<>(name);
	}
}
