package server;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class ByteBufferOutputStreamAdapter extends OutputStream {
	
	private ByteBuffer byteBuffer;
	private SocketChannel channel;
	
	public ByteBufferOutputStreamAdapter(SocketChannel channel) {
		this.channel = channel;
		byteBuffer = ByteBuffer.allocate(1);
	}

	@Override
	public void write(int arg0) throws IOException {
		byteBuffer.put((byte)arg0);
		byteBuffer.flip();
		channel.write(byteBuffer);
		byteBuffer.clear();
	}

}
