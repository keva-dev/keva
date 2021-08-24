package storage;

import java.nio.file.Path;

import com.jinyframework.keva.store.NoHeapStore;

public interface StorageService {
	// setter dep injection
	void setStore(NoHeapStore store);

	Path getSnapshotPath();

	boolean putString(String key, String val);

	String getString(String key);

	boolean remove(String key);
}

