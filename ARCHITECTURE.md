Elastos WebRTC Android SDK Architecture
===========================

## Introduction

Elastos WebRTC Android SDK is a java api set for webrtc connection using elastos carrier network. 
With the Elastos Android WebRTC SDK, it is possible to build voip applications for mobile phones, tablets, wearables, TVs and car media systems that run on the Android Operating System (OS) while utilizing the functionalities of the Elastos Carrier and WebRTC protocol.

The Elastos Carrier is a decentralized and encrypted peer-to-peer (P2P) communication framework that routes network traffic between virtual machines and Decentralized Applications (DApps).

With WebRTC, you can add real-time communication capabilities to your application that works on top of an open standard. It supports video, voice, and generic data to be sent between peers, allowing developers to build powerful voice- and video-communication solutions. 

Elastos WebRTC Android SDK combines Elastos Carrier and WebRTC, it use Elastos Carrier network as signaling services and STUN/TURN server.

If you want to know how to build the sdk from source code, please refer to:
[Elastos WebRTC Android SDK](./README.md)

## What can Elastos WebRTC Android SDK do for a real webrtc based project?

***************
### 1. What we should care about in a real webrtc based project.

WebRTC enables peer to peer communication.

BUT...

WebRTC still needs services:

For clients to exchange metadata to coordinate communication: this is called signaling.
To cope with network address translators (NATs) and firewalls. 

So, in the api of Elastos WebRTC Android SDK, we supply a CarrierChannelClient (org.elastos.carrier.webrtc.signaling.CarrierChannelClient) for signaling service through carrier network.
Generally you don't need to use this class directly, it's used in the CarrierWebrtcClient (implemented CarrierChannelEvents interface) for SDP and ICE candidates message handling.

We also provide CarrierTurnServerFetcher (org.elastos.carrier.webrtc.CarrierTurnServerFetcher) class for getting STUN/TURN server from carrier network, it's an asynchronous service, 
so you don't need to worry about the situation that the ICE candidates communication process will be blocked.
Also, you don't need to use this class directly in general cases, it's been encapsulated in the CarrierWebrtcClient (implementing CarrierChannelEvents interface) for PeerConnection.IceServer automatic preparation.  

### 2. How to use WebrtcClient
In the real webrtc project, you need to handle the peer connection and implement your signaling and STUN/TUNR services, as we already introduced, the signaling and STUN/TUNR service have been
implemented in CarrierChannelClient and CarrierTurnServerFetcher and encapsulated in CarrierWebrtcClient.

The sdk also supply a peer connection help class CarrierPeerConnectionClient (org.elastos.carrier.webrtc.CarrierPeerConnectionClient) for handling webrtc peer connection events (which are defined in org.elastos.carrier.webrtc.CarrierPeerConnectionClient.PeerConnectionEvents).

#### Initial peerConnection using CarrierPeerConnectionClient object
```

    // Create peer connection client in an Activity, generally in the onCreate() method.
    // The second parameter is the PeerConnectionEvents, here the Activity (this) has already implemented PeerConnectionEvents interface. 
    carrierPeerConnectionClient = new CarrierPeerConnectionClient(
            getApplicationContext(), this);
            
    // Then call the createPeerConnectionFactory(options) method to initialize the CarrierPeerConnectionClient
    // You can also set your own PeerConnectionFactory.Options through this static method. 
    PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();
    carrierPeerConnectionClient.createPeerConnectionFactory(options);

```

#### Initial CarrierWebrtcClient object to register signaling event handler and implement the p2p communication action such as initialCall() and disconnectFromCall().

```
    // Create connection client from an Activity, generally in the onCreate() method.
    // The first parameter is the CarrierChannelEvents, here the Activity (this) has already implemented CarrierChannelEvents interface.
    webrtcClient = new CarrierWebrtcClient(this, getApplicationContext());

```

#### Initial and start a call for webrtc communication.
```

    //Initial a call connection. The callerAddress and calleeAddress represent the carrier address for the calleer and callee.
    webrtcClient.initialCall(calleeAddress);

    //Send a call invite to a carrier User. We need carrier userId instead of carrier address for the call invitation. 
    String carrierUserId = CarrierClient.getInstance(this).getUserIdFromAddress(remoteAddress);
    webrtcClient.sendInvite();

```

### Implement PeerConnectionEvents (org.elastos.carrier.webrtc.PeerConnectionEvents) and SignalingEvents (org.elastos.carrier.webrtc.WebrtcClient.SignalingEvents) interfaces
In the previous we initialized the CarrierPeerConnectionClient and CarrierWebrtcClient, both has the "this" parameter,
as described in the comments, the Activity implements PeerConnectionEvents and CarrierChannelEvents interfaces.

The BaseCallActivity has the reference implementation for these two interfaces. You can change your Activity to extend BaseCallActivity or implement the interface by yourself.


```
  /**
   * Callback interface for messages delivered on signaling channel.
   *
   * <p>Methods are guaranteed to be invoked on the UI thread of |activity|.
   */
  interface SignalingEvents {
    /**
     * Callback fired once webrtc call started and the webrtc connection's
     * SignalingParameters are extracted.
     */
    void onCallInitialized(final SignalingParameters params);

    /**
     * Callback fired once remote SDP is received.
     */
    void onRemoteDescription(final SessionDescription sdp);

    /**
     * Callback fired once remote Ice candidate is received.
     */
    void onRemoteIceCandidate(final IceCandidate candidate);

    /**
     * Callback fired once remote Ice candidate removals are received.
     */
    void onRemoteIceCandidatesRemoved(final IceCandidate[] candidates);

    /**
     * Callback fired once channel is closed.
     */
    void onChannelClose();

    /**
     * Callback fired once channel error happened.
     */
    void onChannelError(final String description);

    void onCreateOffer();
  }

```

The most important method of SignalingEvents is onCallInitialized(), it's a callback function, once the webrtc call initialize by the WebrtcClient.initialCall(),
it well be fired with the SignalingParameters parameters.

If you want to handle the sdp and ICE candidates messages by yourself, you can implement PeerConnectionEvents interface, otherwise you can just extend the BaseCallActivity.

![](https://gitlab.com/elastos/Elastos.NET.WebRTC.Android.SDK/-/raw/master/doc/images/peerConnection.jpg)

```
/**
 * Peer connection events.
 */
public interface PeerConnectionEvents {
    /**
     * Callback fired once local SDP is created and set.
     */
    void onLocalDescription(final SessionDescription sdp);

    /**
     * Callback fired once local Ice candidate is generated.
     */
    void onIceCandidate(final IceCandidate candidate);

    /**
     * Callback fired once local ICE candidates are removed.
     */
    void onIceCandidatesRemoved(final IceCandidate[] candidates);

    /**
     * Callback fired once connection is established (IceConnectionState is
     * CONNECTED).
     */
    void onIceConnected();

    /**
     * Callback fired once connection is disconnected (IceConnectionState is
     * DISCONNECTED).
     */
    void onIceDisconnected();

    /**
     * Callback fired once DTLS connection is established (PeerConnectionState
     * is CONNECTED).
     */
    void onConnected();

    /**
     * Callback fired once DTLS connection is disconnected (PeerConnectionState
     * is DISCONNECTED).
     */
    void onDisconnected();

    /**
     * Callback fired once peer connection is closed.
     */
    void onPeerConnectionClosed();

    /**
     * Callback fired once peer connection statistics is ready.
     */
    void onPeerConnectionStatsReady(final StatsReport[] reports);

    /**
     * Callback fired once peer connection error happened.
     */
    void onPeerConnectionError(final String description);
}

```


## Contribution

We welcome contributions to the Elastos WebRTC Android SDK Project.

## Acknowledgments

A sincere thank you to all teams and projects that we rely on directly or indirectly.

## License
This project is licensed under the terms of the [GPLv3 license](./LICENSE)

