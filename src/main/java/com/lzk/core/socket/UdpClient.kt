package com.lzk.core.socket

import com.lzk.core.socket.bean.UdpInfo
import com.lzk.core.socket.data.UdpState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

@Suppress("ktlint:standard:backing-property-naming")
class UdpClient : IUdpClient {
    companion object {
        private const val BUFFER_SIZE = 1024
    }

    private val scope = CoroutineScope(Job() + Dispatchers.IO)
    private val mutex = Mutex()
    private var datagramSocket: DatagramSocket? = null
    private var receiveJob: Job? = null

    private val _udpStateFlow = MutableStateFlow<UdpState>(UdpState.Init)

    override val stateFlow: StateFlow<UdpState> = _udpStateFlow.asStateFlow()

    override fun send(
        data: ByteArray,
        localPort: Int,
        remoteAddress: String,
        remotePort: Int,
    ) {
        scope.launch {
            mutex.withLock {
                runCatching {
                    if (datagramSocket == null) {
                        datagramSocket =
                            DatagramSocket(localPort).apply {
                                this.reuseAddress = true
                                this.soTimeout = 0 // 无超时，阻塞等待
                                startReceiving(this)
                            }
                    } else {
                        val sendPacket =
                            DatagramPacket(
                                data,
                                data.size,
                                InetAddress.getByName(remoteAddress),
                                remotePort,
                            )
                        datagramSocket?.send(sendPacket)
                    }
                }.onFailure {
                    _udpStateFlow.emit(UdpState.OnError)
                }
            }
        }
    }

    private fun startReceiving(datagramSocket: DatagramSocket) {
        receiveJob?.cancel()
        receiveJob =
            scope.launch {
                while (isActive && !datagramSocket.isClosed) {
                    // 每次循环都创建新的缓冲区和数据包
                    val buffer = ByteArray(BUFFER_SIZE)
                    val packet = DatagramPacket(buffer, buffer.size)
                    datagramSocket.receive(packet) // 这里会阻塞直到收到数据
                    // 提取实际接收到的数据（不是整个buffer）
                    val receivedData = packet.data.copyOf(packet.length)
                    val senderAddress = packet.address.hostAddress
                    val senderPort = packet.port
                    _udpStateFlow.emit(
                        UdpState.Receive(
                            UdpInfo(
                                receivedData,
                                senderAddress,
                                senderPort,
                                datagramSocket.localPort,
                            ),
                        ),
                    )
                }
            }
    }

    override fun destroy() {
        scope.launch {
            mutex.withLock {
                datagramSocket?.close()
                datagramSocket = null
                receiveJob?.cancel()
                receiveJob = null
            }
        }
    }
}
