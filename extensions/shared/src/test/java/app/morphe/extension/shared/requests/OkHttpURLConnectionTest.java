package app.morphe.extension.shared.requests;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import androidx.annotation.Nullable;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.net.SocketFactory;

import okhttp3.OkHttpClient;

/**
 * Checks that the connection behaves as the connection of the platform does for the callers,
 * and that an address that never answers does not hold up a request.
 */
public class OkHttpURLConnectionTest {
    /** Address of the documentation range, so nothing is ever sent to a real host. */
    private static final String UNANSWERED_ADDRESS = "2001:db8::1";

    private ServerSocket server;
    private volatile String lastMethod;
    /** Names and values of the request headers, in the order they were sent. */
    private volatile List<String[]> lastHeaders;
    private volatile byte[] lastBody;

    @Before
    public void startServer() throws IOException {
        server = new ServerSocket(0, 50, InetAddress.getLoopbackAddress());
        Thread thread = new Thread(() -> {
            while (!server.isClosed()) {
                try (Socket socket = server.accept()) {
                    serve(socket);
                } catch (IOException ignored) {
                    // The client went away, or the server is closed.
                }
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    @After
    public void stopServer() throws IOException {
        server.close();
    }

    /**
     * Reads one request and answers it. The connection is closed after the response.
     */
    private void serve(Socket socket) throws IOException {
        InputStream in = new BufferedInputStream(socket.getInputStream());
        String requestLine = readLine(in);
        if (requestLine == null) return;
        String[] request = requestLine.split(" ");

        List<String[]> headers = new ArrayList<>();
        int contentLength = 0;
        boolean chunked = false;
        for (String line = readLine(in); line != null && !line.isEmpty(); line = readLine(in)) {
            final int colon = line.indexOf(':');
            String name = line.substring(0, colon);
            String value = line.substring(colon + 1).trim();
            headers.add(new String[]{name, value});
            if (name.equalsIgnoreCase("Content-Length")) contentLength = Integer.parseInt(value);
            if (name.equalsIgnoreCase("Transfer-Encoding")) chunked = value.equalsIgnoreCase("chunked");
        }

        ByteArrayOutputStream body = new ByteArrayOutputStream();
        if (chunked) {
            for (int size = Integer.parseInt(readLine(in), 16); size > 0;
                 size = Integer.parseInt(readLine(in), 16)) {
                body.write(in.readNBytes(size));
                readLine(in);
            }
            readLine(in);
        } else {
            body.write(in.readNBytes(contentLength));
        }

        lastMethod = request[0];
        lastHeaders = headers;
        lastBody = body.toByteArray();

        switch (request[1]) {
            case "/missing" -> respond(socket, "404 Not Found", "", "no such page");
            case "/redirect" -> respond(socket, "302 Found", "Location: /ok\r\n", "");
            case "/slow" -> {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ignored) {
                }
                respond(socket, "200 OK", "", "late");
            }
            default -> respond(socket, "200 OK",
                    "Content-Type: text/plain\r\nSet-Cookie: a=1\r\nSet-Cookie: b=2\r\n", "hello");
        }
    }

    private static void respond(Socket socket, String status, String headers, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        String head = "HTTP/1.1 " + status + "\r\n"
                + "Date: Thu, 01 Jan 2026 00:00:00 GMT\r\n"
                + headers
                + "Content-Length: " + bytes.length + "\r\n"
                + "Connection: close\r\n\r\n";
        OutputStream out = socket.getOutputStream();
        out.write(head.getBytes(StandardCharsets.US_ASCII));
        out.write(bytes);
        out.flush();
    }

    @Nullable
    private static String readLine(InputStream in) throws IOException {
        ByteArrayOutputStream line = new ByteArrayOutputStream();
        for (int read = in.read(); read != '\n'; read = in.read()) {
            if (read == -1) return line.size() == 0 ? null : line.toString("US-ASCII");
            if (read != '\r') line.write(read);
        }
        return line.toString("US-ASCII");
    }

    @Test
    public void getReturnsTheResponse() throws Exception {
        HttpURLConnection connection = open("/ok");
        connection.setRequestProperty("X-Test", "value");

        assertEquals(200, connection.getResponseCode());
        assertEquals("OK", connection.getResponseMessage());
        assertEquals("hello", readString(connection.getInputStream()));
        assertEquals("text/plain", connection.getContentType());
        assertEquals(5, connection.getContentLength());
        assertNull(connection.getErrorStream());

        assertEquals("GET", lastMethod);
        assertEquals("value", header("X-Test"));
        assertNull(header("Content-Type"));
        assertEquals(0, lastBody.length);
    }

    @Test
    public void headerFieldsAreFoundWhateverTheirCase() throws Exception {
        HttpURLConnection connection = open("/ok");

        Map<String, List<String>> fields = connection.getHeaderFields();
        assertEquals(Arrays.asList("a=1", "b=2"), fields.get("Set-Cookie"));
        assertEquals(Arrays.asList("a=1", "b=2"), fields.get("set-cookie"));
        assertEquals(List.of("HTTP/1.1 200 OK"), fields.get(null));
        assertEquals("HTTP/1.1 200 OK", connection.getHeaderField(null));
        // The last value of a repeated header, as the connection of the platform gives it.
        assertEquals("b=2", connection.getHeaderField("SET-COOKIE"));
        assertTrue(connection.getHeaderFieldDate("Date", -1) > 0);
    }

    @Test
    public void defaultUserAgentIsTheOneOfThePlatform() throws Exception {
        String previous = System.getProperty("http.agent");
        System.setProperty("http.agent", "TestAgent/1.0");
        try {
            assertEquals(200, open("/ok").getResponseCode());
            assertEquals("TestAgent/1.0", header("User-Agent"));

            HttpURLConnection connection = open("/ok");
            connection.setRequestProperty("User-Agent", "Mine/2.0");
            assertEquals(200, connection.getResponseCode());
            assertEquals("Mine/2.0", header("User-Agent"));
        } finally {
            if (previous == null) {
                System.clearProperty("http.agent");
            } else {
                System.setProperty("http.agent", previous);
            }
        }
    }

    @Test
    public void postWithoutBodySendsAnEmptyBody() throws Exception {
        HttpURLConnection connection = open("/ok");
        connection.setFixedLengthStreamingMode(0);
        connection.setRequestMethod("POST");

        assertEquals(200, connection.getResponseCode());
        assertEquals("POST", lastMethod);
        assertEquals("0", header("Content-Length"));
        assertEquals("application/x-www-form-urlencoded", header("Content-Type"));
        assertEquals(0, lastBody.length);
    }

    @Test
    public void askingForOutputMakesGetAPost() throws Exception {
        byte[] body = "{\"a\":1}".getBytes(StandardCharsets.UTF_8);
        HttpURLConnection connection = open("/ok");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);
        connection.setFixedLengthStreamingMode(body.length);
        try (OutputStream stream = connection.getOutputStream()) {
            stream.write(body);
        }

        assertEquals(200, connection.getResponseCode());
        assertEquals("POST", lastMethod);
        assertEquals("application/json", header("Content-Type"));
        assertEquals(String.valueOf(body.length), header("Content-Length"));
        assertArrayEquals(body, lastBody);
    }

    @Test
    public void headersAreSentInTheOrderOfThePlatform() throws Exception {
        byte[] body = "{}".getBytes(StandardCharsets.UTF_8);
        HttpURLConnection connection = open("/ok");
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("User-Agent", "Mine/2.0");
        connection.setRequestProperty("X-Test", "value");
        connection.setDoOutput(true);
        connection.setFixedLengthStreamingMode(body.length);
        try (OutputStream stream = connection.getOutputStream()) {
            stream.write(body);
        }

        assertEquals(200, connection.getResponseCode());
        assertEquals(Arrays.asList("Content-Type", "User-Agent", "X-Test",
                "Content-Length", "Host", "Connection", "Accept-Encoding"), headerNames());
    }

    @Test
    public void lengthOfAKeptBodyIsTheLastHeader() throws Exception {
        HttpURLConnection connection = open("/ok");
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("User-Agent", "Mine/2.0");
        connection.setDoOutput(true);
        try (OutputStream stream = connection.getOutputStream()) {
            stream.write("{}".getBytes(StandardCharsets.UTF_8));
        }

        assertEquals(200, connection.getResponseCode());
        assertEquals(Arrays.asList("Content-Type", "User-Agent",
                "Host", "Connection", "Accept-Encoding", "Content-Length"), headerNames());
    }

    @Test
    public void lengthSetByTheCallerComesBeforeTheDefaultHeaders() throws Exception {
        HttpURLConnection connection = open("/ok");
        connection.setRequestProperty("X-Test", "value");
        connection.setFixedLengthStreamingMode(0);
        connection.setRequestMethod("POST");

        assertEquals(200, connection.getResponseCode());
        assertEquals("X-Test", headerNames().get(0));
        assertEquals("Content-Length", headerNames().get(1));
        assertEquals("Content-Type", headerNames().get(2));
    }

    @Test
    public void bodyCanBeWrittenAfterConnect() throws Exception {
        byte[] body = "payload".getBytes(StandardCharsets.UTF_8);
        HttpURLConnection connection = open("/ok");
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.connect();
        connection.setReadTimeout(5000);
        try (OutputStream stream = connection.getOutputStream()) {
            stream.write(body);
        }

        assertEquals(200, connection.getResponseCode());
        assertArrayEquals(body, lastBody);
    }

    @Test
    public void chunkedModeSendsNoContentLength() throws Exception {
        byte[] body = "payload".getBytes(StandardCharsets.UTF_8);
        HttpURLConnection connection = open("/ok");
        connection.setRequestMethod("PUT");
        connection.setDoOutput(true);
        connection.setChunkedStreamingMode(1024);
        try (OutputStream stream = connection.getOutputStream()) {
            stream.write(body);
        }

        assertEquals(200, connection.getResponseCode());
        assertEquals("PUT", lastMethod);
        assertEquals("chunked", header("Transfer-Encoding"));
        assertNull(header("Content-Length"));
        assertArrayEquals(body, lastBody);
    }

    @Test
    public void getDoesNotAcceptABody() throws Exception {
        HttpURLConnection connection = open("/ok");
        assertThrows(IOException.class, connection::getOutputStream);
    }

    @Test
    public void errorResponseIsReadFromTheErrorStream() throws Exception {
        HttpURLConnection connection = open("/missing");

        assertEquals(404, connection.getResponseCode());
        assertThrows(FileNotFoundException.class, connection::getInputStream);
        InputStream errorStream = connection.getErrorStream();
        assertNotNull(errorStream);
        assertEquals("no such page", readString(errorStream));
    }

    @Test
    public void redirectIsFollowedUnlessTurnedOff() throws Exception {
        HttpURLConnection followed = open("/redirect");
        assertEquals(200, followed.getResponseCode());
        assertEquals("/ok", followed.getURL().getPath());

        HttpURLConnection notFollowed = open("/redirect");
        notFollowed.setInstanceFollowRedirects(false);
        assertEquals(302, notFollowed.getResponseCode());
        assertEquals("/ok", notFollowed.getHeaderField("Location"));
    }

    @Test
    public void readTimeoutIsKept() throws Exception {
        HttpURLConnection connection = open("/slow");
        connection.setReadTimeout(300);
        assertThrows(SocketTimeoutException.class, connection::getResponseCode);
        // The failure is kept, and the header getters do not throw.
        assertThrows(SocketTimeoutException.class, connection::getResponseCode);
        assertNull(connection.getHeaderField("Content-Type"));
        assertNull(connection.getErrorStream());
    }

    @Test
    public void requestCannotChangeAfterItIsSent() throws Exception {
        HttpURLConnection connection = open("/ok");
        assertEquals(200, connection.getResponseCode());
        assertThrows(IllegalStateException.class, () -> connection.setRequestProperty("A", "b"));
        connection.disconnect();
    }

    @Test
    public void cookieHandlerOfTheAppIsUsed() throws Exception {
        CookieHandler previous = CookieHandler.getDefault();
        CookieHandler.setDefault(new CookieManager());
        try {
            assertEquals(200, open("/ok").getResponseCode());
            assertNull(header("Cookie"));

            assertEquals(200, open("/ok").getResponseCode());
            assertEquals("a=1; b=2", header("Cookie"));
        } finally {
            CookieHandler.setDefault(previous);
        }
    }

    @Test
    public void disconnectWithoutReadingTheBodyIsAllowed() throws Exception {
        HttpURLConnection unread = open("/missing");
        assertEquals(404, unread.getResponseCode());
        unread.disconnect();
        unread.disconnect();

        HttpURLConnection notSent = open("/ok");
        notSent.disconnect();
        assertEquals(200, notSent.getResponseCode());
    }

    /**
     * The first address of the host never answers and the second one is the server.
     * With one address tried after the other, the request would take the whole connect timeout.
     */
    @Test
    public void addressThatNeverAnswersDoesNotHoldUpTheRequest() throws Exception {
        final int connectTimeoutMillis = 5000;
        InetAddress unanswered = InetAddress.getByName(UNANSWERED_ADDRESS);
        OkHttpClient client = new OkHttpClient.Builder()
                .dns(hostname -> Arrays.asList(unanswered, InetAddress.getLoopbackAddress()))
                .socketFactory(new StallingSocketFactory(unanswered))
                .build();

        URL url = new URL("http://two-addresses.test:" + server.getLocalPort() + "/ok");
        HttpURLConnection connection = new OkHttpURLConnection(url, client);
        connection.setConnectTimeout(connectTimeoutMillis);

        final long start = System.nanoTime();
        assertEquals(200, connection.getResponseCode());
        final long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);

        assertEquals("hello", readString(connection.getInputStream()));
        assertTrue("Took " + elapsedMillis + " ms", elapsedMillis < connectTimeoutMillis / 2);
    }

    private HttpURLConnection open(String path) throws IOException {
        return new OkHttpURLConnection(
                new URL("http://127.0.0.1:" + server.getLocalPort() + path),
                new OkHttpClient());
    }

    private String header(String name) {
        for (String[] header : lastHeaders) {
            if (header[0].equalsIgnoreCase(name)) return header[1];
        }
        return null;
    }

    private List<String> headerNames() {
        List<String> names = new ArrayList<>();
        for (String[] header : lastHeaders) {
            names.add(header[0]);
        }
        return names;
    }

    private static String readString(InputStream stream) throws IOException {
        return new String(readAll(stream), StandardCharsets.UTF_8);
    }

    private static byte[] readAll(InputStream stream) throws IOException {
        try (stream) {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int read;
            while ((read = stream.read(buffer)) != -1) {
                bytes.write(buffer, 0, read);
            }
            return bytes.toByteArray();
        }
    }

    /**
     * Sockets that wait for the whole timeout when connecting to one address, as a network does
     * that drops the traffic to it without an error.
     */
    private static final class StallingSocketFactory extends SocketFactory {
        private final InetAddress stalled;

        StallingSocketFactory(InetAddress stalled) {
            this.stalled = stalled;
        }

        @Override
        public Socket createSocket() {
            return new Socket() {
                private volatile boolean closed;

                @Override
                public void connect(SocketAddress endpoint, int timeout) throws IOException {
                    if (!stalled.equals(((InetSocketAddress) endpoint).getAddress())) {
                        super.connect(endpoint, timeout);
                        return;
                    }
                    final long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeout);
                    while (!closed && System.nanoTime() < deadline) {
                        try {
                            Thread.sleep(20);
                        } catch (InterruptedException ex) {
                            Thread.currentThread().interrupt();
                            throw new IOException(ex);
                        }
                    }
                    throw new SocketTimeoutException("connect timed out");
                }

                @Override
                public synchronized void close() throws IOException {
                    closed = true;
                    super.close();
                }
            };
        }

        @Override
        public Socket createSocket(String host, int port) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Socket createSocket(String host, int port, InetAddress localHost, int localPort) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Socket createSocket(InetAddress host, int port) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) {
            throw new UnsupportedOperationException();
        }
    }
}
