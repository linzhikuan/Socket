package com.lzk.core.socket

interface ITcpClient {
    fun connect(
        ip: String,
        port: Int,
    ): Result<Boolean>

    fun sendMessage(data: ByteArray): Result<Boolean>

    fun close()
}
