package org.example;


import java.io.IOException;

import org.example.core.Cache;
import org.example.core.CacheManager;

public class Main {
	public static void main(String[] args) throws IOException {
		Cache<String> cache = CacheManager.newCache("test", 3, String.class);

		cache.put("1", "One");
		cache.put("2", "Two");
		cache.put("3", "Three");
		cache.put("4", "Four");

		System.out.println(cache.get("1"));
		System.out.println(cache.get("2"));
		cache.put("5", "Five");
		System.out.println(cache.get("2"));
		System.out.println(cache.get("3"));
	}
}
