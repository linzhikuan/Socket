package com.lzk.core.socket

interface ISocketClient {
    fun connect(
        ip: String,
        port: Int,
    ): Result<Boolean>

    fun sendMessage(data: ByteArray): Result<Boolean>

    fun close()
}
