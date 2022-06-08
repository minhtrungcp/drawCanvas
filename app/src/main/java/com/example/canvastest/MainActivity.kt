package com.example.canvastest

import android.content.Context
import android.content.res.Resources
import android.graphics.*
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color.Companion.Gray
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.canvastest.ui.theme.CanvasTestTheme
import jp.co.cyberagent.android.gpuimage.GPUImage
import jp.co.cyberagent.android.gpuimage.filter.*
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CanvasTestTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    InImagine()
                }
            }
        }
    }
}

@Composable
fun InImagine() {
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var squares by remember { mutableStateOf(2) }
    val context = LocalContext.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        bitmap?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = "",
                Modifier.background(color = Gray)
            )
        }
        Row {
            Button(onClick = {
                squares = 2
                bitmap = drawSquares(context, squares)
            }, Modifier.padding(15.dp)) {
                Text(text = "2x2")
            }
            Button(onClick = {
                squares = 3
                bitmap = drawSquares(context, squares)
            }, Modifier.padding(15.dp)) {
                Text(text = "3x3")
            }
            Button(onClick = {
                bitmap = drawSquares(context, squares, true)
            }, Modifier.padding(15.dp)) {
                Text(text = "Load")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    CanvasTestTheme {
        InImagine()
    }
}

fun drawSquares(context: Context, size: Int = 2, loadImage: Boolean = false): Bitmap {
    val canvasSize = 400
    val margin = 50

    val bitmap = Bitmap.createBitmap(canvasSize, canvasSize, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val squareSize = (canvasSize - ((size + 1) * margin)) / size
    for (x in 0 until size) {
        for (y in 0 until size) {
            canvas.save()
            val totalMarginSizeX = margin * (x + 1)
            val totalMarginSizeY = margin * (y + 1)
            val top = totalMarginSizeY.toFloat() + (squareSize * y)
            val left = totalMarginSizeX.toFloat() + (squareSize * x)
            val origin = 0f
            canvas.translate(left, top)
            canvas.drawRect(
                origin,
                origin,
                squareSize.toFloat(),
                squareSize.toFloat(),
                Paint().apply {
                    style = Paint.Style.FILL
                    color = Color.WHITE
                }
            )
            canvas.drawRect(
                origin,
                origin,
                squareSize.toFloat(),
                squareSize.toFloat(),
                Paint().apply {
                    style = Paint.Style.STROKE
                    color = Color.BLACK
                }
            )
            if (loadImage && (x == 0 && y == 0)) {
                canvas.drawBitmap(
                    loadImage(
                        context,
                        squareSize,
                        listOf(
                            GPUImageAddBlendFilter(),
                            GPUImageBrightnessFilter()
                        )
                    ),
                    origin,
                    origin,
                    Paint().apply {
                        isAntiAlias = true
                    })
            }
            canvas.restore()
        }
    }
    return bitmap
}

fun applyFilter(context: Context, bitmap: Bitmap, list: List<GPUImageFilter>): Bitmap {
    if (list.isEmpty()) return bitmap
    val gpuImage = GPUImage(context)
    gpuImage.setFilter(GPUImageFilterGroup(list))
    gpuImage.setImage(bitmap)
    return gpuImage.bitmapWithFilterApplied
}

fun loadImage(context: Context, size: Int, list: List<GPUImageFilter>): Bitmap {
    val image = BitmapFactory.Options().run {
        inSampleSize = size
        inJustDecodeBounds = false
        BitmapFactory.decodeResource(context.resources, R.drawable.flowers, this)
    }
    val scale: Float =
        if (image.width < image.height) {
            size / image.width.toFloat()
        } else {
            size / image.height.toFloat()
        }

    val scaledWidth = (image.width * scale).roundToInt()
    val scaledHeight = (image.height * scale).roundToInt()

    val scaled = Bitmap.createScaledBitmap(image, scaledWidth, scaledHeight, true)
    val croppedX = (scaledWidth - size) / 2
    val croppedY = (scaledHeight - size) / 2
    val cropped = Bitmap.createBitmap(scaled, croppedX, croppedY, size, size)
    return applyFilter(context, cropped, list)
}