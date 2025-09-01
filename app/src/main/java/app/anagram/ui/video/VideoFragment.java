package app.anagram.ui.video;

import android.app.Activity;
import android.content.Context;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.Camera2Enumerator;
import org.webrtc.DataChannel;
import org.webrtc.DefaultVideoDecoderFactory;
import org.webrtc.DefaultVideoEncoderFactory;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.MediaStreamTrack;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.RtpTransceiver;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;

import java.util.Collections;

import app.anagram.Config;
import app.anagram.R;
import app.anagram.api.ApiClientConnectionEvent;
import app.anagram.databinding.FragmentVideoBinding;
import app.anagram.dependencies.ApplicationDependencies;
import app.anagram.ui.BaseFragment;
import app.anagram.util.Util;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.Disposable;

public class VideoFragment extends BaseFragment<FragmentVideoBinding> implements VideoHandler {

    private VideoViewModel videoViewModel;
    private PeerConnection peerConnection;
    private PeerConnectionFactory factory;
    private EglBase eglBase;
    private VideoCapturer videoCapturer;
    private VideoClient videoClient;
    private VideoSource videoSource;
    private VideoTrack videoTrack;
    private AudioSource audioSource;
    private AudioTrack audioTrack;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        videoViewModel = new ViewModelProvider(requireActivity()).get(VideoViewModel.class);

        PeerConnectionFactory.InitializationOptions initOpts = PeerConnectionFactory.InitializationOptions.builder(requireContext()).createInitializationOptions();
        PeerConnectionFactory.initialize(initOpts);

        eglBase = EglBase.create();

        PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();
        factory = PeerConnectionFactory.builder()
                .setOptions(options)
                .setVideoEncoderFactory(new DefaultVideoEncoderFactory(eglBase.getEglBaseContext(), true, true))
                .setVideoDecoderFactory(new DefaultVideoDecoderFactory(eglBase.getEglBaseContext()))
                .createPeerConnectionFactory();
    }

    @Override
    protected void initView() {
        binding.localView.init(eglBase.getEglBaseContext(), null);
        binding.remoteView.init(eglBase.getEglBaseContext(), null);

        binding.localView.setMirror(true);
        binding.remoteView.setMirror(false);

        binding.localView.setEnableHardwareScaler(true);
        binding.remoteView.setEnableHardwareScaler(true);

        binding.localView.setZOrderMediaOverlay(true);
    }

    @Override
    protected void initControl() {
        requireActivity().getOnBackPressedDispatcher().addCallback(
                getViewLifecycleOwner(),
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        Util.runOnMain(() -> { // завернуть в метод вo fragment и baseactivity
                            Context context = getContext();
                            if (context != null) {
                                ((Activity) context).finishAffinity();
                            }
                        });
                    }
                }
        );

        Disposable openDisposable = ApplicationDependencies.getApiClient()
                .getConnectionEvents()
                .observeOn(AndroidSchedulers.mainThread())
                .filter(event -> event instanceof ApiClientConnectionEvent.Open)
                .subscribe(event -> startVideo());

        compositeDisposable.add(openDisposable);

        ApplicationDependencies.getApiClient().connect();
    }

    @Override
    public void onResume() {
        super.onResume();

        Disposable resumeDisposable = ApplicationDependencies.getApiClient()
                .getConnectionEvents()
                .observeOn(AndroidSchedulers.mainThread())
                .filter(event -> !(event instanceof ApiClientConnectionEvent.Open))
                .subscribe(event -> {
                    if (event instanceof ApiClientConnectionEvent.Message) {
                        videoClient.onSignalMessage(((ApiClientConnectionEvent.Message) event).message);
                    } else if (event instanceof ApiClientConnectionEvent.Close) {
                        Context context = getContext();
                        if (context != null) {
                            ((Activity) context).finishAffinity();
                        }
                    } else if (event instanceof ApiClientConnectionEvent.Error) {
                        toast(R.string.signal_server_connection_error);
                    }
                });

        compositeDisposable.add(resumeDisposable);
    }

    private void startVideo() {

        peerConnection = factory.createPeerConnection(new PeerConnection.RTCConfiguration(Config.ICE_URI), new PeerConnection.Observer() {
            @Override
            public void onSignalingChange(PeerConnection.SignalingState newState) {
            }

            @Override
            public void onIceConnectionChange(PeerConnection.IceConnectionState newState) {
            }

            @Override
            public void onIceConnectionReceivingChange(boolean receiving) {
            }

            @Override
            public void onIceGatheringChange(PeerConnection.IceGatheringState newState) {
            }

            @Override
            public void onIceCandidate(IceCandidate candidate) {
                if (videoClient != null) {
                    videoClient.sendIce(candidate);
                }
            }

            @Override
            public void onIceCandidatesRemoved(IceCandidate[] candidates) {
            }

            @Override
            public void onAddStream(MediaStream stream) {
            }

            @Override
            public void onRemoveStream(MediaStream stream) {
            }

            @Override
            public void onDataChannel(DataChannel dataChannel) {
            }

            @Override
            public void onRenegotiationNeeded() {
                if (videoClient != null) {
                    videoClient.onNegotiationNeeded();
                }
            }

            @Override
            public void onTrack(RtpTransceiver transceiver) {
                MediaStreamTrack track = transceiver.getReceiver().track();
                if (track instanceof VideoTrack) {
                    ((VideoTrack) track).addSink(binding.remoteView);
                }
            }
        });

        videoClient = new VideoClient(peerConnection, Config.ClientRole.slave.equals(Config.ROLE), json -> {
            ApplicationDependencies.getApiClient().sendMessage(json);
        });

        // local audio
        MediaConstraints audioConstraints = new MediaConstraints();
        audioConstraints.mandatory.add(new MediaConstraints.KeyValuePair("googEchoCancellation", "true"));
        audioConstraints.mandatory.add(new MediaConstraints.KeyValuePair("googNoiseSuppression", "true"));
        audioConstraints.mandatory.add(new MediaConstraints.KeyValuePair("googAutoGainControl", "true"));

        audioSource = factory.createAudioSource(audioConstraints);
        audioTrack = factory.createAudioTrack("AUDIO", audioSource);

        Context context = getContext();
        if (context != null) {
            setSpeakerphone(context, true);
        }

        // local video
        videoCapturer = createCameraCapturer();
        if (videoCapturer != null) {
            SurfaceTextureHelper sth = SurfaceTextureHelper.create("CaptureThread", eglBase.getEglBaseContext());
            videoSource = factory.createVideoSource(false);
            videoCapturer.initialize(sth, requireContext(), videoSource.getCapturerObserver());
            videoCapturer.startCapture(1280, 720, 30);
            videoTrack = factory.createVideoTrack("VIDEO", videoSource);

            peerConnection.addTrack(audioTrack, Collections.singletonList("stream"));
            peerConnection.addTrack(videoTrack, Collections.singletonList("stream"));
            videoTrack.addSink(binding.localView);
        }

    }

    @Nullable
    private VideoCapturer createCameraCapturer() {
        Context context = getContext();
        if (context == null) {
            return null;
        }
        Camera2Enumerator enumerator = new Camera2Enumerator(context);
        String[] deviceNames = enumerator.getDeviceNames();
        for (String device : deviceNames) {
            if (enumerator.isFrontFacing(device)) {
                VideoCapturer capturer = enumerator.createCapturer(device, null);
                if (capturer != null) {
                    return capturer;
                }
            }
        }
        for (String device : deviceNames) {
            VideoCapturer capturer = enumerator.createCapturer(device, null);
            if (capturer != null) {
                return capturer;
            }
        }
        return null;
    }

    private void setSpeakerphone(Context context, boolean enable) {
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

        if (audioManager == null) {
            return;
        }

        audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (enable) {
                AudioDeviceInfo speakerDevice = null;
                for (AudioDeviceInfo device : audioManager.getAvailableCommunicationDevices()) {
                    if (device.getType() == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER) {
                        speakerDevice = device;
                        break;
                    }
                }
                if (speakerDevice != null) {
                    audioManager.setCommunicationDevice(speakerDevice);
                }
            } else {
                audioManager.clearCommunicationDevice();
            }
        } else {
            audioManager.setSpeakerphoneOn(enable);
            if (enable) {
                int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL);
                audioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, maxVolume, 0);
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        ApplicationDependencies.getApiClient().close();
        ApplicationDependencies.dispose();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        stopVideo();
    }

    public void stopVideo() {

        videoClient = null;

        if (peerConnection != null) {
            try {
                peerConnection.dispose();
            } catch (Exception ignored) {
            }
            peerConnection = null;
        }

        if (videoCapturer != null) {
            try {
                videoCapturer.stopCapture();
            } catch (InterruptedException ignored) {
            }
            try {
                videoCapturer.dispose();
            } catch (Exception ignored) {
            }
            videoCapturer = null;
        }

        if (videoSource != null) {
            try {
                videoSource.dispose();
            } catch (Exception ignored) {
            }
            videoSource = null;
        }

        if (audioSource != null) {
            try {
                audioSource.dispose();
            } catch (Exception ignored) {
            }
            audioSource = null;
        }

        if (videoTrack != null) {
            videoTrack.removeSink(binding.localView);
            videoTrack.removeSink(binding.remoteView);
            videoTrack.setEnabled(false);
            videoTrack.dispose();
            videoTrack = null;
        }

        if (audioTrack != null) {
            audioTrack.setEnabled(false);
            audioTrack.dispose();
            audioTrack = null;
        }

        if (factory != null) {
            factory.dispose();
            factory = null;
        }

        if (eglBase != null) {
            binding.localView.release();
            binding.remoteView.release();
            eglBase.release();
            eglBase = null;
        }

    }

    /// //////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    protected FragmentVideoBinding getViewBinding(LayoutInflater inflater, ViewGroup container) {
        FragmentVideoBinding binding = FragmentVideoBinding.inflate(inflater, container, false);
        binding.setViewModel(videoViewModel);
        binding.setHandler(this);
        binding.setLifecycleOwner(getViewLifecycleOwner()); // call after setAdapter
        return binding;
    }

}
