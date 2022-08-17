package com.rabbitmq.samples;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

public class Producer {

    public static String getURI() {
        String uri = System.getenv("URI");
        if (uri == null) uri = "amqp://guest:guest@localhost:5672/%2F";
        return uri;
    }
    public static ConnectionFactory setTLS(ConnectionFactory factory) throws NoSuchAlgorithmException, KeyManagementException, URISyntaxException, CertificateException, KeyStoreException, IOException {
        factory = setTrustStore(factory);

        String uri = System.getenv("URI");
        if (uri == null) uri = "amqp://guest:guest@localhost:5672/%2F";
        factory.setUri(uri);

        if (System.getenv("ENABLE_HOSTNAME_VERIFICATION") != null) {
            if (Boolean.parseBoolean(System.getenv("ENABLE_HOSTNAME_VERIFICATION")))
                factory.enableHostnameVerification();
        }
        return factory;
    }
    private static ConnectionFactory setTrustStore(ConnectionFactory factory) throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException, KeyManagementException {
        String trustStorePassword = System.getenv("TRUST_STORE_PASSWORD");
        if (trustStorePassword == null) trustStorePassword = "password";

        char[] trustPassphrase = trustStorePassword.toCharArray();
        KeyStore tks = KeyStore.getInstance("JKS");
        String trustStorePath = System.getenv("TRUST_STORE_PATH");
        tks.load(new FileInputStream(trustStorePath), trustPassphrase);

        TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
        tmf.init(tks);
        SSLContext c = SSLContext.getInstance("TLSv1.2");
        c.init(null, tmf.getTrustManagers(), null);

        factory.useSslProtocol(c);

        return factory;
    }
    public static void main(String args[]) throws Exception {
        String queue = System.getenv("QUEUE_NAME");
        if (queue == null) queue = "test";

        ConnectionFactory factory = setTLS(new ConnectionFactory());
        factory.setAutomaticRecoveryEnabled(true);

        try (Connection connection = factory.newConnection();
            Channel channel = connection.createChannel()) {
            channel.queueDeclare(queue, false, false, false, null);
            String message = "Hello World!";
            for (;;) {
                try {
                    channel.basicPublish("", queue, null, message.getBytes(StandardCharsets.UTF_8));
                    System.out.println(" [x] Sent '" + message + "'");
                }catch(com.rabbitmq.client.AlreadyClosedException e) {
                    System.err.println("Connection closed");
                }finally {
                    Thread.sleep(2000);
                }
            }
        }

    }
}
