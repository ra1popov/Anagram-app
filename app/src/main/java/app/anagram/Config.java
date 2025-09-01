package app.anagram;

import org.webrtc.PeerConnection;

import java.util.Arrays;
import java.util.List;

public class Config {

    public enum ClientRole {
        master,
        slave
    }

    public static final String SIGNAL_URI = "ws://YOUR_DOMAIN:8888";
    public static final List<PeerConnection.IceServer> ICE_URI = Arrays.asList(
            PeerConnection.IceServer.builder("stun:YOUR_DOMAIN:3478").createIceServer(),
            PeerConnection.IceServer.builder("turn:YOUR_DOMAIN.es:3478")
                    .setUsername("username")
                    .setPassword("password")
                    .createIceServer()
    );

    public static final ClientRole ROLE = ClientRole.master;

}
