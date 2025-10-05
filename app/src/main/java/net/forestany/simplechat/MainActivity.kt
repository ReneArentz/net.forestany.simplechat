package net.forestany.simplechat

// android studio: collapse all methods: ctrl + shift + * and then 1 on numpad
// android studio: expand all with ctrl + shift + numpad + several times

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.os.LocaleListCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import net.forestany.simplechat.Util.errorSnackbar
import net.forestany.simplechat.Util.notifySnackbar
import net.forestany.simplechat.chat.ChatActivity
import net.forestany.simplechat.lobby.LobbyActivity
import net.forestany.simplechat.settings.SettingsActivity
import kotlin.system.exitProcess

class MainActivity : AppCompatActivity() {
    /*
    HowTo interconnect two android emulators
        both must have separate ips; check android WLAN settings and set static ips
        get emulator names
            /home/user/Android/Sdk/platform-tools -> ./adb devices
                List of devices attached
                emulator-5554	device
                emulator-5556	device
            ./adb -s emulator-5554 emu avd name
                Medium_Phone_29_10_One
        adb setting for emulator where server is hosted; emulator-name -> emulator-5554; forward tcp:client-port to tcp:host-port
            /home/user/Android/Sdk/platform-tools
            ./adb -s emulator-5554 forward tcp:12365 tcp:11365
        server socket (use emulator-5554 for server)
            0.0.0.0:11365
        client socket
            10.0.2.2:12365

        see more https://stackoverflow.com/questions/4278037/communication-between-two-android-emulators
     */

    companion object {
        private const val TAG = "MainActivity"

        const val RETURN_CODE_NO_USER = 3742
        const val RETURN_CODE_NO_ROOM = 4238
        const val RETURN_CODE_NO_NETWORK_INTERFACE = 5748
        const val RETURN_CODE_LOST_CONNECTION = 1654
        const val RETURN_CODE_OTHER_EXIT = 2683
        const val RETURN_CODE_OWN_EXIT = 8948
        const val RETURN_CODE_LOBBY_EXIT = 1356
        const val RETURN_CODE_INVALID_LOBBY = 4727

        const val SETTINGS_UDP_NETWORK_INTERFACE_NAME = "wlan0"
        const val SETTINGS_UDP_MULTICAST_IP = "FF05:0:0:0:0:0:0:342"
        const val SETTINGS_UDP_MULTICAST_PORT = "12805"
        const val SETTINGS_UDP_MULTICAST_TTL = "5"
        const val SETTINGS_TCP_SERVER_PORT = "12365"
        const val SETTINGS_TCP_COMMON_PASSPHRASE = "1234567890abcdefghijklmnopqrstuvwxyz"
        const val SETTINGS_TCP_ENCRYPTION = "false"

        const val CHAT_LOST_MESSAGE = "%LOST%"
        const val CHAT_EXIT_MESSAGE = "%EXIT%"
        const val CHAT_WAIT_MESSAGE = "%WAIT%"
        const val CHAT_PING_MESSAGE = "%PING%"
    }

    private lateinit var btn_find: Button
    private lateinit var btn_createRoom: Button

    private val launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        when (it.resultCode) {
            RESULT_OK -> {

            }

            RETURN_CODE_NO_USER -> {
                errorSnackbar(message = getString(R.string.main_return_message_no_user), view = findViewById(android.R.id.content), length = com.google.android.material.snackbar.Snackbar.LENGTH_LONG)
            }

            RETURN_CODE_NO_ROOM -> {
                errorSnackbar(message = getString(R.string.main_return_message_no_room), view = findViewById(android.R.id.content), length = com.google.android.material.snackbar.Snackbar.LENGTH_LONG)
            }

            RETURN_CODE_NO_NETWORK_INTERFACE -> {
                errorSnackbar(message = getString(R.string.main_return_message_no_network_interface), view = findViewById(android.R.id.content), length = com.google.android.material.snackbar.Snackbar.LENGTH_LONG)
            }

            RETURN_CODE_LOST_CONNECTION -> {
                errorSnackbar(message = getString(R.string.main_return_message_lost_connection), view = findViewById(android.R.id.content))
            }

            RETURN_CODE_OTHER_EXIT -> {
                errorSnackbar(message = getString(R.string.main_return_message_other_exit), view = findViewById(android.R.id.content))
            }

            RETURN_CODE_OWN_EXIT -> {
                notifySnackbar(message = getString(R.string.main_return_message_own_exit), view = findViewById(android.R.id.content), length = com.google.android.material.snackbar.Snackbar.LENGTH_LONG)
            }

            RETURN_CODE_LOBBY_EXIT -> {
                notifySnackbar(message = getString(R.string.main_return_message_lobby_exit), view = findViewById(android.R.id.content), length = com.google.android.material.snackbar.Snackbar.LENGTH_LONG)
            }

            RETURN_CODE_INVALID_LOBBY -> {
                errorSnackbar(message = getString(R.string.main_return_message_invalid_lobby), view = findViewById(android.R.id.content))
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // init forestj logging
        initLogging()

        // default settings
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_activity)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        try {
            // settings toolbar
            val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar_main)
            toolbar.overflowIcon = ContextCompat.getDrawable(this, R.drawable.ic_hamburger_menu)
            setSupportActionBar(toolbar)
            supportActionBar?.setDisplayHomeAsUpEnabled(false) /* standard back/home button */
            supportActionBar?.title = getString(R.string.app_name)

            // deactivate standard back button
            onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    /* execute anything, e.g. finish() - if nothing is here, nothing happens pushing main back button */
                }
            })

            btn_find = findViewById(R.id.bt_findRoom)
            btn_find.setOnClickListener {
                showFindRoomDialog()
            }

            btn_createRoom = findViewById(R.id.bt_createRoom)
            btn_createRoom.setOnClickListener {
                showCreateRoomDialog()
            }

            // restart all settings of app
            //getSharedPreferences("${packageName}_preferences", Context.MODE_PRIVATE).edit(commit = true) { clear() }

            resetAll()
            initSettings()

//            Log.v(TAG, getAllWifiIpv4Addresses(this).joinToString(", "))
//
//            val cm : ConnectivityManager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
//            val networkCapabilities = cm.getNetworkCapabilities(cm.activeNetwork)
//            Log.e(TAG, "Network connection: " + networkCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET))
//            Log.e(TAG, "WiFi: " + networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI))
//            Log.e(TAG, "Mobile: " + networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR))
//
//            cm.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
//                override fun onAvailable(network : android.net.Network) {
//                    Log.e(TAG, "The default network is now: " + network)
//                }
//
//                override fun onLost(network : android.net.Network) {
//                    Log.e(TAG, "The application no longer has a default network. The last default network was " + network)
//                }
//
//                override fun onCapabilitiesChanged(network : android.net.Network, networkCapabilities : NetworkCapabilities) {
//                    Log.e(TAG, "The default network changed capabilities: " + networkCapabilities)
//                }
//
//                override fun onLinkPropertiesChanged(network : android.net.Network, linkProperties : android.net.LinkProperties) {
//                    Log.e(TAG, "The default network changed link properties: " + linkProperties)
//                }
//            })
        } catch (e: Exception) {
            errorSnackbar(message = e.message ?: "Exception in onCreate method.", view = findViewById(R.id.main_activity))
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                finishAffinity()
                exitProcess(0)
            }, 15000)
        }

        Log.v(TAG, "onCreate $TAG")
    }

    private fun initLogging() {
        net.forestany.forestj.lib.Global.get().resetLog()

        val o_loggingConfigAll = net.forestany.forestj.lib.LoggingConfig()
        o_loggingConfigAll.level = java.util.logging.Level.OFF
        //o_loggingConfigAll.level = java.util.logging.Level.SEVERE
        //o_loggingConfigAll.level = java.util.logging.Level.WARNING
        //o_loggingConfigAll.level = java.util.logging.Level.INFO
        //o_loggingConfigAll.level = java.util.logging.Level.CONFIG
        //o_loggingConfigAll.level = java.util.logging.Level.FINE
        //o_loggingConfigAll.level = java.util.logging.Level.FINER
        //o_loggingConfigAll.level = java.util.logging.Level.FINEST
        //o_loggingConfigAll.level = java.util.logging.Level.ALL
        o_loggingConfigAll.useConsole = false

        o_loggingConfigAll.consoleLevel = java.util.logging.Level.OFF
        //o_loggingConfigAll.consoleLevel = java.util.logging.Level.SEVERE
        //o_loggingConfigAll.consoleLevel = java.util.logging.Level.WARNING
        //o_loggingConfigAll.consoleLevel = java.util.logging.Level.INFO
        //o_loggingConfigAll.consoleLevel = java.util.logging.Level.CONFIG
        //o_loggingConfigAll.consoleLevel = java.util.logging.Level.FINE
        //o_loggingConfigAll.consoleLevel = java.util.logging.Level.FINER
        //o_loggingConfigAll.consoleLevel = java.util.logging.Level.FINEST
        //o_loggingConfigAll.consoleLevel = java.util.logging.Level.ALL

        //o_loggingConfigAll.useFile = true
        //o_loggingConfigAll.fileLevel = java.util.logging.Level.SEVERE
        //o_loggingConfigAll.filePath = "C:\\Users\\Public\\Documents\\"
        //o_loggingConfigAll.fileLimit = 1000000 // ~ 1.0 MB
        //o_loggingConfigAll.fileCount = 25
        o_loggingConfigAll.loadConfig()

        net.forestany.forestj.lib.Global.get().by_logControl = net.forestany.forestj.lib.Global.OFF

        //net.forestany.forestj.lib.Global.get().by_logControl = (net.forestany.forestj.lib.Global.SEVERE + net.forestany.forestj.lib.Global.WARNING + net.forestany.forestj.lib.Global.INFO).toByte()
        net.forestany.forestj.lib.Global.get().by_internalLogControl = net.forestany.forestj.lib.Global.OFF
        //net.forestany.forestj.lib.Global.get().by_internalLogControl = net.forestany.forestj.lib.Global.SEVERE
        //net.forestany.forestj.lib.Global.get().by_internalLogControl = (net.forestany.forestj.lib.Global.SEVERE + net.forestany.forestj.lib.Global.WARNING).toByte()
        //net.forestany.forestj.lib.Global.get().by_internalLogControl = (net.forestany.forestj.lib.Global.SEVERE + net.forestany.forestj.lib.Global.WARNING + net.forestany.forestj.lib.Global.INFO).toByte()
        //net.forestany.forestj.lib.Global.get().by_internalLogControl = (net.forestany.forestj.lib.Global.SEVERE + net.forestany.forestj.lib.Global.WARNING + net.forestany.forestj.lib.Global.INFO + net.forestany.forestj.lib.Global.CONFIG).toByte()
        //net.forestany.forestj.lib.Global.get().by_internalLogControl = (net.forestany.forestj.lib.Global.SEVERE + net.forestany.forestj.lib.Global.WARNING + net.forestany.forestj.lib.Global.INFO + net.forestany.forestj.lib.Global.CONFIG + net.forestany.forestj.lib.Global.FINE + net.forestany.forestj.lib.Global.FINER).toByte()
        //net.forestany.forestj.lib.Global.get().by_internalLogControl = (net.forestany.forestj.lib.Global.SEVERE + net.forestany.forestj.lib.Global.WARNING + net.forestany.forestj.lib.Global.INFO + net.forestany.forestj.lib.Global.CONFIG + net.forestany.forestj.lib.Global.FINE + net.forestany.forestj.lib.Global.FINER + net.forestany.forestj.lib.Global.FINEST).toByte()
        //net.forestany.forestj.lib.Global.get().by_internalLogControl = (net.forestany.forestj.lib.Global.SEVERE + net.forestany.forestj.lib.Global.WARNING + net.forestany.forestj.lib.Global.INFO + net.forestany.forestj.lib.Global.CONFIG + net.forestany.forestj.lib.Global.FINE + net.forestany.forestj.lib.Global.FINER + net.forestany.forestj.lib.Global.FINEST + net.forestany.forestj.lib.Global.MASS).toByte()
    }

    private fun getAllWifiIpv4Addresses(context: Context): List<String> {
        val ipv4Addresses = mutableListOf<String>()
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork
        val networkCapabilities = connectivityManager.getNetworkCapabilities(network)

        if (networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true) {
            val linkProperties = connectivityManager.getLinkProperties(network)
            linkProperties?.linkAddresses?.forEach { linkAddress ->
                if (linkAddress.address is java.net.Inet4Address) {
                    ipv4Addresses.add(linkAddress.address.hostAddress ?: "ip is null")
                }
            }
        }

        return ipv4Addresses
    }

    @SuppressLint("RestrictedApi")
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.toolbar_menu, menu)

        // allow showing icons on dropdown toolbar menu
        try {
            if (menu is androidx.appcompat.view.menu.MenuBuilder) {
                val menuBuilder: androidx.appcompat.view.menu.MenuBuilder = menu as androidx.appcompat.view.menu.MenuBuilder
                menuBuilder.setOptionalIconsVisible(true)
            }
            // does not run with release build, so the solution above is enough - @SuppressLint("RestrictedApi") needed
            //val method = menu?.javaClass?.getDeclaredMethod("setOptionalIconsVisible", Boolean::class.javaPrimitiveType)
            //method?.isAccessible = true
            //method?.invoke(menu, true)
        } catch (e: Exception) {
            errorSnackbar(message = e.message ?: "Exception in onCreateOptionsMenu method.", view = findViewById(android.R.id.content))
        }

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.mI_settings -> {
                val intent = Intent(this, SettingsActivity::class.java)
                launcher.launch(intent)

                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }

    private fun showAlertDialog(title: String, view: View?, positiveClickListener: View.OnClickListener) {
        AlertDialog.Builder(this, R.style.SelectionDialogStyle).setTitle(title).setView(view)
            .setNegativeButton(getString(R.string.text_cancel), null)
            .setPositiveButton(getString(R.string.text_ok)) { _, _ ->
                positiveClickListener.onClick(null)
            }.show()
    }

    private fun showCreateRoomDialog() {
        val dialogView = View.inflate(this, R.layout.dialog_create_or_join_chat, null)
        val eT_user = dialogView.findViewById<EditText>(R.id.eT_user)
        val eT_room = dialogView.findViewById<EditText>(R.id.eT_room)
        val sp_networkInterface = dialogView.findViewById<Spinner>(R.id.sp_networkInterface)
        eT_room.visibility = View.VISIBLE
        sp_networkInterface.visibility = View.VISIBLE

        var selectedNetworkInterface: String? = null

        sp_networkInterface.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                selectedNetworkInterface = parent.getItemAtPosition(position).toString()
                /* Log.i(TAG, "selected network interface: ${selectedNetworkInterface!!}") */
            }

            override fun onNothingSelected(parent: AdapterView<*>) {

            }
        }

        var a_networkInterfaces: List<String> = getAllWifiIpv4Addresses(this)

        if (a_networkInterfaces.isEmpty()) {
            a_networkInterfaces = a_networkInterfaces.plus(getString(R.string.chat_no_network_interfaces))
        }

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, a_networkInterfaces)
        adapter.setDropDownViewResource(android.R.layout.select_dialog_singlechoice)
        sp_networkInterface.adapter = adapter

        showAlertDialog(getString(R.string.main_create_room_title), dialogView) {
            if (selectedNetworkInterface == getString(R.string.chat_no_network_interfaces)) {
                errorSnackbar(message = getString(R.string.main_return_message_no_network_interface), view = findViewById(android.R.id.content))
                return@showAlertDialog
            } else if (eT_user.text.toString().isEmpty()) {
                errorSnackbar(message = getString(R.string.main_user_empty), view = findViewById(android.R.id.content))
                return@showAlertDialog
            } else if (eT_user.text.toString().length < 4) {
                errorSnackbar(message = getString(R.string.main_user_length_to_low, 4), view = findViewById(android.R.id.content))
                return@showAlertDialog
            } else if (eT_room.text.toString().isEmpty()) {
                errorSnackbar(message = getString(R.string.main_room_empty), view = findViewById(android.R.id.content))
                return@showAlertDialog
            } else if (eT_room.text.toString().length < 4) {
                errorSnackbar(message = getString(R.string.main_room_length_to_low, 4), view = findViewById(android.R.id.content))
                return@showAlertDialog
            }

            Log.i(TAG, "create room '" + eT_room.text + "' as '" + eT_user.text + "'")

            GlobalInstance.get().b_isServer = true

            assumeSharedPreferencesToGlobal(getSharedPreferences("${packageName}_preferences", Context.MODE_PRIVATE))

            val intent = Intent(this, ChatActivity::class.java)
            intent.putExtra("CHAT_ROOM", eT_room.text.toString())
            intent.putExtra("CHAT_USER", eT_user.text.toString())
            intent.putExtra("NETWORK_INTERFACE", "$selectedNetworkInterface:${GlobalInstance.get().getPreferences()["tcp_server_port"]}")
            GlobalInstance.get().s_user = eT_user.text.toString()

            // settings for emulator tests
            /*intent.putExtra("CHAT_ROOM", "Administration room")
            intent.putExtra("CHAT_USER", "Admin")
            intent.putExtra("NETWORK_INTERFACE", "0.0.0.0:11365")
            GlobalInstance.get().s_user = "Admin"*/

            launcher.launch(intent)
        }
    }

    private fun showFindRoomDialog() {
        val dialogView = View.inflate(this, R.layout.dialog_create_or_join_chat, null)
        val eT_user = dialogView.findViewById<EditText>(R.id.eT_user)
        val eT_room = dialogView.findViewById<EditText>(R.id.eT_room)
        val sp_networkInterface = dialogView.findViewById<Spinner>(R.id.sp_networkInterface)
        eT_room.visibility = View.GONE
        sp_networkInterface.visibility = View.GONE

        showAlertDialog(getString(R.string.main_find_room_title), dialogView) {
            if (eT_user.text.toString().isEmpty()) {
                errorSnackbar(message = getString(R.string.main_user_empty), view = findViewById(android.R.id.content))
                return@showAlertDialog
            } else if (eT_user.text.toString().length < 4) {
                errorSnackbar(message = getString(R.string.main_user_length_to_low, 4), view = findViewById(android.R.id.content))
                return@showAlertDialog
            }

            Log.i(TAG, "find room as '" + eT_user.text + "'")

            assumeSharedPreferencesToGlobal(getSharedPreferences("${packageName}_preferences", Context.MODE_PRIVATE))

            val intent = Intent(this, LobbyActivity::class.java)
            intent.putExtra("CHAT_USER", eT_user.text.toString())
            GlobalInstance.get().s_user = eT_user.text.toString()

            // settings for emulator tests
            /*val intent = Intent(this, ChatActivity::class.java)
            intent.putExtra("CHAT_ROOM", "Admin room")
            intent.putExtra("CHAT_USER", "User")
            intent.putExtra("NETWORK_INTERFACE", "10.0.2.2:12365")
            GlobalInstance.get().s_user = "User"*/

            launcher.launch(intent)
        }
    }

    private fun resetAll() {
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
        GlobalInstance.get().s_user = "NO_USER_SPECIFIED"
        GlobalInstance.get().clearChatMessageQueue()
        GlobalInstance.get().clearClientLobbyEntries()
        GlobalInstance.get().clearMessageBox()
    }

    private fun initSettings() {
        val sharedPreferences = getSharedPreferences("${packageName}_preferences", Context.MODE_PRIVATE)

        checkForAppUpdate(sharedPreferences)

        //sharedPreferences.all.forEach {
        //    Log.v(TAG, "${it.key} -> ${it.value}")
        //}

        if (
            (sharedPreferences.all.isEmpty()) ||
            (!sharedPreferences.contains("general_locale")) ||
            (!sharedPreferences.contains("udp_network_interface_name")) ||
            (!sharedPreferences.contains("udp_multicast_ip")) ||
            (!sharedPreferences.contains("udp_multicast_port")) ||
            (!sharedPreferences.contains("udp_multicast_ttl")) ||
            (!sharedPreferences.contains("tcp_server_port")) ||
            (!sharedPreferences.contains("tcp_common_passphrase")) ||
            (!sharedPreferences.contains("tcp_encryption"))
        ) {
            sharedPreferences.edit(commit = true) {
                if (!sharedPreferences.contains("general_locale")) {
                    val s_locale = java.util.Locale.getDefault().toString()

                    if ((s_locale.lowercase().startsWith("de")) || (s_locale.lowercase().startsWith("en"))) {
                        putString("general_locale", java.util.Locale.getDefault().toString().substring(0, 2))
                    } else {
                        putString("general_locale", "en")
                    }
                }

                if (!sharedPreferences.contains("udp_network_interface_name")) putString("udp_network_interface_name", SETTINGS_UDP_NETWORK_INTERFACE_NAME)
                if (!sharedPreferences.contains("udp_multicast_ip")) putString("udp_multicast_ip", SETTINGS_UDP_MULTICAST_IP)
                if (!sharedPreferences.contains("udp_multicast_port")) putString("udp_multicast_port", SETTINGS_UDP_MULTICAST_PORT)
                if (!sharedPreferences.contains("udp_multicast_ttl")) putString("udp_multicast_ttl", SETTINGS_UDP_MULTICAST_TTL)
                if (!sharedPreferences.contains("tcp_server_port")) putString("tcp_server_port", SETTINGS_TCP_SERVER_PORT)
                if (!sharedPreferences.contains("tcp_common_passphrase")) putString("tcp_common_passphrase", SETTINGS_TCP_COMMON_PASSPHRASE)
                if (!sharedPreferences.contains("tcp_encryption")) putBoolean("tcp_encryption", SETTINGS_TCP_ENCRYPTION.contentEquals("true"))
            }
        }

        assumeSharedPreferencesToGlobal(sharedPreferences)

        if (java.util.Locale.getDefault().toString().substring(0, 2) != sharedPreferences.all["general_locale"].toString()) {
            AppCompatDelegate.setApplicationLocales(
                LocaleListCompat.forLanguageTags(
                    sharedPreferences.all["general_locale"].toString()
                )
            )
        }
    }

    private fun assumeSharedPreferencesToGlobal(sharedPreferences: SharedPreferences) {
        GlobalInstance.get().clearPreferences()

        sharedPreferences.all.forEach {
            GlobalInstance.get().addPreference(it.key, it.value)
            //if (it.key!!.contentEquals("option_one")) GlobalInstance.get().optionOne = it.value.toString()
            //if (it.key!!.contentEquals("option_two")) GlobalInstance.get().optionTwo = it.value.toString()
            //if (it.key!!.contentEquals("option_three")) GlobalInstance.get().optionThree = it.value.toString()
        }
    }

    private fun getCurrentAppVersion(): String? {
        return try {
            packageManager.getPackageInfo(packageName, 0).versionName
        } catch (_: PackageManager.NameNotFoundException) {
            "unknown_version"
        }
    }

    private fun checkForAppUpdate(o_sharedPreferences: SharedPreferences) {
        val s_lastVersion: String = o_sharedPreferences.getString("last_version", "") ?: ""

        val s_currentVersion = getCurrentAppVersion()

        if (s_currentVersion.contentEquals("unknown_version")) {
            errorSnackbar(message = getString(R.string.main_app_unknown_version), view = findViewById(android.R.id.content))
        } else if (s_lastVersion.isEmpty()) {
            onFirstLaunchEver()
            o_sharedPreferences.edit { putString("last_version", s_currentVersion) }
        } else if (s_currentVersion != s_lastVersion) {
            onFirstLaunchAfterUpdate()
            o_sharedPreferences.edit { putString("last_version", s_currentVersion) }
        } else {
            Log.v(TAG, "app has not changed")
        }
    }

    private fun onFirstLaunchEver() {
        Log.v(TAG, "first launch ever")
    }

    private fun onFirstLaunchAfterUpdate() {
        Log.v(TAG, "first launch after update")
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

        assumeSharedPreferencesToGlobal(getSharedPreferences("${packageName}_preferences", Context.MODE_PRIVATE))

        Log.v(TAG, "onRestart $TAG")
    }

    override fun onDestroy() {
        super.onDestroy()

        resetAll()

        Log.v(TAG, "onDestroy $TAG")
    }
}