package org.example.core;

import java.util.HashMap;
import java.util.Map;

import org.example.core.store.StorageManager;

public class CacheManager {
	public static <T> Cache<T> newCache(String name, int size, Class<T> type) {
		return new LruCache<>(StorageManager.getStore(name, type), size);
	}
}
