package y2k.joyreactor

import android.app.Application
import android.os.Handler
import android.os.StrictMode
import com.splunk.mint.Mint
import y2k.joyreactor.common.AndroidPlatform
import y2k.joyreactor.common.ServiceLocator
import y2k.joyreactor.common.executeOnUi
import y2k.joyreactor.common.platform.Platform

/**
 * Created by y2k on 9/26/15.
 */
class App : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this

        if (BuildConfig.DEBUG) {
            StrictMode.enableDefaults()
        } else {
            Mint.disableNetworkMonitoring()
            Mint.initAndStartSession(this, "66d8751e")
        }

        val handler = Handler()
        executeOnUi = { handler.post(it) }
        ServiceLocator.registerSingleton<Platform> { AndroidPlatform(this) }
    }

    companion object {

        lateinit var instance: App
    }
}