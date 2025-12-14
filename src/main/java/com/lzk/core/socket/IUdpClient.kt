package com.lzk.core.socket

import com.lzk.core.socket.data.UdpState
import kotlinx.coroutines.flow.StateFlow

interface IUdpClient {
    val stateFlow: StateFlow<UdpState>

    fun send(
        data: ByteArray,
        localPort: Int,
        remoteAddress: String,
        remotePort: Int,
    )

    fun destroy()
}
