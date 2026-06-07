package com.lzk.core.socket

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.atomic.AtomicBoolean

class SocketClient : ISocketClient {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val isConnected = AtomicBoolean(false)
    private val socket: Socket by lazy {
        Socket().apply {
            setTcpNoDelay(true)
            setKeepAlive(true)
            setSoLinger(true, 0)
        }
    }

    override fun connect(
        ip: String,
        port: Int,
    ): Result<Boolean> =
        runCatching {
            socket.connect(InetSocketAddress(ip, port))
            startRev()
            true
        }

    override fun sendMessage(data: ByteArray): Result<Boolean> =
        runCatching {
            socket.getOutputStream().write(data)
            true
        }

    override fun close() {
        socket.close()
    }

    private fun startRev() {
        val inputStream = socket.getInputStream()
        val buffer = ByteArray(1024)
        while (isConnected.get()) {
            val read = inputStream.read(buffer)
            if (read == -1) break
            // 处理接收到的数据
        }
    }
}
