package dev.dmanluc.openbankmobiletest.utils

import android.graphics.drawable.Drawable
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.viewbinding.ViewBinding
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.signature.ObjectKey
import com.google.android.material.snackbar.Snackbar
import dev.dmanluc.openbankmobiletest.core.GlideApp
import java.security.MessageDigest
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

fun String.fromUTCTimeToDate(): Date? {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())

    return try {
        dateFormat.parse(this)
    } catch (parseException: ParseException) {
        null
    }
}

fun String.hashMd5(): String {
    val hexCharacters = "0123456789abcdef"
    val digest = MessageDigest.getInstance("MD5").digest(this.toByteArray())
    return digest.joinToString(separator = "",
        transform = { a ->
            String(
                charArrayOf(
                    hexCharacters[a.toInt() shr 4 and 0x0f],
                    hexCharacters[a.toInt() and 0x0f]
                )
            )
        })
}

fun Boolean?.orFalse(): Boolean = this ?: false

fun Fragment.showSnackbar(snackbarText: String, timeLength: Int) {
    activity?.let {
        Snackbar.make(it.findViewById(android.R.id.content), snackbarText, timeLength).show()
    }
}

fun Fragment.setupSnackbarWithStringResId(
    lifecycleOwner: LifecycleOwner,
    snackbarEvent: LiveData<Event<Int>>,
    timeLength: Int
) {
    snackbarEvent.observe(lifecycleOwner, { event ->
        event.getContentIfNotHandled()?.let { res ->
            context?.let { showSnackbar(it.getString(res), timeLength) }
        }
    })
}

fun Fragment.setupSnackbarWithStringLiteral(
    lifecycleOwner: LifecycleOwner,
    snackbarEvent: LiveData<Event<String>>,
    timeLength: Int
) {
    snackbarEvent.observe(lifecycleOwner, { event ->
        event.getContentIfNotHandled()?.let { literal ->
            context?.let { showSnackbar(literal, timeLength) }
        }
    })
}

inline fun <T : ViewBinding> AppCompatActivity.viewBinding(
    crossinline bindingInflater: (LayoutInflater) -> T
) =
    lazy(LazyThreadSafetyMode.NONE) {
        bindingInflater.invoke(layoutInflater)
    }

fun <T : ViewBinding> Fragment.viewBinding(viewBindingFactory: (View) -> T) =
    FragmentViewBindingDelegate(this, viewBindingFactory)

fun ImageView.loadImage(
    path: String,
    errorResource: Int,
    onExceptionDelegate: () -> Unit = {},
    onResourceReadyDelegate: () -> Unit = {},
    daysWhileValidCache: Int = 1
) {

    GlideApp.with(this.context).load(Uri.parse(path)).listener(object : RequestListener<Drawable> {
        override fun onLoadFailed(
            e: GlideException?,
            model: Any?,
            target: Target<Drawable>?,
            isFirstResource: Boolean
        ): Boolean {
            onExceptionDelegate()
            return false
        }

        override fun onResourceReady(
            resource: Drawable?,
            model: Any?,
            target: Target<Drawable>?,
            dataSource: com.bumptech.glide.load.DataSource?,
            isFirstResource: Boolean
        ): Boolean {
            onResourceReadyDelegate()
            return false
        }
    })
        .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
        .signature(ObjectKey(with(System.currentTimeMillis()) {
            if (daysWhileValidCache > 0) {
                (this / (daysWhileValidCache * 24 * 60 * 60 * 1000)).toString()
            } else {
                this.toString()
            }
        }))
        .error(errorResource).transition(DrawableTransitionOptions().crossFade()).into(this)
}

fun String?.enforceHttps(): String? =
    if (this != null && !this.contains("https"))
        this.replace("http", "https")
    else this