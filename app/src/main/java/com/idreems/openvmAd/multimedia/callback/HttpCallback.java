package com.idreems.openvmAd.multimedia.callback;

import java.io.IOException;

import okhttp3.Response;

/**
 * Created by ramonqlee on 6/28/16.
 */
public interface HttpCallback {
    /**
     * Called when the request could not be executed due to cancellation, a connectivity problem or
     * timeout. Because networks can fail during an exchange, it is possible that the remote server
     * accepted the request before the failure.
     */
    void onFailure(final IOException e);

    /**
     * Called when the HTTP response was successfully returned by the remote server. The callback may
     * proceed to read the response body with {@link Response#body}. The response is still live until
     * its response body is closed with {@code response.body().close()}. The recipient of the callback
     * may even consume the response body on another thread.
     * <p/>
     * <p>Note that transport-layer success (receiving a HTTP response code, headers and body) does
     * not necessarily indicate application-layer success: {@code response} may still indicate an
     * unhappy HTTP response code like 404 or 500.
     */
    void onResponse(final String response, boolean isCache);
}
