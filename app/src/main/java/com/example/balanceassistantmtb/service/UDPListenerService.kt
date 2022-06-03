package com.example.balanceassistantmtb.service

import android.util.Log
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

object UDPListenerService {

     fun receiveUDPPackage(ipAddress: String, port: Int): String {
        val buffer = ByteArray(2048)
        var socket: DatagramSocket? = null
         return try {
             //Keep a socket open to listen to all the UDP trafic that is destined for this port
             socket = DatagramSocket(port, InetAddress.getByName(ipAddress))
             socket.broadcast = true
             val packet = DatagramPacket(buffer, buffer.size)
             socket.receive(packet)
             Log.d("ReceivedUDP","open fun receiveUDP packet received = " + packet.data)
             "data"
         } catch (e: Exception) {
             Log.d("ReceiveUDPError", "open fun receiveUDP catch exception.$e")
             e.printStackTrace()
             "invalid_ip_address"
         } finally {
             socket?.close()
         }
    }
}