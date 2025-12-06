package com.lzk.core.socket

import com.lzk.core.socket.bean.UdpInfo
import kotlinx.coroutines.flow.SharedFlow

interface IUdpClient {
    val dataFlow: SharedFlow<UdpInfo>

    fun send(
        data: ByteArray,
        localPort: Int,
        remoteAddress: String,
        remotePort: Int,
    )

    fun destroy()
}
