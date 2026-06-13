package com.lzk.core.socket

import com.lzk.core.socket.bean.TcpState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket

class TcpClient : ITcpClient {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var socket: Socket? = null
    private val _state = MutableStateFlow<TcpState>(TcpState.Init)

    val state: SharedFlow<TcpState>
        get() = _state.asSharedFlow()

    override suspend fun connect(
        ip: String,
        port: Int,
    ): Result<Boolean> =
        withContext(Dispatchers.IO) {
            if (socket != null) {
                return@withContext Result.failure(IllegalStateException("socket is exit"))
            }
            runCatching {
                _state.value = TcpState.Connecting
                Socket().apply {
                    socket = this
                    setTcpNoDelay(true)
                    setKeepAlive(true)
                    setSoLinger(true, 0)
                    connect(InetSocketAddress(ip, port))
                    if (isConnected) {
                        _state.value = TcpState.ConnectSuccess
                        startRev(this)
                    } else {
                        throw IllegalStateException("连接失败")
                    }
                }
                true
            }.onFailure {
                close()
                _state.value = TcpState.ConnectFailed(it)
            }
        }

    override suspend fun sendMessage(data: ByteArray): Result<Boolean> =
        withContext(Dispatchers.IO) {
            runCatching {
                socket?.getOutputStream()?.write(data)
                true
            }.onFailure {
                _state.value = TcpState.OnSendMsgFailed(it)
            }
        }

    override fun close() {
        val exception =
            runCatching {
                socket?.close()
                socket = null
            }.exceptionOrNull()
        _state.value = TcpState.OnClosed(exception)
    }

    private fun startRev(socket: Socket) {
        runCatching {
            val inputStream = socket.getInputStream()
            val buffer = ByteArray(1024)
            while (socket.isConnected) {
                runCatching {
                    val read = inputStream.read(buffer)
                    if (read == -1) return@runCatching
                    // 处理接收到的数据
                    val buff = ByteArray(read)
                    System.arraycopy(buffer, 0, buff, 0, read)
                    scope.launch {
                        _state.value = TcpState.OnReceiveMsg(buffer)
                    }
                }.onFailure { error ->
                    scope.launch {
                        _state.value = TcpState.OnReceiveMsgFailed(error)
                    }
                }
            }
        }.onFailure { error ->
            scope.launch {
                _state.value = TcpState.OnReceiveMsgFailed(error)
            }
        }
        close()
    }
}
