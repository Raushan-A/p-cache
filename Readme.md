
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


## DiskStore

DiskStore creates two file
1. .idx file to store hashtable data
2. .dat file to store serialized data 

### Operations 

#### Contains key

1. Calculate bucket no. by `hashcode % NO_OF_BUCKETS`
2. Seek to bucket location of index file, read `long` value for data address
   1. If data address = 0, return `false`
   2. If data address is non-zero, read key data from data file by seeking to data address
      1. If key is equals to provided key returns true
      2. If it's not _(hash collision case)_, repeat steps 2.i for by reading next long value in bucket until data address = 0 or all elements in bucket read  

#### Get

1. Check key contains as above steps in **Contains key** section
   1. If key not found return empty optional
   2. If key found read value data using data address from above steps by skipping to data address and skipping key data and read data value from data file
   3. Deserialize value data and return optional with data

#### Put

1. Find the key contains as above steps in **Contains key** section
   1. If key not found and next data address in bucket is = 0  _(new entry case)_
      1. Seek to end of data file - keep the file pointer value 
      2. Serialize key object to byte array
      3. Write key byte array size and then byte array 
      4. Serialize value object to value byte array
      5. Write value byte array size and then byte array
      6. Write file pointer value kept in step 1.i.a in index file at bucket location
   2. If key is found, read value data by seeking to the data address from data file
      1. If value is same as provided value, return the method _(same key-value update)_
      2. If value is not same _(value update case)_
         1. Seek to end of file and write key-value data and update data address to index file(as 1.i.a - 1.1.f)
         2. Seek back to previous data address (from 1.ii) in data file, write (key size + data size + 4 + 4) bytes with zero value to remove the data
   3. If key is not fond and all element in bucket have non-zero value (i.e. no space in bucket) - _(bucket overflow case - not yet implemented)_ 
      1. Create new index file with twice the bucket
      2. Read all values from existing index file
      3. Read respective keys - recompute the bucket no. and move it to new bucket in new file 
      4. Recompute the bucket for provided key and add new entry as per steps 1.i.a - 1.i.f

#### Remove

1. Find the key contains as above steps in **Contains key** section
   1. If key not found, return null
   2. If key found 
      1. Read the data value 
      2. Seek back to data address and write (key size + data size + 4 + 4) bytes with zero value to remove the data
      3. Write zero to current location in index file
      4. If any next element in bucket shift them up

### TODOs

1. Handle bucket overflow, increase no. of bucket and re-distribute elements 
2. Reduce data fragmentation in data file 
3. Thread safety 

