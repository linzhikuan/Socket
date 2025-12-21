package com.lzk.core.socket

import com.lzk.core.socket.bean.UdpInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

@Suppress("ktlint:standard:backing-property-naming")
class UdpClient : IUdpClient {
    private constructor()

    companion object {
        private const val BUFFER_SIZE = 1024
        val instance: UdpClient by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) { UdpClient() }
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val mMutex = Mutex()
    private val mSocketMap = hashMapOf<Int, DatagramSocket>()
    private val mReceivingJobs = hashMapOf<Int, Job>()
    private val _udpDataFlow =
        MutableSharedFlow<UdpInfo>(
            extraBufferCapacity = 1,
            replay = 0,
            onBufferOverflow = BufferOverflow.DROP_LATEST,
        )

    override val udpDataFlow: SharedFlow<UdpInfo> = _udpDataFlow.asSharedFlow()

    override suspend fun sendMessage(
        data: ByteArray,
        ip: String,
        port: Int,
        localPort: Int,
    ) {
        mMutex
            .withLock {
                mSocketMap[localPort] ?: DatagramSocket(localPort).apply {
                    this.reuseAddress = true
                    this.soTimeout = 0 // 无超时，阻塞等待
                    mSocketMap[localPort] = this
                    startReceiving(localPort)
                }
            }.apply {
                val sendPacket =
                    DatagramPacket(
                        data,
                        data.size,
                        InetAddress.getByName(ip),
                        port,
                    )
                this.send(sendPacket)
            }
    }

    private fun startReceiving(localPort: Int) {
        mReceivingJobs[localPort]?.cancel()
        val datagramSocket = mSocketMap[localPort] ?: return
        val job =
            scope.launch {
                while (isActive && !datagramSocket.isClosed) {
                    try {
                        // 每次循环都创建新的缓冲区和数据包
                        val buffer = ByteArray(BUFFER_SIZE)
                        val packet = DatagramPacket(buffer, buffer.size)

                        datagramSocket.receive(packet) // 这里会阻塞直到收到数据

                        // 提取实际接收到的数据（不是整个buffer）
                        val receivedData = packet.data.copyOf(packet.length)
                        val senderAddress = packet.address.hostAddress
                        val senderPort = packet.port
                        // 在这里处理接收到的数据
                        handleReceivedData(
                            receivedData,
                            senderAddress,
                            senderPort,
                            datagramSocket.localPort,
                        )
                    } catch (e: Exception) {
                        if (isActive && !datagramSocket.isClosed) {
                            delay(1000)
                        }
                    }
                }
            }

        mReceivingJobs[localPort] = job
    }

    private fun handleReceivedData(
        data: ByteArray,
        fromIp: String?,
        fromPort: Int,
        localPort: Int,
    ) {
        scope.launch {
            _udpDataFlow.emit(UdpInfo(data, fromIp, fromPort, localPort))
        }
    }

    /**
     * 关闭指定端口的UDP socket
     */
    override fun closeSocket(port: Int) {
        scope.launch {
            mMutex.withLock {
                mReceivingJobs[port]?.cancel()
                mSocketMap[port]?.close()
                mSocketMap.remove(port)
                mReceivingJobs.remove(port)
            }
        }
    }

    /**
     * 关闭所有UDP socket
     */
    override fun closeAllSockets() {
        scope.launch {
            mMutex.withLock {
                mReceivingJobs.values.forEach { it.cancel() }
                mSocketMap.values.forEach { it.close() }
                mSocketMap.clear()
                mReceivingJobs.clear()
            }
        }
    }
}
