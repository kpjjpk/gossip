package ar.edu.itba.it.gossip.proxy.xml;

import static com.fasterxml.aalto.AsyncXMLStreamReader.EVENT_INCOMPLETE;
import static javax.xml.stream.XMLStreamConstants.CHARACTERS;
import static javax.xml.stream.XMLStreamConstants.END_DOCUMENT;
import static javax.xml.stream.XMLStreamConstants.END_ELEMENT;
import static javax.xml.stream.XMLStreamConstants.START_DOCUMENT;
import static javax.xml.stream.XMLStreamConstants.START_ELEMENT;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import javax.xml.stream.XMLStreamException;

import ar.edu.itba.it.gossip.proxy.tcp.DeferredConnector;
import ar.edu.itba.it.gossip.proxy.tcp.TCPStreamHandler;

import com.fasterxml.aalto.AsyncByteBufferFeeder;
import com.fasterxml.aalto.AsyncXMLInputFactory;
import com.fasterxml.aalto.AsyncXMLStreamReader;
import com.fasterxml.aalto.stax.InputFactoryImpl;

public abstract class XMLStreamHandler implements TCPStreamHandler {
    private static final byte LT = 0x3C;
    private static final byte GT = 0x3E;

    private static final AsyncXMLInputFactory inputFactory = new InputFactoryImpl();

    private XMLEventHandler xmlHandler;
    private AsyncXMLStreamReader<AsyncByteBufferFeeder> reader;
    private DeferredConnector connector;

    protected XMLStreamHandler() throws XMLStreamException {
        this.reader = newReader();
    }

    @Override
    public int handleRead(ByteBuffer buf, final DeferredConnector connector) {
        this.connector = connector;
        try {
            reader.getInputFeeder().feedInput(buf);

            while (reader.hasNext()) {
                int type = reader.next();
                switch (type) {
                case START_DOCUMENT:
                    xmlHandler.handleStartDocument(reader);
                    break;
                case END_DOCUMENT:
                    xmlHandler.handleEndDocument(reader);
                    break;
                case START_ELEMENT:
                    xmlHandler.handleStartElement(reader);
                    break;
                case CHARACTERS:
                    xmlHandler.handleCharacters(reader);
                    break;
                case END_ELEMENT:
                    xmlHandler.handleEndElement(reader);
                    break;
                }
                if (type == EVENT_INCOMPLETE) {
                    break;
                }
            }
        } catch (XMLStreamException e) {
            throw new RuntimeException(e);
        }
        return getNewPosition(buf);
    }

    private int getNewPosition(ByteBuffer buffer) { // because thank you aalto
                                                    // for not showing offsets
        byte[] bytes = buffer.array();
        for (int i = buffer.limit() - 1; i > buffer.position(); i--) {
            if (bytes[i] == GT) { // "...>X" -> X's position
                return buffer.limit();
            }
            if (bytes[i] == LT) { // "...<" -> <'s position
                return i;
            }
        }
        return buffer.limit(); // "..."X -> X's position (X hasn't been read
                               // yet)
    }

    protected void resetStream() {
        try {
            reader.close();
            reader = newReader();
        } catch (XMLStreamException e) {
            throw new RuntimeException(e);
        }
    }

    private AsyncXMLStreamReader<AsyncByteBufferFeeder> newReader()
            throws XMLStreamException {
        return inputFactory.createAsyncFor(ByteBuffer.allocate(0));
    }

    protected DeferredConnector getConnector() {
        return connector;
    }

    public void setXMLEventHandler(final XMLEventHandler eventHandler) {
        this.xmlHandler = eventHandler;
    }

    @Override
    public void handleEndOfInput() {
        reader.getInputFeeder().endOfInput();
    }

    protected void writeTo(OutputStream stream, String payload) {
        try {
            stream.write(payload.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
