package com.searchstax.aem.connector.core.utils;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 * SSLSocketFactory that always negotiates TLS 1.2+ for SMTP STARTTLS upgrades on Java 11.
 */
final class Tls12SocketFactory extends SSLSocketFactory {

    private static final String[] ENABLED_PROTOCOLS = new String[] { "TLSv1.2", "TLSv1.3" };

    private final SSLSocketFactory delegate;

    Tls12SocketFactory() throws NoSuchAlgorithmException, KeyManagementException {
        final SSLContext context = SSLContext.getInstance("TLS");
        context.init(null, null, new SecureRandom());
        this.delegate = context.getSocketFactory();
    }

    @Override
    public String[] getDefaultCipherSuites() {
        return delegate.getDefaultCipherSuites();
    }

    @Override
    public String[] getSupportedCipherSuites() {
        return delegate.getSupportedCipherSuites();
    }

    @Override
    public Socket createSocket(final Socket socket, final String host, final int port, final boolean autoClose)
            throws IOException {
        return configure(delegate.createSocket(socket, host, port, autoClose));
    }

    @Override
    public Socket createSocket(final String host, final int port) throws IOException {
        return configure(delegate.createSocket(host, port));
    }

    @Override
    public Socket createSocket(
            final String host,
            final int port,
            final InetAddress localHost,
            final int localPort) throws IOException {
        return configure(delegate.createSocket(host, port, localHost, localPort));
    }

    @Override
    public Socket createSocket(final InetAddress host, final int port) throws IOException {
        return configure(delegate.createSocket(host, port));
    }

    @Override
    public Socket createSocket(
            final InetAddress address,
            final int port,
            final InetAddress localAddress,
            final int localPort) throws IOException {
        return configure(delegate.createSocket(address, port, localAddress, localPort));
    }

    private static Socket configure(final Socket socket) {
        if (socket instanceof SSLSocket) {
            ((SSLSocket) socket).setEnabledProtocols(ENABLED_PROTOCOLS);
        }
        return socket;
    }
}
