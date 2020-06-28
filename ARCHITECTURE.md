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

### How to use WebrtcClient
In the real webrtc project, you need to handle the call events by implements CallHandler, you will receive
onInvite callback if someone is calling you.


#### Initial WebrtcClient to register signaling event handler and implement the p2p communication action such as inviteCall() and disconnectFromCall().

```
    import org.elastos.carrier.webrtc.call.CallHandler
    import org.elastos.carrier.webrtc.PeerConnectionParametersBuilder;

    // set PeerConnection parameters, this could be null
    PeerConnectionParametersBuilder parameters = PeerConnectionParametersBuilder.builder().build;
    // implements org.elastos.carrier.webrtc.call.CallHandler
    CallHandlerImpl callHandler = new CallHandlerImpl();
    // initialize WebrtcClient.
    WebrtcClient.initialize(context, Carrier.getInstance(), callHandler,  parameters);

```

#### start a call for webrtc communication.
```

    // Send a call invite to a carrier User.
    WebrtcClient.getInstance().makeCall(friendId);

    // Render video wherever you want
    WebrtcClient.getInstance().renderVideo(localRenderer, remoteRenderer);

```

### when you receive invite
```

    // accept invite
    WebrtcClient.getInstance().answerCall();
    // reject invite
    WebrtcClient.getInstance().rejectCall();
    // Render video wherever you want
    WebrtcClient.getInstance().renderVideo(localRenderer, remoteRenderer);

```

### Implement CallHandler (org.elastos.carrier.webrtc.call.CallHandler) interface
In the previous we initialized WebrtcClient, has the "this" parameter,

```
  package org.elastos.carrier.webrtc.call;

  /**
   * call events
   */
  public interface CallHandler {

      /**
       * fired when receive invite from your friends
       * @param friendId who is calling you
       */
      void onInvite(String friendId, boolean audio, boolean video);

      /**
       * when your friend accept you invite
       */
      void onAnswer();

      /**
       * peer is connected
       */
      void onActive();

      /**
       * when remote hangup or reject invite
       * @param reason
       */
      void onEndCall(CallReason reason);

      /**
       * webrtc ice connected
       */
      void onIceConnected();

      /**
       * webrtc ice disconnected
       */
      void onIceDisConnected();

      /**
       * webrtc connect error
       * @param description
       */
      void onConnectionError(String description);

      /**
       * webrtc connection closed
       */
      void onConnectionClosed();

  }


```


## Contribution

We welcome contributions to the Elastos WebRTC Android SDK Project.

## Acknowledgments

A sincere thank you to all teams and projects that we rely on directly or indirectly.

## License
This project is licensed under the terms of the [GPLv3 license](./LICENSE)

