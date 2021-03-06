package ar.edu.itba.it.gossip.proxy.xmpp.handler;

import static ar.edu.itba.it.gossip.proxy.xmpp.element.PartialXMPPElement.Type.OTHER;
import static ar.edu.itba.it.gossip.proxy.xmpp.element.PartialXMPPElement.Type.STREAM_START;
import static ar.edu.itba.it.gossip.util.XMLUtils.DOCUMENT_START;
import static ar.edu.itba.it.gossip.util.XMLUtils.attr;
import static ar.edu.itba.it.gossip.util.XMLUtils.attributes;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;

import javax.xml.stream.XMLStreamException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import ar.edu.itba.it.gossip.proxy.tcp.DeferredConnector;
import ar.edu.itba.it.gossip.proxy.tcp.TCPStream;
import ar.edu.itba.it.gossip.proxy.xmpp.Credentials;
import ar.edu.itba.it.gossip.proxy.xmpp.ProxiedXMPPConversation;
import ar.edu.itba.it.gossip.proxy.xmpp.element.Auth;
import ar.edu.itba.it.gossip.proxy.xmpp.element.PartialXMPPElement;
import ar.edu.itba.it.gossip.proxy.xmpp.handler.c2o.ClientToOriginXMPPStreamHandler;

@RunWith(MockitoJUnitRunner.class)
public class ClientToOriginXMPPStreamHandlerTest extends
        AbstractXMPPStreamHandlerTest {
    private static final String FAKE_STREAM_START_FOR_CLIENT = "<stream:stream"
            + attributes(
                    attr("xmlns:stream", "http://etherx.jabber.org/streams"),
                    attr("version", "1.0"), attr("xmlns", "jabber:client"),
                    attr("xml:lang", "en"),
                    attr("xmlns:xml", "http://www.w3.org/XML/1998/namespace"))
            + ">";
    private static final String FAKE_STREAM_AUTH_FEATURES = "<stream:features>"
            + "<register"
            + attributes(attr("xmlns", "http://jabber.org/features/iq-register"))
            + "/>" + "<mechanisms"
            + attributes(attr("xmlns", "urn:ietf:params:xml:ns:xmpp-sasl"))
            + ">" + "<mechanism>PLAIN</mechanism>" + "</mechanisms>"
            + "</stream:features>";

    private static final String FAKE_STREAM_START_FOR_ORIGIN = "<stream:stream"
            + attributes(
                    attr("xmlns:stream", "http://etherx.jabber.org/streams"),
                    attr("version", "1.0"), attr("xmlns", "jabber:client"),
                    attr("xml:lang", "en"),
                    attr("xmlns:xml", "http://www.w3.org/XML/1998/namespace"),
                    attr("from", "testUsername@localhost"),
                    attr("to", "localhost")) + ">";

    private static final String CURRENT_USER = "testUsername";

    private static final Credentials CREDENTIALS = new Credentials(
            CURRENT_USER, "testPassword");

    @Mock
    private ProxiedXMPPConversation conversation;

    @Mock
    private TCPStream clientToOrigin;
    private ByteArrayOutputStream toOrigin;
    private ByteArrayOutputStream toClient;

    private TestClientToOriginXMPPStreamHandler sut;

    @Before
    public void setUp() throws XMLStreamException {
        when(conversation.getCredentials()).thenReturn(CREDENTIALS);

        toOrigin = new ByteArrayOutputStream();
        toClient = new ByteArrayOutputStream();
        when(clientToOrigin.getOutputStream()).thenReturn(toOrigin);

        sut = new TestClientToOriginXMPPStreamHandler(conversation,
                clientToOrigin, toClient);
    }

    @Test
    public void testSendStreamStartAndFeaturesToClientOnConversationStart() {
        sut.handleStart(xmppElement(STREAM_START));
        assertEquals(DOCUMENT_START + FAKE_STREAM_START_FOR_CLIENT
                + FAKE_STREAM_AUTH_FEATURES, contents(toClient));
    }

    @Test
    public void testSetCredentialsAndStartConversationWithOriginOnAuthEnd() {
        startStream();
        toClient.reset();

        sendAuthAsClient();

        verify(conversation, times(1)).setCredentials(CREDENTIALS);
        assertEquals(1, sut.connectionAttempts);
        assertEquals(1, sut.streamResets);
        assertEquals(DOCUMENT_START + FAKE_STREAM_START_FOR_ORIGIN,
                contents(toOrigin));
    }

    @Test
    public void testSendDocumentStartAndOriginalStreamStartToOriginOnSecondStreamStart() {
        startStream();
        toClient.reset();

        sendAuthAsClient();
        toOrigin.reset();

        String clientStreamStartSerialization = "serialization of client's stream start";
        startStream(clientStreamStartSerialization);

        assertEquals(DOCUMENT_START + clientStreamStartSerialization,
                contents(toOrigin));
    }

    @Test
    public void testMessagesSentThroughAfterSecondStreamStart() {
        startStream();
        toClient.reset();

        sendAuthAsClient();
        toOrigin.reset();

        startStream("serialization of client's stream start");
        toOrigin.reset();

        assertElementIsSentThrough("<a>", "some text", "</a>");
        toOrigin.reset();
        assertElementIsSentThrough("<b>", "some other text", "</b>");
    }

    private void sendAuthAsClient() {
        Auth auth = auth(CREDENTIALS);
        sut.handleStart(auth);
        sut.handleEnd(auth);
    }

    private void assertElementIsSentThrough(String startTag, String body,
            String endTag) {
        sendComplete(OTHER, startTag, body, endTag);
        assertEquals(startTag + body + endTag, contents(toOrigin));
    }

    @Override
    protected XMPPStreamHandler getSUT() {
        return sut;
    }

    // class needed for method overrides
    private static class TestClientToOriginXMPPStreamHandler extends
            ClientToOriginXMPPStreamHandler {
        int streamResets = 0;
        int connectionAttempts = 0;

        TestClientToOriginXMPPStreamHandler(
                ProxiedXMPPConversation conversation, TCPStream clientToOrigin,
                OutputStream toClient) throws XMLStreamException {
            super(conversation, clientToOrigin, toClient);
        }

        @Override
        protected void resetStream() { // stub
            streamResets++;
        }

        @Override
        protected DeferredConnector getConnector() {
            return address -> connectionAttempts++;
        }

        @Override
        protected void sendToOrigin(PartialXMPPElement element) { // TODO:
                                                                  // remove this
                                                                  // once sysos
                                                                  // that do
                                                                  // casts are
                                                                  // removed
                                                                  // from the
                                                                  // original
                                                                  // method
            sendToOrigin(element.serializeCurrentContent());
        }
    }
}
