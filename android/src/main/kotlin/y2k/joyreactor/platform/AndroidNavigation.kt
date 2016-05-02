package y2k.joyreactor.platform

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v4.app.FragmentManager
import android.support.v7.app.AppCompatActivity
import y2k.joyreactor.*
import y2k.joyreactor.common.ActivityLifecycleCallbacksAdapter
import y2k.joyreactor.common.startActivity
import y2k.joyreactor.viewmodel.*
import kotlin.reflect.KClass

/**
 * Created by y2k on 10/19/15.
 */
class AndroidNavigation(app: Application) : NavigationService {

    private val fragmentManager: FragmentManager?
        get() = (currentActivity as AppCompatActivity?)?.supportFragmentManager

    internal var currentActivity: Activity? = null

    init {
        app.registerActivityLifecycleCallbacks(MyActivityLifecycleCallbacks())
    }

    override fun close() {
        val dialog = fragmentManager?.findFragmentByTag("dialog") ?: return
        (dialog as DialogFragment).dismiss()
    }

    override fun <T : Any> open(vmType: KClass<T>, argument: String) {
        sArgument = argument
        when (vmType) {
            MessagesViewModel::class -> currentActivity?.startActivity(MessagesActivity::class)
            PostLikeViewModel::class -> PostLikeFragment().show(fragmentManager, "dialog")
            PostViewModel::class -> currentActivity?.startActivity(PostActivity::class)
            CreateCommentViewModel::class -> currentActivity?.startActivity(CreateCommentActivity::class)
            GalleryViewModel::class -> currentActivity?.startActivity(GalleryActivity::class)
            VideoViewModel::class -> currentActivity?.startActivity(VideoActivity::class)
            ImageViewModel::class -> currentActivity?.startActivity(ImageActivity::class)
            else -> throw Exception("Can't handler navigation to $vmType")
        }
    }

    override fun switchProfileToLogin() {
        currentActivity?.startActivity(LoginActivity::class)
        currentActivity?.finish()
    }

    override fun switchLoginToProfile() {
        currentActivity?.startActivity(ProfileActivity::class)
        currentActivity?.finish()
    }

    override fun closeCreateComment() {
        currentActivity!!.finish()
    }

    override val argument: String
        get() = sArgument

    override fun openBrowser(url: String) {
        currentActivity!!.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    private inner class MyActivityLifecycleCallbacks : ActivityLifecycleCallbacksAdapter() {

        override fun onActivityCreated(activity: Activity?, savedInstanceState: Bundle?) {
            currentActivity = activity
        }

        override fun onActivityResumed(activity: Activity?) {
            currentActivity = activity
        }

        override fun onActivityPaused(activity: Activity?) {
            if (currentActivity == activity) currentActivity = null
        }
    }

    companion object {

        // FIXME: перенести на уровень платформы
        internal var sArgument = "2294127"
    }
}