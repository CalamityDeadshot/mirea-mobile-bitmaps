package ru.mirea.bitmapsidk

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.widget.ImageView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Slider
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.mirea.bitmapsidk.ui.theme.BitmapsIdkTheme
import java.lang.RuntimeException
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {

            var jackalizationPower by remember {
                mutableStateOf(8)
            }

            BitmapsIdkTheme {
                Surface(
                    color = MaterialTheme.colors.background
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Image(
                            imageResource = R.drawable.pic_jackal,
                            downscaleBy = if (jackalizationPower >= 2) jackalizationPower else null
                        )
                        Text(text = "Степень шакализации")
                        Slider(
                            value = jackalizationPower.toFloat(),
                            onValueChange = {
                                jackalizationPower = it.roundToInt()
                            },
                            valueRange = 0f..64f
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun Image(
    @DrawableRes imageResource: Int,
    downscaleBy: Int? = null,
    requiredHeight: Int? = null,
    requiredWidth: Int? = null,
) {
    val resources = LocalContext.current.resources

    val coroutineScope = rememberCoroutineScope()

    AndroidView(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp),
        factory = {
            ImageView(it).apply {
                scaleType = ImageView.ScaleType.FIT_CENTER
            }
        },
        update = { imageView ->
            decodeOptimized(
                resources = resources,
                imageResource = imageResource,
                scope = coroutineScope,
                downscalingFactor = downscaleBy,
                displayHeight = 50,
                displayWidth = 50
            ) {
                try {
                    imageView.setImageBitmap(it)
                } catch (e: RuntimeException) {

                }
                Log.v("Bitmaps", "${it.width}x${it.height}")
            }
        }
    )
}

fun decodeOptimized(
    resources: Resources,
    @DrawableRes imageResource: Int,
    scope: CoroutineScope,
    downscalingFactor: Int? = null,
    displayHeight: Int,
    displayWidth: Int,
    onDecoded: (Bitmap) -> Unit
) {
    scope.launch(Dispatchers.Default) {
        if (downscalingFactor != null) {
            withContext(Dispatchers.Main) {
                onDecoded(
                    BitmapFactory.decodeResource(
                        resources,
                        imageResource,
                        BitmapFactory.Options().apply {
                            this.inSampleSize = downscalingFactor
                        }
                    )
                )
            }
            return@launch
        }
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeResource(resources, imageResource, options)
        Log.v("Bitmaps", "${options.outWidth}x${options.outHeight}")

        val width = options.outWidth
        val height = options.outHeight

        var inSampleSize = 1

        if (height > displayHeight || width > displayWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2

            while (
                (height / inSampleSize) > displayHeight
                && (width / inSampleSize) > displayWidth
            ) {
                inSampleSize *= 2
                Log.v("Bitmaps", "(halfHeight / inSampleSize) = ${(halfHeight / inSampleSize)}")
                Log.v("Bitmaps", "(halfWidth / inSampleSize) = ${(halfWidth / inSampleSize)}")
            }
        }
        Log.v("Bitmaps", "Downscaling by $inSampleSize")
        options.apply {
            this.inSampleSize = inSampleSize
            inJustDecodeBounds = false
        }
        val bitmap = BitmapFactory.decodeResource(resources, imageResource, options)

        withContext(Dispatchers.Main) {
            onDecoded(bitmap)
        }
    }
}