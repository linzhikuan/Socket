package com.lzk.core.socket.bean

data class UdpInfo(
    val data: ByteArray,
    val fromIp: String?,
    val fromPort: Int,
    val localPort: Int,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UdpInfo

        if (fromPort != other.fromPort) return false
        if (localPort != other.localPort) return false
        if (!data.contentEquals(other.data)) return false
        if (fromIp != other.fromIp) return false

        return true
    }

    override fun hashCode(): Int {
        var result = fromPort
        result = 31 * result + localPort
        result = 31 * result + data.contentHashCode()
        result = 31 * result + (fromIp?.hashCode() ?: 0)
        return result
    }
}