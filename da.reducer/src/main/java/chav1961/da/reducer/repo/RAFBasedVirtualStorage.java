package chav1961.da.reducer.repo;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.locks.Lock;

import chav1961.purelib.basic.SubstitutableProperties;

public class RAFBasedVirtualStorage implements VirtualStorageInterface<Object> {
	public RAFBasedVirtualStorage(final File raf, final SubstitutableProperties props, final boolean readOnly) {
		
	}
	
	
	@Override
	public void close() throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void flush() throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public int read(long address, byte[] content, int offset, int len) throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void write(long address, byte[] content, int offset, int len) throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void allocate(long address, long size, long... parts) throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void free(long address) throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Lock lockRead(long address, long size) throws IOException, InterruptedException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Lock lockWrite(long address, long size) throws IOException, InterruptedException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long getAvailableSize(long... parts) throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public long getUsedSize(long... parts) throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public long getTotalSize(long... parts) throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void mount(Object something, long... parts) throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean canExpand(long... parts) throws IOException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void expand(long... parts) throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean canShrink(long... parts) throws IOException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void shrink(long... parts) throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void maintenance() throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Object umount(long... parts) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public void start() throws Exception {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void suspend() throws Exception {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void resume() throws Exception {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void stop() throws Exception {
		// TODO Auto-generated method stub
		
	}


	@Override
	public boolean isStarted() {
		// TODO Auto-generated method stub
		return false;
	}


	@Override
	public boolean isSuspended() {
		// TODO Auto-generated method stub
		return false;
	}

}
