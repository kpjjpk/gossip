package ar.edu.itba.it.gossip.admin.tcp;

import static ar.edu.itba.it.gossip.util.nio.ChannelUtils.closeQuietly;

import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

import ar.edu.itba.it.gossip.proxy.tcp.ChannelTerminator;
import ar.edu.itba.it.gossip.proxy.tcp.TCPStream;
import ar.edu.itba.it.gossip.proxy.tcp.TCPStreamHandler;
import ar.edu.itba.it.gossip.util.nio.TCPConversation;

public class UnproxiedTCPConversation implements TCPConversation {
    private static final int BUFFER_SIZE = 1 * 1024; // bytes

    private final TCPStream stream;
    private boolean hasQuit = false;

    protected UnproxiedTCPConversation(SocketChannel channel,
            ChannelTerminator terminator) {
        this.stream = new TCPStream(channel, BUFFER_SIZE, channel, BUFFER_SIZE,
                terminator);
    }

    @Override
    public void updateSubscription(Selector selector)
            throws ClosedChannelException {
        int streamFlags = stream.getFromSubscriptionFlags();
        int streamToFlags = stream.getToSubscriptionFlags();
        stream.getFromChannel().register(selector, streamFlags | streamToFlags,
                this);
    }

    @Override
    public void closeChannels() {
        closeQuietly(stream.getFromChannel());
    }

    public void quit() {
        hasQuit = true;
    }

    public boolean hasQuit() {
        return hasQuit;
    }

    public TCPStream getStream() {
        return stream;
    }

    public ByteBuffer getReadBuffer() {
        return stream.getFromBuffer();
    }

    public ByteBuffer getWriteBuffer() {
        return stream.getToBuffer();
    }

    public TCPStreamHandler getHandler() {
        return stream.getHandler();
    }

    public SocketChannel getChannel() {
        return stream.getFromChannel();
    }
}
