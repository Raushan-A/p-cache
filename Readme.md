
# Disk backed caching


Created as part of an interview assignment

```java
Cache<String> cache = CacheManager.newCache("test", 3, String.class);

cache.put("1", "One");
cache.put("2", "Two");
cache.put("3", "Three");
cache.put("4", "Four");

System.out.println(cache.get("1")); 
System.out.println(cache.get("2")); 
```


### Notes

DiskStore create two file
1. .idx file to store hastable data
2. .dat file to store serialized data 

### TODOs

1. Handle bucket overflow, increase no. of bucket and re-distribute elements 
2. Reduce data fragmentation in data file 
3. Thread safety 

