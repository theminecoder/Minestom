# protocol-bedrock

Bedrock Edition protocol support for Minestom, built on NetherNet.

NetherNet is the WebRTC-based transport used by Bedrock Edition 26.50+ and
required by 26.60. This module implements LAN discovery, signaling, SCTP
segmentation, Bedrock packet codecs, and session management.

The ICE/DTLS/SCTP stack is pluggable through
`net.minestom.server.protocol.bedrock.nethernet.NetherNetPeerFactory`. The JDK
has no WebRTC implementation, so the default factory refuses connections; install
a factory backed by libdatachannel, a pure-Java WebRTC stack, or a managed
service before clients can connect. See
[../docs/networking.md](../docs/networking.md) for a complete example.

Bedrock versions and their features are defined in
`net.minestom.server.protocol.bedrock.BedrockVersion`.
