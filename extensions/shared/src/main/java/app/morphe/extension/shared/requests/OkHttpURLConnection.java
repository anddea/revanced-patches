package app.morphe.extension.shared.requests;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.CookieHandler;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.Proxy;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.ConnectionSpec;
import okhttp3.Headers;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.BufferedSink;

/**
 * An {@link HttpURLConnection} that sends its request with OkHttp.
 * <p>
 * The connection of the platform tries the addresses of a host one after the other, each for the whole
 * connect timeout, and the IPv6 addresses come first. On a network that drops IPv6 traffic without an error,
 * a request then times out before any IPv4 address is tried. OkHttp tries IPv6 and IPv4 addresses
 * at the same time and uses the first that connects (RFC 8305).
 * <p>
 * The client is set up as the platform sets up its own: HTTP/1.1 only, the cipher suites and TLS versions
 * of the socket, no time limits unless the caller sets them, and no redirects between http and https.
 * The request has the same headers, and the methods behave the same, so callers do not change.
 * <p>
 * Not supported, since nothing uses them: a response cache, and streaming of a request body.
 * The request body is kept in memory until the request is sent, and the request is sent
 * when the response is first asked for.
 */
public final class OkHttpURLConnection extends HttpURLConnection {

    /**
     * Sends the cookies of the cookie handler of the app, if it has one, and gives it the cookies received,
     * as the connection of the platform does. The headers are passed as they are and not parsed.
     */
    private static final Interceptor SYSTEM_COOKIES = chain -> {
        Request request = chain.request();
        CookieHandler handler = CookieHandler.getDefault();
        if (handler == null) return chain.proceed(request);

        URI uri = request.url().uri();
        Request.Builder builder = request.newBuilder();
        for (Map.Entry<String, List<String>> header
                : handler.get(uri, request.headers().toMultimap()).entrySet()) {
            String name = header.getKey();
            if (!"Cookie".equalsIgnoreCase(name) && !"Cookie2".equalsIgnoreCase(name)) continue;
            if (header.getValue().isEmpty()) continue;
            builder.addHeader(name, String.join("; ", header.getValue()));
        }
        Response received = chain.proceed(builder.build());
        handler.put(uri, received.headers().toMultimap());
        return received;
    };

    /**
     * Place of the Content-Length header among the headers of a request,
     * where the connection of the platform puts it.
     */
    private static final class ContentLengthPlace {
        /**
         * Number of headers before it, or -1 if it is the last header.
         */
        final int index;

        ContentLengthPlace(int index) {
            this.index = index;
        }
    }

    /**
     * Moves the Content-Length header to its {@link ContentLengthPlace},
     * so the headers are in the order the connection of the platform sends them.
     */
    private static final Interceptor CONTENT_LENGTH_PLACE = chain -> {
        Request request = chain.request();
        ContentLengthPlace place = request.tag(ContentLengthPlace.class);
        String contentLength = request.header("Content-Length");
        if (place == null || contentLength == null) return chain.proceed(request);

        Headers headers = request.headers();
        Headers.Builder ordered = new Headers.Builder();
        int index = 0;
        boolean added = false;
        for (int i = 0, size = headers.size(); i < size; i++) {
            String name = headers.name(i);
            if ("Content-Length".equalsIgnoreCase(name)) continue;
            if (index == place.index) {
                ordered.add("Content-Length", contentLength);
                added = true;
            }
            ordered.add(name, headers.value(i));
            index++;
        }
        if (!added) ordered.add("Content-Length", contentLength);
        return chain.proceed(request.newBuilder().headers(ordered.build()).build());
    };

    private static final class ClientHolder {
        static final OkHttpClient CLIENT = new OkHttpClient.Builder()
                .connectTimeout(0, TimeUnit.MILLISECONDS)
                .readTimeout(0, TimeUnit.MILLISECONDS)
                .writeTimeout(0, TimeUnit.MILLISECONDS)
                .followSslRedirects(false)
                .protocols(Collections.singletonList(Protocol.HTTP_1_1))
                .connectionSpecs(Arrays.asList(
                        new ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
                                .allEnabledCipherSuites()
                                .allEnabledTlsVersions()
                                .build(),
                        ConnectionSpec.CLEARTEXT))
                .build();
    }

    private static final List<String> METHODS = Arrays.asList(
            "OPTIONS", "GET", "HEAD", "POST", "PUT", "DELETE", "TRACE", "PATCH");

    private static final List<String> METHODS_WITH_BODY = Arrays.asList(
            "POST", "PUT", "PATCH", "DELETE", "PROPPATCH", "REPORT");

    private static final List<String> METHODS_REQUIRING_BODY = Arrays.asList(
            "POST", "PUT", "PATCH", "PROPPATCH", "REPORT");

    private final OkHttpClient baseClient;
    private final Headers.Builder requestHeaders = new Headers.Builder();

    @Nullable
    private ByteArrayOutputStream requestBody;
    @Nullable
    private volatile Call call;
    @Nullable
    private Response response;
    @Nullable
    private IOException failure;
    /**
     * If the caller has the stream of the response body, and closes it.
     */
    private boolean responseBodyGiven;

    public OkHttpURLConnection(URL url) {
        this(url, ClientHolder.CLIENT);
    }

    /**
     * @param client Client to send the request with. Tests use a client of their own.
     */
    OkHttpURLConnection(URL url, OkHttpClient client) {
        super(url);
        baseClient = client;
    }

    @Override
    public void connect() {
        // The request is sent when the response is first asked for,
        // so a request body can still be written after this call.
        connected = true;
    }

    @Override
    public void disconnect() {
        Call sent = call;
        if (sent == null) return;
        sent.cancel();
        // A response body that the caller never asked for is closed here, since nothing else closes it.
        Response received = response;
        if (received != null && !responseBodyGiven) received.close();
    }

    @Override
    public boolean usingProxy() {
        Proxy proxy = baseClient.proxy();
        return proxy != null && proxy.type() != Proxy.Type.DIRECT;
    }

    @Override
    public void setRequestMethod(String method) throws ProtocolException {
        if (!METHODS.contains(method)) {
            throw new ProtocolException("Expected one of " + METHODS + " but was " + method);
        }
        this.method = method;
    }

    @Override
    public void setIfModifiedSince(long newValue) {
        super.setIfModifiedSince(newValue);
        if (newValue != 0) {
            requestHeaders.set("If-Modified-Since", new Date(newValue));
        } else {
            requestHeaders.removeAll("If-Modified-Since");
        }
    }

    @Override
    public void setRequestProperty(String field, String newValue) {
        checkNotSent();
        if (field == null) throw new NullPointerException("field == null");
        // A header without a value is not sent, as with the connection of the platform.
        if (newValue == null) return;
        requestHeaders.set(field, newValue);
    }

    @Override
    public void addRequestProperty(String field, String value) {
        checkNotSent();
        if (field == null) throw new NullPointerException("field == null");
        if (value == null) return;
        requestHeaders.add(field, value);
    }

    @Override
    public String getRequestProperty(String field) {
        return field == null ? null : requestHeaders.get(field);
    }

    @Override
    public Map<String, List<String>> getRequestProperties() {
        checkNotSent();
        return toMultimap(requestHeaders.build(), null);
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        if (response != null || failure != null) {
            throw new ProtocolException("cannot write request body after response has been read");
        }
        final String requestMethod = effectiveMethod();
        if (!METHODS_WITH_BODY.contains(requestMethod)) {
            throw new ProtocolException("method does not support a request body: " + requestMethod);
        }
        connected = true;
        if (requestBody == null) requestBody = new ByteArrayOutputStream();
        return requestBody;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        if (!doInput) {
            throw new ProtocolException("This protocol does not support input");
        }
        Response received = getResponse();
        if (received.code() >= HTTP_BAD_REQUEST) {
            throw new FileNotFoundException(url.toString());
        }
        responseBodyGiven = true;
        return received.body().byteStream();
    }

    @Override
    public InputStream getErrorStream() {
        try {
            Response received = getResponse();
            if (received.code() >= HTTP_BAD_REQUEST && hasBody(received)) {
                responseBodyGiven = true;
                return received.body().byteStream();
            }
        } catch (IOException ignored) {
            // No error response was received.
        }
        return null;
    }

    @Override
    public int getResponseCode() throws IOException {
        return getResponse().code();
    }

    @Override
    public String getResponseMessage() throws IOException {
        return getResponse().message();
    }

    @Override
    public String getHeaderField(String fieldName) {
        try {
            Response received = getResponse();
            return fieldName == null ? statusLine(received) : received.headers().get(fieldName);
        } catch (IOException ex) {
            return null;
        }
    }

    @Override
    public String getHeaderField(int position) {
        try {
            Headers headers = getResponse().headers();
            return position < 0 || position >= headers.size() ? null : headers.value(position);
        } catch (IOException ex) {
            return null;
        }
    }

    @Override
    public String getHeaderFieldKey(int position) {
        try {
            Headers headers = getResponse().headers();
            return position < 0 || position >= headers.size() ? null : headers.name(position);
        } catch (IOException ex) {
            return null;
        }
    }

    @Override
    public Map<String, List<String>> getHeaderFields() {
        try {
            Response received = getResponse();
            return toMultimap(received.headers(), statusLine(received));
        } catch (IOException ex) {
            return Collections.emptyMap();
        }
    }

    /**
     * Sends the request, if it is not sent yet, and waits for the response headers.
     */
    private Response getResponse() throws IOException {
        if (response != null) return response;
        if (failure != null) throw failure;

        connected = true;
        try {
            Call sending = baseClient.newBuilder()
                    .connectTimeout(getConnectTimeout(), TimeUnit.MILLISECONDS)
                    .readTimeout(getReadTimeout(), TimeUnit.MILLISECONDS)
                    .followRedirects(getInstanceFollowRedirects())
                    .addNetworkInterceptor(SYSTEM_COOKIES)
                    .addNetworkInterceptor(CONTENT_LENGTH_PLACE)
                    .build()
                    .newCall(buildRequest());
            call = sending;
            Response received = sending.execute();
            // After a redirect, the address is the one that gave the response.
            url = received.request().url().url();
            response = received;
            return received;
        } catch (IOException ex) {
            failure = ex;
            throw ex;
        }
    }

    private Request buildRequest() throws IOException {
        final String requestMethod = effectiveMethod();
        method = requestMethod;

        // With a length set by the caller, the platform sends the length after the headers of the caller.
        // Otherwise it sends the length of the body it kept, as the last header.
        requestHeaders.removeAll("Content-Length");
        ContentLengthPlace contentLengthPlace = new ContentLengthPlace(
                fixedContentLengthLong != -1 || fixedContentLength != -1
                        ? requestHeaders.build().size()
                        : -1);

        RequestBody body = null;
        if (METHODS_WITH_BODY.contains(requestMethod)) {
            if (requestBody != null) {
                body = bodyOf(requestBody.toByteArray(), chunkLength > 0);
            } else if (METHODS_REQUIRING_BODY.contains(requestMethod)) {
                body = bodyOf(new byte[0], false);
            }
            // The platform names a content type for these methods, if the caller did not.
            if (requestHeaders.get("Content-Type") == null) {
                requestHeaders.set("Content-Type", "application/x-www-form-urlencoded");
            }
        }
        if (requestHeaders.get("User-Agent") == null) {
            String agent = System.getProperty("http.agent");
            if (agent != null) requestHeaders.set("User-Agent", agent);
        }

        try {
            return new Request.Builder()
                    .url(url)
                    .headers(requestHeaders.build())
                    .method(requestMethod, body)
                    .tag(ContentLengthPlace.class, contentLengthPlace)
                    .build();
        } catch (IllegalArgumentException ex) {
            throw new IOException(ex);
        }
    }

    /**
     * @return The method of the request. Asking for a request body makes a GET request a POST request.
     */
    private String effectiveMethod() throws ProtocolException {
        if (!doOutput) return method;
        if ("GET".equals(method)) return "POST";
        if (!METHODS_WITH_BODY.contains(method)) {
            throw new ProtocolException(method + " does not support writing");
        }
        return method;
    }

    private void checkNotSent() {
        if (response != null || failure != null) {
            throw new IllegalStateException("Cannot change the request after it is sent");
        }
    }

    private static RequestBody bodyOf(byte[] content, boolean chunked) {
        return new RequestBody() {
            @Nullable
            @Override
            public MediaType contentType() {
                // The content type is a header of the request.
                return null;
            }

            @Override
            public long contentLength() {
                return chunked ? -1 : content.length;
            }

            @Override
            public void writeTo(@NonNull BufferedSink sink) throws IOException {
                sink.write(content);
            }
        };
    }

    private static boolean hasBody(Response received) {
        ResponseBody body = received.body();
        final int code = received.code();
        if ("HEAD".equals(received.request().method())) return false;
        if (code >= 200 && code != HTTP_NO_CONTENT && code != HTTP_NOT_MODIFIED) return true;
        return body.contentLength() > 0
                || "chunked".equalsIgnoreCase(received.header("Transfer-Encoding"));
    }

    private static String statusLine(Response received) {
        final String protocol = received.protocol() == Protocol.HTTP_1_0 ? "HTTP/1.0" : "HTTP/1.1";
        return protocol + ' ' + received.code() + ' ' + received.message();
    }

    /**
     * @param statusLine Value of the null key, or null to leave the key out.
     */
    private static Map<String, List<String>> toMultimap(Headers headers, @Nullable String statusLine) {
        // Header names are found whatever their case, as with the connection of the platform.
        Map<String, List<String>> byName = new TreeMap<>((first, second) -> {
            if (first == null || second == null) {
                return first == null ? (second == null ? 0 : -1) : 1;
            }
            return String.CASE_INSENSITIVE_ORDER.compare(first, second);
        });
        for (int i = 0, size = headers.size(); i < size; i++) {
            List<String> values = byName.get(headers.name(i));
            if (values == null) {
                values = new ArrayList<>(2);
                byName.put(headers.name(i), values);
            }
            values.add(headers.value(i));
        }
        if (statusLine != null) {
            byName.put(null, Collections.singletonList(statusLine));
        }
        return Collections.unmodifiableMap(byName);
    }
}
