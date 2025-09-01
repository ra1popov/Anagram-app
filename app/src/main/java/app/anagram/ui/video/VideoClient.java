package app.anagram.ui.video;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.PeerConnection;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class VideoClient {

    public interface SendSignalCallback {
        void send(String json);
    }

    private static class SdpObserverAdapter implements SdpObserver {
        @Override
        public void onCreateSuccess(SessionDescription sdp) {
        }

        @Override
        public void onSetSuccess() {
        }

        @Override
        public void onCreateFailure(String s) {
        }

        @Override
        public void onSetFailure(String s) {
        }
    }

    private final PeerConnection connection;
    private final boolean polite;
    private final SendSignalCallback sender;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public VideoClient(PeerConnection connection, boolean polite, SendSignalCallback sender) {
        this.connection = connection;
        this.polite = polite;
        this.sender = sender;
    }

    public void onNegotiationNeeded() {
        if (polite) {
            return;
        }

        executor.execute(() -> {
            try {
                connection.createOffer(new SdpObserverAdapter() {
                    @Override
                    public void onCreateSuccess(SessionDescription sdp) {
                        connection.setLocalDescription(new SdpObserverAdapter() {
                            @Override
                            public void onSetSuccess() {
                                try {
                                    JSONObject msg = new JSONObject();
                                    msg.put("type", "offer");
                                    msg.put("sdp", sdp.description);
                                    sender.send(msg.toString());
                                } catch (JSONException ignored) {
                                }
                            }
                        }, sdp);
                    }
                }, new MediaConstraints());
            } catch (Exception ignored) {
            }
        });
    }

    public void onSignalMessage(String json) {
        executor.execute(() -> {
            try {
                JSONObject message = new JSONObject(json);
                String type = message.optString("type", "");

                switch (type) {
                    case "offer":
                        handleRemoteOffer(message.optString("sdp", ""));
                        break;
                    case "answer":
                        handleRemoteAnswer(message.optString("sdp", ""));
                        break;
                    case "ice":
                        handleRemoteIce(message);
                        break;
                }
            } catch (Exception ignored) {
            }
        });
    }

    private void handleRemoteOffer(String sdp) {
        try {
            SessionDescription remoteOffer = new SessionDescription(SessionDescription.Type.OFFER, sdp);

            connection.setRemoteDescription(new SdpObserverAdapter() {
                @Override
                public void onSetSuccess() {
                    connection.createAnswer(new SdpObserverAdapter() {
                        @Override
                        public void onCreateSuccess(SessionDescription sdp) {
                            connection.setLocalDescription(new SdpObserverAdapter() {
                                @Override
                                public void onSetSuccess() {
                                    try {
                                        JSONObject msg = new JSONObject();
                                        msg.put("type", "answer");
                                        msg.put("sdp", sdp.description);
                                        sender.send(msg.toString());
                                    } catch (JSONException ignored) {
                                    }
                                }
                            }, sdp);
                        }
                    }, new MediaConstraints());
                }
            }, remoteOffer);
        } catch (Exception ignored) {
        }
    }

    private void handleRemoteAnswer(String sdp) {
        try {
            SessionDescription answer = new SessionDescription(SessionDescription.Type.ANSWER, sdp);
            connection.setRemoteDescription(new SdpObserverAdapter(), answer);
        } catch (Exception ignored) {
        }
    }

    private void handleRemoteIce(JSONObject message) {
        try {
            String candidate = message.optString("candidate", "");
            String sdpMid = message.optString("sdpMid", "");
            int sdpMLineIndex = message.optInt("sdpMLineIndex", -1);
            IceCandidate ice = new IceCandidate(sdpMid, sdpMLineIndex, candidate);
            connection.addIceCandidate(ice);
        } catch (Exception ignored) {
        }
    }

    public void sendIce(IceCandidate candidate) {
        try {
            JSONObject msg = new JSONObject();
            msg.put("type", "ice");
            msg.put("candidate", candidate.sdp);
            msg.put("sdpMid", candidate.sdpMid);
            msg.put("sdpMLineIndex", candidate.sdpMLineIndex);
            sender.send(msg.toString());
        } catch (JSONException ignored) {
        }
    }

}
