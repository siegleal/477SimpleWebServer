package server;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class ByteBufferInputStreamAdapter extends InputStream {

	private ByteBuffer byteBuffer;
	private SocketChannel channel;
	
	public ByteBufferInputStreamAdapter(SocketChannel channel) {
		this.byteBuffer = ByteBuffer.allocate(10);
		this.channel = channel;
		try {
			this.channel.read(this.byteBuffer);
			this.byteBuffer.rewind();
		} catch (IOException e) {
			
		}
	}
	
	@Override
	public int read() throws IOException {
		if (this.byteBuffer.hasRemaining()) {
			int result = this.byteBuffer.get();
			if (!this.byteBuffer.hasRemaining()) {
				byteBuffer.clear();
				int bytesRead = channel.read(this.byteBuffer);
				if (bytesRead < 0) {
					return bytesRead;
				}
				this.byteBuffer.rewind();
			}
			return result;
		}
		return -1;
	}

}
