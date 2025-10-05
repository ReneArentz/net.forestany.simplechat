package net.forestany.simplechat.chat

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import net.forestany.simplechat.GlobalInstance
import net.forestany.simplechat.MainActivity
import net.forestany.simplechat.Other
import net.forestany.simplechat.R

class ChatActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "ChatActivity"
    }

    private lateinit var rV_chat: RecyclerView
    private lateinit var eT_message: EditText
    private lateinit var btn_send: Button
    private lateinit var adapter: ChatViewAdapter
    private lateinit var uiThread: Thread

    private var userName: String = ""
    private var roomName: String = ""
    private var serverIp: String = ""
    private var serverPort: Int = 0

    private var messages: List<Message> = listOf(
        //Message(false, "", "%WAIT%", LocalDateTime.now()),
        //Message(false, "User One", "Hello there!", LocalDateTime.of(2025, 2, 4, 11, 15, 32)),
        //Message(true, "User Two", "Hi!", LocalDateTime.of(2025, 2, 4, 11, 20, 11)),
        //Message(true, "User Two", "Do you know the password for the server?", LocalDateTime.of(2025, 2, 4, 11, 20, 42)),
        //Message(false, "User One", "Sorry, this information is sensitive. I cannot write it to you via chat!", LocalDateTime.of(2025, 2, 4, 11, 22, 0)),
        //Message(true, "User Two", "Alright. I will ask the service desk.", LocalDateTime.of(2025, 2, 4, 11, 24, 52)),
        //Message(true, "User Two", "%EXIT%", LocalDateTime.now())
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_chat)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.chat_activity)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        adapter = ChatViewAdapter(this, messages)

        rV_chat = findViewById(R.id.rV_chat)
        rV_chat.adapter = adapter
        rV_chat.layoutManager = LinearLayoutManager(this)

        eT_message = findViewById(R.id.eT_message)

        userName = intent.extras?.getString("CHAT_USER") ?: "NO_USER_ENTERED"

        if ((userName == "NO_USER_ENTERED") || (userName.isBlank())) {
            setResult(MainActivity.RETURN_CODE_NO_USER)
            finish()
        }

        roomName = intent.extras?.getString("CHAT_ROOM") ?: "NO_ROOM_ENTERED"

        if ((roomName == "NO_ROOM_ENTERED") || (roomName.isBlank())) {
            setResult(MainActivity.RETURN_CODE_NO_ROOM)
            finish()
        }

        val s_networkInterface: String = intent.extras?.getString("NETWORK_INTERFACE") ?: getString(
            R.string.chat_no_network_interfaces
        )

        if ( (s_networkInterface == getString(R.string.chat_no_network_interfaces)) || (s_networkInterface.isBlank()) || (!s_networkInterface.contains(":")) ) {
            setResult(MainActivity.RETURN_CODE_NO_NETWORK_INTERFACE)
            finish()
        } else {
            val serverInfo: List<String> = s_networkInterface.split(":")
            serverIp = serverInfo[0]
            serverPort = serverInfo[1].toInt()
        }

        btn_send = findViewById(R.id.bt_send)
        btn_send.setOnClickListener {
            if (eT_message.text.toString().trim().isNotBlank()) {
                val o_timestamp: java.time.LocalDateTime = java.time.LocalDateTime.now()
                val s_message = "${net.forestany.forestj.lib.Helper.toISO8601UTC(o_timestamp)}|${userName}|${eT_message.text}"

                GlobalInstance.get().enqueueMessageBox(
                    s_message
                )

                GlobalInstance.get().addChatMessageToQueue(
                    Message(
                        false,
                        userName,
                        eT_message.text.toString(),
                        o_timestamp
                    )
                )
            }

            eT_message.text.clear()
            rV_chat.scrollToPosition(adapter.itemCount - 1)
        }

        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                /* execute anything, e.g. finish() - if nothing is here, nothing happens pushing main back button */
                val builder = AlertDialog.Builder(this@ChatActivity, R.style.AlertDialogStyle)
                    .setTitle(getString(R.string.chat_exit_title))
                    .setMessage(getString(R.string.chat_exit_message))
                    .setPositiveButton(getString(R.string.text_yes)) { dialog, _ ->
                        setResult(MainActivity.RETURN_CODE_OWN_EXIT)
                        finish()

                        dialog.dismiss()
                    }
                    .setNegativeButton(
                        getString(R.string.text_no)
                    ) { dialog, _ ->
                        dialog.dismiss()
                    }

                val alert = builder.create()
                alert.show()
            }
        })

        if (GlobalInstance.get().b_isServer) {
            Log.i(TAG, "created room '$roomName' as '$userName' with network interface '$serverIp:$serverPort'")
        } else {
            Log.i(TAG, "joined room '$roomName' as '$userName' with network interface '$serverIp:$serverPort'")
        }

        adapter.addData(
            Message(
                false,
                "",
                MainActivity.CHAT_WAIT_MESSAGE,
                java.time.LocalDateTime.now()
            )
        )

        val rootLayout = findViewById<View>(R.id.chat_activity)
        val chatbox = findViewById<View>(R.id.rL_chatbox)

        rootLayout.viewTreeObserver.addOnGlobalLayoutListener {
            val rect = android.graphics.Rect()
            rootLayout.getWindowVisibleDisplayFrame(rect)

            val screenHeight = rootLayout.rootView.height
            val keypadHeight = screenHeight - rect.bottom

            if (keypadHeight > screenHeight * 0.15) {
                // keyboard is visible -> push chatbox up
                chatbox.translationY = -keypadHeight.toFloat() + chatbox.height
            } else {
                // keyboard hidden -> reset
                chatbox.translationY = 0f
            }
        }

        if ((GlobalInstance.get().b_isServer) && (GlobalInstance.get().o_communicationLobby == null)) {
            /*val wifi = getSystemService(WIFI_SERVICE) as WifiManager
            val multicastLock = wifi.createMulticastLock("multicastLockServer")
            multicastLock.setReferenceCounted(true)
            multicastLock.acquire()*/

            Other().netLobby(
                GlobalInstance.get().getPreferences()["udp_multicast_ip"].toString(), //MainActivity.UDP_MULTICAST_IP,
                Integer.parseInt(GlobalInstance.get().getPreferences()["udp_multicast_port"].toString()), //MainActivity.UDP_MULTICAST_PORT,
                Integer.parseInt(GlobalInstance.get().getPreferences()["udp_multicast_ttl"].toString()), //MainActivity.UDP_MULTICAST_TTL,
                roomName,
                serverIp,
                serverPort
            )
        }

        if (GlobalInstance.get().o_communicationChat == null) {
            Other().netChat(serverIp, serverPort)
        }

        uiThread = object : Thread() {
            override fun run() {
                var b_wait15sec = false
                var i_rc = -1

                while (true) {
                    try {
                        runOnUiThread {
                            if (GlobalInstance.get().b_connected) {
                                adapter.clearWaitMessage()
                            }

                            var message: Message?

                            do {
                                message = GlobalInstance.get().getChatMessageFromQueue()

                                if (message != null) {
                                    if (message.message == MainActivity.CHAT_LOST_MESSAGE) {
                                        b_wait15sec = true
                                        i_rc = MainActivity.RETURN_CODE_LOST_CONNECTION
                                    } else if (message.message == MainActivity.CHAT_EXIT_MESSAGE) {
                                        b_wait15sec = true
                                        i_rc = MainActivity.RETURN_CODE_OTHER_EXIT
                                    }

                                    adapter.addData(message)
                                    rV_chat.scrollToPosition(adapter.itemCount - 1)
                                }
                            } while (message != null)
                        }

                        if (b_wait15sec) {
                            sleep(15000)
                            setResult(i_rc)
                            finish()
                        } else {
                            sleep(500)
                        }
                    } catch (_: InterruptedException) {
                        break
                    }
                }
            }
        }

        uiThread.start()

        Log.v(TAG, "onCreate $TAG")
    }

    override fun onStart() {
        super.onStart()

        Log.v(TAG, "onStart $TAG")
    }

    override fun onResume() {
        super.onResume()

        Log.v(TAG, "onResume $TAG")
    }

    override fun onPause() {
        super.onPause()

        Log.v(TAG, "onPause $TAG")
    }

    override fun onStop() {
        super.onStop()

        Log.v(TAG, "onStop $TAG")
    }

    override fun onRestart() {
        super.onRestart()

        Log.v(TAG, "onRestart $TAG")
    }

    override fun onDestroy() {
        super.onDestroy()

        try {
            uiThread.interrupt()
        } catch (_: Exception) {

        }

        if ((GlobalInstance.get().b_isServer) && (GlobalInstance.get().o_communicationLobby != null)) {
            try {
                GlobalInstance.get().o_threadLobby?.interrupt()
            } catch (_: Exception) {

            } finally {
                GlobalInstance.get().o_threadLobby = null
            }

            try {
                GlobalInstance.get().o_communicationLobby?.stop()
            } catch (_: Exception) {

            } finally {
                GlobalInstance.get().o_communicationLobby = null
            }

            GlobalInstance.get().clearClientLobbyEntries()
        }

        if (GlobalInstance.get().o_communicationChat != null) {
            val o_timestamp: java.time.LocalDateTime = java.time.LocalDateTime.now()
            val s_foo = "${net.forestany.forestj.lib.Helper.toISO8601UTC(o_timestamp)}|${userName}|${MainActivity.CHAT_EXIT_MESSAGE}"

            GlobalInstance.get().enqueueMessageBox(
                s_foo
            )

            Thread.sleep(5000)
        }

        try {
            GlobalInstance.get().o_threadChat?.interrupt()
        } catch (_: Exception) {

        } finally {
            GlobalInstance.get().o_threadChat = null
        }

        try {
            GlobalInstance.get().o_communicationChat?.stop()
        } catch (_: Exception) {

        } finally {
            GlobalInstance.get().o_communicationChat = null
        }

        GlobalInstance.get().o_lastPing = null
        GlobalInstance.get().b_isServer = false
        GlobalInstance.get().b_connected = false
        GlobalInstance.get().clearChatMessageQueue()
        GlobalInstance.get().clearMessageBox()
        adapter.clearMessageList()

        Log.v(TAG, "onDestroy $TAG")
    }
}