package ar.edu.itba.it.gossip.proxy.xmpp.handler;

import static ar.edu.itba.it.gossip.proxy.xmpp.element.PartialXMPPElement.Type.AUTH_FAILURE;
import static ar.edu.itba.it.gossip.proxy.xmpp.element.PartialXMPPElement.Type.AUTH_FEATURES;
import static ar.edu.itba.it.gossip.proxy.xmpp.element.PartialXMPPElement.Type.AUTH_MECHANISM;
import static ar.edu.itba.it.gossip.proxy.xmpp.element.PartialXMPPElement.Type.AUTH_MECHANISMS;
import static ar.edu.itba.it.gossip.proxy.xmpp.element.PartialXMPPElement.Type.AUTH_REGISTER;
import static ar.edu.itba.it.gossip.proxy.xmpp.element.PartialXMPPElement.Type.AUTH_SUCCESS;
import static ar.edu.itba.it.gossip.proxy.xmpp.element.PartialXMPPElement.Type.OTHER;
import static ar.edu.itba.it.gossip.proxy.xmpp.element.PartialXMPPElement.Type.STREAM_START;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;

import javax.xml.stream.XMLStreamException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import ar.edu.itba.it.gossip.proxy.tcp.TCPStream;
import ar.edu.itba.it.gossip.proxy.xmpp.Credentials;
import ar.edu.itba.it.gossip.proxy.xmpp.ProxiedXMPPConversation;
import ar.edu.itba.it.gossip.proxy.xmpp.element.PartialXMPPElement;
import ar.edu.itba.it.gossip.proxy.xmpp.element.PartialXMPPElement.Type;
import ar.edu.itba.it.gossip.proxy.xmpp.handler.o2c.OriginToClientXMPPStreamHandler;

@RunWith(MockitoJUnitRunner.class)
public class OriginToClientXMPPStreamHandlerTest extends
        AbstractXMPPStreamHandlerTest {
    private static final String DOCUMENT_START = "<?xml version=\"1.0\"?>";

    private static final Credentials credentials = new Credentials(
            "testUsername", "testPassword");

    private static final String FAKE_AUTH = "<auth xmlns=\"urn:ietf:params:xml:ns:xmpp-sasl\" mechanism=\"PLAIN\">"
            + credentials.encode() + "</auth>";

    @Mock
    private ProxiedXMPPConversation conversation;

    @Mock
    private TCPStream originToClient;
    private ByteArrayOutputStream toOrigin;
    private ByteArrayOutputStream toClient;

    private TestOriginToClientXMPPStreamHandler sut;

    @Before
    public void setUp() throws XMLStreamException {
        when(conversation.getCredentials()).thenReturn(credentials);

        toClient = new ByteArrayOutputStream();
        toOrigin = new ByteArrayOutputStream();
        when(originToClient.getOutputStream()).thenReturn(toClient);

        sut = new TestOriginToClientXMPPStreamHandler(conversation,
                originToClient, toOrigin);
    }

    @Test
    public void testAuthSentOnConversationStart() {
        sendOriginsFirstStart();

        assertEquals(FAKE_AUTH, contents(toOrigin));
    }

    @Test
    public void testSendSuccessAndResetStreamThroughOnAuthSuccess() {
        sendOriginsFirstStart();
        toOrigin.reset();

        String successSerialization = "success serialization";
        sendAuthSuccess(successSerialization);

        assertEquals(successSerialization, contents(toClient));
        assertEquals(1, sut.streamResets);
    }

    @Test
    public void testSendStartDocumentAndOriginalStreamStartOnSecondStreamStart() {
        sendOriginsFirstStart();
        toOrigin.reset();

        sendAuthSuccess();
        toClient.reset();

        String streamStartSerialization = "start serialization";
        startStream(streamStartSerialization);
        assertEquals(DOCUMENT_START + streamStartSerialization,
                contents(toClient));
    }

    @Test
    public void testMessagesSentThroughAfterSecondStreamStart() {
        sendOriginsFirstStart();
        toOrigin.reset();

        sendAuthSuccess();
        assertEquals(1, sut.twinAwakens);
        toClient.reset();

        startStream();
        toClient.reset();

        assertElementIsSentThroughToClient(OTHER, "<a>", "some body for a",
                "</a>");
        toClient.reset();

        assertElementIsSentThroughToClient(OTHER, "<b>", "some body for b",
                "</b>");
    }

    private void sendOriginsFirstStart() {
        sut.handleStart(xmppElement(STREAM_START));
        // features
        sut.handleStart(xmppElement(AUTH_FEATURES));
        // register
        sendComplete(AUTH_REGISTER, "", "");
        // mechanisms
        sut.handleStart(xmppElement(AUTH_MECHANISMS));
        sendComplete(AUTH_MECHANISM, "", "");
        sendComplete(AUTH_MECHANISM, "", "");
        sut.handleEnd(xmppElement(AUTH_MECHANISMS));
        sut.handleEnd(xmppElement(AUTH_FEATURES));
    }

    private void sendAuthSuccess(String serialization) {
        sendComplete(AUTH_SUCCESS, serialization);
    }

    private void sendAuthSuccess() {
        sendAuthSuccess("");
    }

    private void assertElementIsSentThroughToClient(Type type, String startTag,
            String... rest) {
        sendComplete(type, startTag, rest);

        String serialization = startTag + stream(rest).collect(joining());
        assertEquals(serialization, contents(toClient));
    }

    @Override
    protected XMPPStreamHandler getSUT() {
        return sut;
    }

    // class needed for method overrides
    private static class TestOriginToClientXMPPStreamHandler extends
            OriginToClientXMPPStreamHandler {
        int streamResets = 0;
        int twinAwakens = 0;
        int endedTwin = 0;

        TestOriginToClientXMPPStreamHandler(
                ProxiedXMPPConversation conversation, TCPStream originToClient,
                OutputStream toOrigin) throws XMLStreamException {
            super(conversation, originToClient, toOrigin);
        }

        @Override
        protected void resetStream() { // stub
            streamResets++;
        }

        @Override
        protected void resumeTwin() {
            twinAwakens++;
        }

        @Override
        protected void endTwinsHandling() {
            endedTwin++;
        }

        @Override
        protected void sendToClient(PartialXMPPElement element) { // TODO:
                                                                  // remove this
                                                                  // once sysos
                                                                  // that do
                                                                  // casts are
                                                                  // removed
                                                                  // from the
                                                                  // original
                                                                  // method
            sendToClient(element.serializeCurrentContent());
        }
    }
}
