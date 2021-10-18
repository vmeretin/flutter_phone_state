package co.sunnyapp.flutter_phone_state

import android.content.Context
import android.os.Binder
import android.os.Build
import android.telephony.PhoneStateListener
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import androidx.annotation.NonNull
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.EventChannel

class FlutterPhoneStatePlugin : FlutterPlugin, EventChannel.StreamHandler {
    private val mainThreadExecutor = MainThreadExecutor()

    private var applicationContext: Context? = null
    private var eventChannel: EventChannel? = null

    private var telephonyManager: TelephonyManager? = null
    private var phoneStateListener: PhoneStateListener? = null
    private var telephonyCallback: TelephonyCallback? = null

    override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        val applicationContext = flutterPluginBinding.applicationContext
        this.applicationContext = applicationContext

        val eventChannel = EventChannel(flutterPluginBinding.binaryMessenger, "co.sunnyapp/phone_events")
        eventChannel.setStreamHandler(this)
        this.eventChannel = eventChannel

        telephonyManager = applicationContext.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
    }

    override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
        applicationContext = null
        eventChannel?.setStreamHandler(null)
        eventChannel = null
        telephonyManager = null
    }

    override fun onListen(arguments: Any?, eventSink: EventChannel.EventSink?) {
        if (eventSink == null) return

        val telephonyManager = this.telephonyManager ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val telephonyCallback = object : TelephonyCallback(), TelephonyCallback.CallStateListener {
                override fun onCallStateChanged(state: Int) {
                    handleCallStateChanged(eventSink, state, null)
                }
            }

            telephonyManager.registerTelephonyCallback(mainThreadExecutor, telephonyCallback)
            this.telephonyCallback = telephonyCallback
        } else {
            val phoneStateListener = object : PhoneStateListener() {
                override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                    handleCallStateChanged(eventSink, state, phoneNumber)
                }
            }

            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE)
            this.phoneStateListener = phoneStateListener
        }
    }

    override fun onCancel(arguments: Any?) {
        val telephonyManager = this.telephonyManager ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            telephonyCallback?.let {
                telephonyManager.unregisterTelephonyCallback(it)
            }
        } else {
            phoneStateListener?.let {
                telephonyManager.listen(it, PhoneStateListener.LISTEN_NONE)
                this.phoneStateListener = null
            }
        }
    }

    private fun handleCallStateChanged(eventSink: EventChannel.EventSink, state: Int, phoneNumber: String?) {
        when (state) {
            /** Device call state: No activity.  */
            /** Device call state: No activity.  */
            TelephonyManager.CALL_STATE_IDLE -> {
                eventSink.success(PhoneCallEvent(phoneNumber, PhoneEventType.disconnected).toMap())
            }
            /** Device call state: Off-hook. At least one call exists
             * that is dialing, active, or on hold, and no calls are ringing
             * or waiting. */
            /** Device call state: Off-hook. At least one call exists
             * that is dialing, active, or on hold, and no calls are ringing
             * or waiting. */
            TelephonyManager.CALL_STATE_OFFHOOK -> {
                eventSink.success(PhoneCallEvent(phoneNumber, PhoneEventType.connected).toMap())
            }
            /** Device call state: Ringing. A new call arrived and is
             * ringing or waiting. In the latter case, another call is
             * already active.  */
            /** Device call state: Ringing. A new call arrived and is
             * ringing or waiting. In the latter case, another call is
             * already active.  */
            TelephonyManager.CALL_STATE_RINGING -> {
                eventSink.success(PhoneCallEvent(phoneNumber, PhoneEventType.inbound).toMap())
            }
        }
    }
}
