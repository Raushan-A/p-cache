package org.example.core.store;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class DiskStore<T> implements Store<T> {
	private final int BUCKET_COUNT = 16;
	private final long BUCKET_ELEMENTS_COUNT = 4;
	private final long BUCKET_SIZE = BUCKET_ELEMENTS_COUNT * 8; // 4 element of size 64 bit (long)
	private final String name;
	private RandomAccessFile indexFile;
	private RandomAccessFile dataFile;

	public DiskStore(String name) {
		this.name = name;

		try {
			init();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void init() throws IOException {
		indexFile = new RandomAccessFile(name + ".idx", "rw");
		indexFile.setLength(BUCKET_SIZE * BUCKET_COUNT);

		dataFile = new RandomAccessFile(name + ".dat", "rw");
		if (dataFile.length() == 0) {
			dataFile.writeUTF("data"); // Avoid data address = 0
		}
	}

	@Override
	public Optional<T> get(String key) {
		try {
			Optional<Long> index = findKeyIndex(key);
			if (index.isEmpty()) {
				return Optional.empty();
			}

			indexFile.seek(index.get());
			long dataPointer = indexFile.readLong();
			return Optional.of(readValue(dataPointer));

		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void put(String key, T value) {
		try {
			Optional<Long> index = findKeyIndex(key);
			if (index.isPresent()) { // Key exist
				indexFile.seek(index.get());
				long dataPointer = indexFile.readLong();

				T existingValue = readValue(dataPointer);
				if (existingValue.equals(value)) {
					return;
				}

				deleteKeyValueData(dataPointer);

			} else {
				index = findEmptyIndex(key);
				index.orElseThrow(() -> new RuntimeException("Bucket overflow")); // TODO: Increase no. of buckets and re-distribute elements
			}

			dataFile.seek(dataFile.length()); // seek to end of file. TODO: Find empty space due to removal and try to re-use it
			long dataPointer = dataFile.getFilePointer();

			writeData(dataPointer, key, value);

			indexFile.seek(index.get());
			indexFile.writeLong(dataPointer);

		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public T remove(String key) {
		try {
			int bucket = bucket(key);
			indexFile.seek(bucket * BUCKET_SIZE);

			int i = 0;
			long dataPointer = 0;
			for (;i < BUCKET_ELEMENTS_COUNT; i++) {
				dataPointer = indexFile.readLong();
				if (dataPointer == 0) {
					break;
				}

				String readKey = readKey(dataPointer);
				if (readKey.equals(key)) {
					break;
				}
			}

			if (dataPointer == 0 || i == BUCKET_ELEMENTS_COUNT) {
				return null;
			}

			T value = deleteKeyValueData(dataPointer);

			// Shifting next elements in bucket if present
			deleteAndShiftElements(i, bucket);

			return value;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void deleteAndShiftElements(int index, long bucket) throws IOException {
		long indexPointer = bucket * BUCKET_SIZE + index * 8L;
		indexFile.seek(indexPointer);
		indexFile.skipBytes(8);

		List<Long> nextElements = new ArrayList<>();
		for (int i = index + 1; i < BUCKET_ELEMENTS_COUNT; i++) {
			long dp = indexFile.readLong();
			if (dp == 0) {
				break;
			}

			nextElements.add(dp);
		}

		indexFile.seek(indexPointer);
		for (long element : nextElements) {
			indexFile.writeLong(element);
		}

		indexFile.writeLong(0);
	}

	@Override
	public boolean contains(String key) {
		try {
			return findKeyIndex(key).isPresent();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private int bucket(String key) {
		return key.hashCode() % BUCKET_COUNT;
	}

	private Optional<Long> findKeyIndex(String key) throws IOException {
		int bucket = bucket(key);
		indexFile.seek(bucket * BUCKET_SIZE);

		for (int i = 0; i < BUCKET_ELEMENTS_COUNT; i++) {
			long indexPointer = indexFile.getFilePointer();
			long dataPointer = indexFile.readLong();
			if (dataPointer == 0) {  // No more elements
				return Optional.empty();
			}

			String readKey = readKey(dataPointer);
			if (readKey.equals(key)) {
				return Optional.of(indexPointer);
			}
		}
		return Optional.empty();
	}

	private Optional<Long> findEmptyIndex(String key) throws IOException {
		int hash = bucket(key);
		indexFile.seek(hash * BUCKET_SIZE);
		for (int i = 0; i < BUCKET_ELEMENTS_COUNT; i++) {
			long indexPointer = indexFile.getFilePointer();
			long dataPointer = indexFile.readLong();
			if (dataPointer == 0) {
				return Optional.of(indexPointer);
			}
		}
		return Optional.empty();
	}

	private void writeData(long dataPointer, String key, T value) throws IOException {
		byte[] keyBytes = serialize(key);
		byte[] valueBytes = serialize(value);

		dataFile.seek(dataPointer);
		dataFile.writeInt(keyBytes.length);
		dataFile.write(keyBytes);

		dataFile.writeInt(valueBytes.length);
		dataFile.write(valueBytes);
	}

	private String readKey(long dataPointer) throws IOException {
		dataFile.seek(dataPointer);
		int keySize = dataFile.readInt();
		byte[] keyBytes = new byte[keySize];
		dataFile.read(keyBytes);

		return deserialize(keyBytes);
	}

	private T readValue(long dataPointer) throws IOException {
		dataFile.seek(dataPointer);

		int keySize = dataFile.readInt();
		dataFile.skipBytes(keySize);

		int valueSize = dataFile.readInt();
		byte[] valueBytes = new byte[valueSize];
		dataFile.read(valueBytes);

		return deserialize(valueBytes);
	}

	private <V> V deserialize(byte[] bytes) throws IOException {
		ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
		ObjectInput in = new ObjectInputStream(bis);
		try {
			return  (V) in.readObject();
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	private <V> byte[] serialize(V value) throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ObjectOutput out = new ObjectOutputStream(bos);
		out.writeObject(value);
		return bos.toByteArray();
	}

	private T deleteKeyValueData(long dataPointer) throws IOException {
		dataFile.seek(dataPointer);
		int keySize = dataFile.readInt();

		dataFile.skipBytes(keySize);
		int valueSize = dataFile.readInt();
		byte[] valueBytes = new byte[valueSize];
		dataFile.read(valueBytes);

		byte[] empty = new byte[keySize + valueSize + 4 + 4];
		dataFile.seek(dataPointer);
		dataFile.write(empty);

		return deserialize(valueBytes);
	}
}
