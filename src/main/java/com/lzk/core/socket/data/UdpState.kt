package com.lzk.core.socket.data

import com.lzk.core.socket.bean.UdpInfo

sealed class UdpState {
    object Init : UdpState()

    data class Receive(
        val udpInfo: UdpInfo,
    ) : UdpState()

    object OnError : UdpState()
}
