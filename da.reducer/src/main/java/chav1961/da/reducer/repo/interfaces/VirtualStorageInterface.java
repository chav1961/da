package chav1961.da.reducer.repo.interfaces;

import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;
import java.util.concurrent.locks.Lock;

import chav1961.purelib.concurrent.interfaces.ExecutionControl;

public interface VirtualStorageInterface<T> extends Closeable, Flushable, ExecutionControl {
	int read(final long address, final byte[] content, final int offset, final int len) throws IOException;

	default int read(final long hiAddress, final long loAddress, final byte[] content, final int offset, final int len) throws IOException {
		return read(loAddress, content, offset, len);
	}

	void write(final long address, final byte[] content, final int offset, final int len) throws IOException;
	
	default void write(final long hiAddress, final long loAddress, final byte[] content, final int offset, final int len) throws IOException {
		write(loAddress, content, offset, len);
	}

	void allocate(final long address, final long size, final long... parts) throws IOException;

	default void allocate(final long hiAddress, final long loAddress, final long size, final long... parts)  throws IOException {
		allocate(loAddress, size, parts);
	}
	
	void free(final long address, final long size) throws IOException;

	default void free(final long hiAddress, final long loAddress, final long size) throws IOException {
		free(loAddress, size);
	}

	Lock lockRead(final long address, final long size) throws IOException, InterruptedException;
	
	default Lock lockRead(final long hiAddress, final long loAddress, final long size) throws IOException, InterruptedException {
		return lockRead(loAddress, size);
	}
	
	Lock lockWrite(final long address, final long size) throws IOException, InterruptedException;

	default Lock lockWrite(final long hiAddress, final long loAddress, final long size) throws IOException, InterruptedException {
		return lockWrite(loAddress, size);
	}
	
	long getAvailableSize(final long... parts) throws IOException;

	default void getAvailableSize(final long[] result, final long... parts) throws IOException {
		result[0] = getAvailableSize(parts);
	}

	long getUsedSize(final long... parts) throws IOException;
	
	default void getUsedSize(final long[] result, final long... parts) throws IOException {
		result[0] = getUsedSize(parts);
	}

	long getTotalSize(final long... parts) throws IOException;
	
	default void getTolalSize(final long[] result, final long... parts) throws IOException {
		result[0] = getTotalSize(parts);
	}

	void mount(final T something, final long... parts) throws IOException;
	
	boolean canExpand(final long... parts) throws IOException;
	void expand(final long... parts) throws IOException;

	boolean canShrink(final long... parts) throws IOException;
	void shrink(final long... parts) throws IOException;
	
	void maintenance() throws IOException; 

	T umount(final long... parts) throws IOException;
}
