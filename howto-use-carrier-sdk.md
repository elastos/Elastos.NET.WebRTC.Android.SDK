## WebRTCClient 

WebRTCClient 应该是一个实现类，并且应该static 方法创建WebRTCClient实例：
```
WebRTCClient WebRTCClient::CreateInstance(Carrier carrier)
```

也就是说 WebRTCClient依赖于已有的用户Carrier实例。



## CarrierExtension

CarrierExtesion 目前是Carrier SDK 特意实现的抽象类为WebRTC服务（不建议普通开发者使用CarrierExtension） .
WebRTCClient 可以继承CarrierExtension抽象类并实现其的抽象方法

```
class WebRTCClient implements CarrierExtension {
...
void onFriendInvite(Carrier carrier, String from, String data)  {
   // you own handler implementation
}
...
}
```

## register Extension
调用CarrierExtension::registerExtension将 extension module注册到Carrier SDk中，以保证后续能够收到 属于webrtc 模块的onFriendInvite callback。

## Invite/Reply Friend
调用CarrierExtension::InviteFriend/replyFriendInvite方法 与对端webrtc peer 交互sdp/控制信息。

## GetTurnServerInfo
可以直接调用CarrierExtension::GetTurnServerInfo() 方法获取底层TurnServer的所有信息。