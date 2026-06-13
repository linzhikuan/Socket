package com.lzk.core.socket

import com.lzk.core.socket.bean.TcpState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.atomic.AtomicBoolean

class TcpClient : ITcpClient {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val isConnected = AtomicBoolean(false)
    private val socket: Socket by lazy {
        Socket().apply {
            setTcpNoDelay(true)
            setKeepAlive(true)
            setSoLinger(true, 0)
        }
    }
    private val _state = MutableStateFlow<TcpState>(TcpState.Init)

    val state: SharedFlow<TcpState>
        get() = _state.asSharedFlow()

    override suspend fun connect(
        ip: String,
        port: Int,
    ): Result<Boolean> =
        withContext(Dispatchers.IO) {
            runCatching {
                _state.value = TcpState.Connecting
                socket.connect(InetSocketAddress(ip, port))
                startRev()
                true
            }.onFailure {
                _state.value = TcpState.ConnectFailed(it)
            }
        }

    override suspend fun sendMessage(data: ByteArray): Result<Boolean> =
        withContext(Dispatchers.IO) {
            runCatching {
                socket.getOutputStream().write(data)
                true
            }.onFailure {
                _state.value = TcpState.OnSendMsgFailed(it)
            }
        }

    override fun close() {
        socket.close()
        scope.cancel()
    }

    private fun startRev() {
        runCatching {
            val inputStream = socket.getInputStream()
            val buffer = ByteArray(1024)
            while (isConnected.get()) {
                runCatching {
                    val read = inputStream.read(buffer)
                    if (read == -1) throw IllegalStateException("read size -1")
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
