package com.example.balanceassistantmtb.service

import android.os.Handler
import android.util.Log
import java.io.IOException
import java.net.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class UdpClient(private val handler: Handler) {
    private val tag = "UdpSocket"

    private var mThreadPool: ExecutorService? = null
    private var socket: DatagramSocket? = null
    private var receivePacket: DatagramPacket? = null
    private val bufferLength = 1024
    private val receiveByte = ByteArray(bufferLength)

    private var isThreadRunning = false
    private lateinit var clientThread: Thread

    init {
        val cpuNumbers = Runtime.getRuntime().availableProcessors()
        mThreadPool = Executors.newFixedThreadPool(cpuNumbers * 5)
    }

    fun startUDPSocket() {
        if (socket != null) return
        try {
            //Keep a socket open to listen to all the UDP traffic that is destined for this port
            socket = DatagramSocket(59379, InetAddress.getByName("0.0.0.0")) /*InetAddress.getByName("0.0.0.0")*/
            if (receivePacket == null)
                receivePacket = DatagramPacket(receiveByte, bufferLength)
            startSocketThread()
        } catch (e: SocketException) {
            e.printStackTrace()
        }

    }

    private fun startSocketThread() {
        clientThread = Thread {
            Log.d(tag, "clientThread is running...")
            receiveMessage()
        }
        isThreadRunning = true
        clientThread.start()
    }

    private fun receiveMessage() {
        while (isThreadRunning) {
            Log.d(tag, "listening to receive message...")
            try {
                socket?.receive(receivePacket)
                if (receivePacket == null || receivePacket?.length == 0)
                    continue

                //multi thread to handle multi packages
                mThreadPool?.execute {
                    val strReceive = String(receivePacket!!.data, receivePacket!!.offset, receivePacket!!.length)
                    Log.d(tag, strReceive + " from " + receivePacket!!.address.hostAddress + ":" + receivePacket!!.port)

                    handler.sendMessage(handler.obtainMessage(1,strReceive))
                    receivePacket?.length = bufferLength
                }
            } catch (e: IOException) {
                stopUDPSocket()
                e.printStackTrace()
                return
            }
        }
    }

    fun stopUDPSocket() {
        isThreadRunning = false
        receivePacket = null
        clientThread.interrupt()
        if (socket != null) {
            socket?.close()
            socket = null
        }
        Log.d(tag, "UDP socket stopped")
    }

}