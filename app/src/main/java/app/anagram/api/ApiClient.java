package app.anagram.api;

import android.content.Context;

import androidx.annotation.NonNull;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.reactivex.rxjava3.subjects.PublishSubject;

public class ApiClient {

    private final Context context;
    private final String url;
    private WebSocketClient client;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final PublishSubject<ApiClientConnectionEvent> connectionEvents = PublishSubject.create();

    public ApiClient(@NonNull Context context, @NonNull String url) {
        this.context = context;
        this.url = url;
    }

    public void connect() {
        try {

            client = new WebSocketClient(new URI(url)) {
                @Override
                public void onOpen(ServerHandshake handshake) {
                    connectionEvents.onNext(new ApiClientConnectionEvent.Open());
                }

                @Override
                public void onMessage(String message) {
                    connectionEvents.onNext(new ApiClientConnectionEvent.Message(message));
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    connectionEvents.onNext(new ApiClientConnectionEvent.Close());
                }

                @Override
                public void onError(Exception e) {
                    connectionEvents.onNext(new ApiClientConnectionEvent.Error(e));
                }
            };

            client.connect();

        } catch (Exception e) {
            connectionEvents.onNext(new ApiClientConnectionEvent.Error(e));
        }
    }

    public void sendMessage(String json) {
        executor.execute(() -> {
            if (client != null && client.isOpen()) {
                client.send(json);
            }
        });
    }

    public void close() {
        if (client != null) {
            client.close();
            client = null;
        }
    }

    public PublishSubject<ApiClientConnectionEvent> getConnectionEvents() {
        return connectionEvents;
    }

}

