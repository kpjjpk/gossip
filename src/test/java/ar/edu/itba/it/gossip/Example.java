package ar.edu.itba.it.gossip;

import java.io.IOException;

import ar.edu.itba.it.gossip.async.tcp.TCPReactor;
import ar.edu.itba.it.gossip.async.tcp.TCPReactorImpl;
import ar.edu.itba.it.gossip.proxy.xmpp.XMPPProxy;

public class Example {
    public static void main(String[] args) throws IOException {
        short proxyPort = 9998;
        // short originPort = 5222;

        TCPReactor reactor = new TCPReactorImpl();
        reactor.addHandler(new XMPPProxy(reactor), proxyPort);

        reactor.start();
    }
}
