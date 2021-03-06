package ar.edu.itba.it.gossip.proxy.tcp;

import static ar.edu.itba.it.gossip.util.nio.ChannelUtils.closeQuietly;

import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

import ar.edu.itba.it.gossip.util.nio.TCPConversation;

public class ProxiedTCPConversation implements TCPConversation {
    private static final int INPUT_BUFFER_SIZE = 4 * 1024; // bytes
    private static final int OUT_TO_IN_RATIO = 4; // worst case, 'c' becomes
                                                  // '<', which is "&lt;"

    private final TCPStream clientToOrigin;
    private final TCPStream originToClient;
    private Boolean connectingToOrigin = null; // Note: this is completely
                                               // intentional

    protected ProxiedTCPConversation(SocketChannel clientChannel,
            ChannelTerminator terminator) {
        this.clientToOrigin = new TCPStream(clientChannel, INPUT_BUFFER_SIZE,
                null, INPUT_BUFFER_SIZE * OUT_TO_IN_RATIO, terminator);
        this.originToClient = new TCPStream(null, INPUT_BUFFER_SIZE,
                clientChannel, INPUT_BUFFER_SIZE, terminator);
    }

    public void updateSubscription(Selector selector)
            throws ClosedChannelException {
        int clientFlags = clientToOrigin.getFromSubscriptionFlags()
                | originToClient.getToSubscriptionFlags();
        getClientChannel().register(selector, clientFlags, this);

        if (getOriginChannel() == null) {
            return;
        }

        final int originFlags;
        if (connectingToOrigin) {
            originFlags = SelectionKey.OP_CONNECT;
            connectingToOrigin = false;
        } else {
            originFlags = originToClient.getFromSubscriptionFlags()
                    | clientToOrigin.getToSubscriptionFlags();
        }
        getOriginChannel().register(selector, originFlags, this);
    }

    ByteBuffer getReadBufferFor(SocketChannel channel) {
        if (getClientChannel() == channel) {
            return clientToOrigin.getFromBuffer();
        }
        if (getOriginChannel() == channel) {
            return originToClient.getFromBuffer();
        }
        throw new IllegalArgumentException("Unknown socket");
    }

    ByteBuffer getWriteBufferFor(SocketChannel channel) {
        if (getClientChannel() == channel) {
            return originToClient.getToBuffer();
        }
        if (getOriginChannel() == channel) {
            return clientToOrigin.getToBuffer();
        }
        throw new IllegalArgumentException("Unknown socket");
    }

    // NOTE: A handler is activated only when data is written into its stream
    // (that is, on the TCP *read* event of the stream's input channel)
    TCPStreamHandler getHandlerFor(SocketChannel channel) {
        if (getClientChannel() == channel) {
            return clientToOrigin.getHandler();
        }
        if (getOriginChannel() == channel) {
            return originToClient.getHandler();
        }
        throw new IllegalArgumentException("Unknown socket");
    }

    public void closeChannels() {
        closeQuietly(getClientChannel());

        SocketChannel originChannel = getOriginChannel();
        if (originChannel != null) {
            closeQuietly(originChannel);
        }
    }

    SocketChannel getClientChannel() {
        return clientToOrigin.getFromChannel();
    }

    SocketChannel getOriginChannel() {
        return clientToOrigin.getToChannel();
    }

    void connectToOrigin(SocketChannel originChannel) {
        this.clientToOrigin.setToChannel(originChannel);
        this.originToClient.setFromChannel(originChannel);
        connectingToOrigin = true;
        // NOTE: the rest will be handled by updateSubscription
    }

    public TCPStream getClientToOriginStream() {
        return clientToOrigin;
    }

    public TCPStream getOriginToClientStream() {
        return originToClient;
    }

    // FIXME: just for debugging purposes
    public String getBufferName(ByteBuffer buffer) {
        if (buffer == clientToOrigin.getFromBuffer()) {
            return "clientToOrigin.from";
        }
        if (buffer == clientToOrigin.getToBuffer()) {
            return "clientToOrigin.to";
        }
        if (buffer == originToClient.getFromBuffer()) {
            return "originToClient.from";
        }
        if (buffer == originToClient.getToBuffer()) {
            return "originToClient.to";
        }
        throw new IllegalArgumentException("Unknown buffer");
    }
}
