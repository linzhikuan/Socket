package com.lzk.core.socket.bean

sealed class TcpState {
    object Init : TcpState()

    object Connecting : TcpState()

    data class ConnectFailed(
        val throwable: Throwable,
    ) : TcpState()

    data class OnReceiveMsg(
        val byteArray: ByteArray,
    ) : TcpState() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as OnReceiveMsg
            return byteArray.contentEquals(other.byteArray)
        }

        override fun hashCode(): Int = byteArray.contentHashCode()
    }

    data class OnDisconnect(
        val throwable: Throwable,
    ) : TcpState()

    data class OnSendMsgFailed(
        val throwable: Throwable,
    ) : TcpState()

    data class OnReceiveMsgFailed(
        val throwable: Throwable,
    ) : TcpState()
}
