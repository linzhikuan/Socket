package com.lzk.core.socket

import com.lzk.core.socket.bean.UdpInfo
import kotlinx.coroutines.flow.SharedFlow

interface IUdpClient {
    val udpDataFlow: SharedFlow<UdpInfo>

    suspend fun sendMessage(
        data: ByteArray,
        ip: String,
        port: Int,
        localPort: Int,
    )

    fun closeSocket(port: Int)

    fun closeAllSockets()
}
