package com.lzk.core.socket

interface ITcpClient {
    suspend fun connect(
        ip: String,
        port: Int,
    ): Result<Boolean>

    suspend fun sendMessage(data: ByteArray): Result<Boolean>

    fun close()
}
