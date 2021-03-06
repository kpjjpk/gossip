package ar.edu.itba.it.gossip.proxy.xmpp.handler.c2o;

import static ar.edu.itba.it.gossip.util.xmpp.XMPPUtils.streamError;

import java.io.OutputStream;

import javax.xml.stream.XMLStreamException;

import ar.edu.itba.it.gossip.proxy.configuration.ProxyConfig;
import ar.edu.itba.it.gossip.proxy.tcp.DeferredConnector;
import ar.edu.itba.it.gossip.proxy.tcp.TCPStream;
import ar.edu.itba.it.gossip.proxy.xmpp.Credentials;
import ar.edu.itba.it.gossip.proxy.xmpp.ProxiedXMPPConversation;
import ar.edu.itba.it.gossip.proxy.xmpp.element.Message;
import ar.edu.itba.it.gossip.proxy.xmpp.element.PartialXMPPElement;
import ar.edu.itba.it.gossip.proxy.xmpp.handler.XMPPHandlerState;
import ar.edu.itba.it.gossip.proxy.xmpp.handler.XMPPStreamHandler;
import ar.edu.itba.it.gossip.util.xmpp.XMPPError;

public class ClientToOriginXMPPStreamHandler extends XMPPStreamHandler {
    private static final ProxyConfig proxyConfig = ProxyConfig.getInstance();

    private final ProxiedXMPPConversation conversation;
    private final OutputStream toOrigin;
    private final OutputStream toClient;

    private XMPPHandlerState<ClientToOriginXMPPStreamHandler> state = InitialState
            .getInstance();
    private boolean clientNotifiedOfMute;
    private boolean clientCauseOfMute;

    public ClientToOriginXMPPStreamHandler(
            final ProxiedXMPPConversation conversation,
            final TCPStream clientToOrigin, final OutputStream toClient)
            throws XMLStreamException {
        super(clientToOrigin);
        this.conversation = conversation;
        this.toOrigin = clientToOrigin.getOutputStream();
        this.toClient = toClient;
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

    @Override
    protected void sendErrorToClient(XMPPError error) {
        sendToClient(streamError(error));
        endHandling();
    }

    protected void sendToOrigin(String message) {
        writeTo(toOrigin, message);
    }

    protected void sendToClient(String message) {
        writeTo(toClient, message);
    }

    protected void sendToOrigin(PartialXMPPElement element) {
        String currentContent = element.serializeCurrentContent();
        sendToOrigin(currentContent);
    }

    String getCurrentUser() {
        return conversation.getCredentials().getUsername();
    }

    boolean isMuted(Message message) {
        String to = message.getReceiver();
        return isClientMuted() || isJIDMuted(to);
    }

    boolean isClientMuted() {
        return isJIDMuted(proxyConfig.getJID(getCurrentUser()));
    }

    private boolean isJIDMuted(String jid) {
        return proxyConfig.isJIDSilenced(jid);
    }

    void setState(final XMPPHandlerState<ClientToOriginXMPPStreamHandler> state) {
        this.state = state;
    }

    void setCredentials(Credentials credentials) {
        conversation.setCredentials(credentials);
    }

    @Override
    protected DeferredConnector getConnector() { // just for visibility
        return super.getConnector();
    }

    @Override
    protected void resetStream() { // just for visibility
        super.resetStream();
    }

    @Override
    protected void waitForTwin() { // just for visibility
        super.waitForTwin();
    }

    boolean isClientNotifiedOfMute() {
        return clientNotifiedOfMute;
    }

    void setClientNotifiedOfMute(final boolean value) {
        this.clientNotifiedOfMute = value;
    }

    boolean isClientCauseOfMute() {
        return clientCauseOfMute;
    }

    void setClientCauseOfMute(final boolean value) {
        this.clientCauseOfMute = value;
    }
}
