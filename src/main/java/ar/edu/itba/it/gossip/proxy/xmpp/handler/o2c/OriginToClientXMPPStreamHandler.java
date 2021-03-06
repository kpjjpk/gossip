package ar.edu.itba.it.gossip.proxy.xmpp.handler.o2c;

import static ar.edu.itba.it.gossip.util.xmpp.XMPPUtils.streamError;

import java.io.OutputStream;

import javax.xml.stream.XMLStreamException;

import ar.edu.itba.it.gossip.proxy.configuration.ProxyConfig;
import ar.edu.itba.it.gossip.proxy.tcp.TCPStream;
import ar.edu.itba.it.gossip.proxy.xmpp.ProxiedXMPPConversation;
import ar.edu.itba.it.gossip.proxy.xmpp.element.Message;
import ar.edu.itba.it.gossip.proxy.xmpp.element.PartialXMPPElement;
import ar.edu.itba.it.gossip.proxy.xmpp.handler.XMPPHandlerState;
import ar.edu.itba.it.gossip.proxy.xmpp.handler.XMPPStreamHandler;
import ar.edu.itba.it.gossip.util.xmpp.XMPPError;

public class OriginToClientXMPPStreamHandler extends XMPPStreamHandler {
    private static final ProxyConfig proxyConfig = ProxyConfig.getInstance();

    private final ProxiedXMPPConversation conversation;
    private final OutputStream toClient;
    private final OutputStream toOrigin;

    private XMPPHandlerState<OriginToClientXMPPStreamHandler> state = InitialState
            .getInstance();

    public OriginToClientXMPPStreamHandler(
            final ProxiedXMPPConversation conversation,
            final TCPStream originToClient, final OutputStream toOrigin)
            throws XMLStreamException {
        super(originToClient);
        this.conversation = conversation;
        this.toOrigin = toOrigin;
        this.toClient = originToClient.getOutputStream();
    }

    @Override
    public void handleStart(PartialXMPPElement element) {
        state.handleStart(this, element);
    }

    @Override
    public void handleBody(PartialXMPPElement element) {
        state.handleBody(this, element);
    }

    @Override
    public void handleEnd(PartialXMPPElement element) {
        state.handleEnd(this, element);
    }

    @Override
    public void handleError(XMLStreamException xmlEx) {
        state.handleError(this, xmlEx);
    }

    String getCurrentUser() {
        return conversation.getCredentials().getUsername();
    }

    boolean isMuted(Message message) {
        String from = message.getSender();
        if (from == null) { // administrative message from own server
            return isClientMuted();
        }
        return isJIDMuted(from) || isClientMuted();
    }

    boolean isClientMuted() {
        return isJIDMuted(proxyConfig.getJID(getCurrentUser()));
    }

    private boolean isJIDMuted(String jid) {
        return proxyConfig.isJIDSilenced(jid);
    }

    String encodeCredentials() {
        return conversation.getCredentials().encode();
    }

    protected void sendToClient(PartialXMPPElement element) {
        String currentContent = element.serializeCurrentContent();
        sendToClient(currentContent);
    }

    protected void sendToClient(String message) {
        writeTo(toClient, message);
    }

    protected void sendToOrigin(String payload) {
        writeTo(toOrigin, payload);
    }

    @Override
    protected void resetStream() { // just for visibility
        super.resetStream();
    }

    @Override
    protected void resumeTwin() { // just for visibility
        super.resumeTwin();
    }

    void setState(final XMPPHandlerState<OriginToClientXMPPStreamHandler> state) {
        this.state = state;
    }

    @Override
    protected void sendErrorToClient(XMPPError error) {
        sendToClient(streamError(error));
        endHandling();
        endTwinsHandling();
    }
}
