package chav1961.da.reducer.repo;

import java.io.File;
import java.io.IOException;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.locks.Lock;

import chav1961.da.reducer.repo.interfaces.VirtualStorageInterface;
import chav1961.purelib.basic.SubstitutableProperties;

public class RAFBasedVirtualStorage implements VirtualStorageInterface<Object> {
	public static final String	KEY_INITIAL_SIZE = "initialSize";
	public static final String	KEY_EXTENT_SIZE = "extentSize";
	public static final String	KEY_NUMBER_OF_EXTENTS = "numberOfExtents";

	private final AsynchronousFileChannel	channel;
	private final boolean					readOnly;	
	
	public RAFBasedVirtualStorage(final File raf, final SubstitutableProperties props, final boolean readOnly) throws IOException {
		if (raf == null) {
			throw new NullPointerException("File can't be null"); 
		}
		else if (props == null) {
			throw new NullPointerException("Properties can't be null"); 
		}
		else {
			if (!raf.exists()) {
				createRAF(raf, props);
			}
			this.readOnly = readOnly;
			if (readOnly) {
				this.channel = AsynchronousFileChannel.open(raf.toPath(), StandardOpenOption.READ);
			}
			else {
				this.channel = AsynchronousFileChannel.open(raf.toPath(), StandardOpenOption.READ, StandardOpenOption.WRITE);
			}
		}
	}

	@Override
	public void close() throws IOException {
		flush();
		channel.close();
	}

	@Override
	public void flush() throws IOException {
		channel.force(true);
	}

	@Override
	public int read(final long address, final byte[] content, final int offset, final int len) throws IOException {
		// TODO Auto-generated method stub
		if (content == null) {
			throw new NullPointerException("Content to read data to can't be null"); 
		}
		else if (offset < 0 || len < 0 || offset + len < 0 || offset+len >= content.length) {
			throw new NullPointerException("Offset ["+offset+"] and/or length ["+len+"] outsize the range 0.."+(content.length-1)); 
		}
		else {
			final long	executive = getExecutiveAddress(address);
			
			return 0;
		}
	}

	@Override
	public void write(final long address, final byte[] content, final int offset, final int len) throws IOException {
		// TODO Auto-generated method stub
		if (content == null) {
			throw new NullPointerException("Content to read data to can't be null"); 
		}
		else if (offset < 0 || len < 0 || offset + len < 0 || offset+len >= content.length) {
			throw new NullPointerException("Offset ["+offset+"] and/or length ["+len+"] outsize the range 0.."+(content.length-1)); 
		}
		else if (readOnly) {
			throw new IllegalStateException("Attempt to write data on read-only mode");
		}
		else {
			final long	executive = getExecutiveAddress(address);

			
		}
	}

	@Override
	public void allocate(final long address, final long size, final long... parts) throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void free(final long address, final long size) throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Lock lockRead(final long address, final long size) throws IOException, InterruptedException {
		// TODO Auto-generated method stub
		final long	executive = getExecutiveAddress(address);
		return null;
	}

	@Override
	public Lock lockWrite(final long address, final long size) throws IOException, InterruptedException {
		// TODO Auto-generated method stub
		final long	executive = getExecutiveAddress(address);
		return null;
	}

	@Override
	public long getAvailableSize(final long... parts) throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public long getUsedSize(final long... parts) throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public long getTotalSize(final long... parts) throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void mount(final Object something, final long... parts) throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean canExpand(final long... parts) throws IOException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void expand(final long... parts) throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean canShrink(final long... parts) throws IOException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void shrink(final long... parts) throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void maintenance() throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Object umount(final long... parts) throws IOException {
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

	protected long getExecutiveAddress(final long virtualAddress) throws IOException {
		return virtualAddress;
	}
	
	public static void createRAF(final File raf, final SubstitutableProperties props) throws IOException {
		// TODO Auto-generated method stub
		
	}
}
