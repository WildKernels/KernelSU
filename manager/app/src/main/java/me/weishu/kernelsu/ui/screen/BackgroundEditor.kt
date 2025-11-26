package me.weishu.kernelsu.ui.screen

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Save
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ksuApp
import me.weishu.kernelsu.ui.util.BackgroundUtil
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.FloatingActionButton
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
@Destination<RootGraph>
fun BackgroundEditorScreen(navigator: DestinationsNavigator) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {
        imageUri = it
        scale = 1f
        offsetX = 0f
        offsetY = 0f
    }

    Scaffold(
        topBar = {
            TopAppBar(
                color = Color.Transparent,
                title = context.getString(R.string.background),
                actions = {
                    IconButton(onClick = { pickImage.launch("image/*") }) {
                        Icon(Icons.Rounded.Image, contentDescription = null)
                    }
                    IconButton(onClick = {
                        if (BackgroundUtil.clear()) navigator.navigateUp()
                    }) {
                        Icon(Icons.Rounded.Delete, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize()) {
            if (imageUri != null) {
                AsyncImage(
                    model = imageUri,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            translationX = offsetX
                            translationY = offsetY
                        }
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale = (scale * zoom).coerceIn(0.5f, 5f)
                                offsetX += pan.x
                                offsetY += pan.y
                            }
                        },
                    contentScale = ContentScale.Fit
                )
            } else if (BackgroundUtil.hasBackground()) {
                AsyncImage(
                    model = BackgroundUtil.uriForCoil(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(androidx.compose.ui.res.stringResource(id = R.string.background_pick_prompt))
                    IconButton(onClick = { pickImage.launch("image/*") }) {
                        Icon(Icons.Rounded.Image, contentDescription = null)
                    }
                }
            }

            FloatingActionButton(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                onClick = {
                    val uri = imageUri ?: return@FloatingActionButton
                    scope.launch {
                        val success = withContext(Dispatchers.IO) {
                            val src = ksuApp.contentResolver.openInputStream(uri)?.use { input ->
                                BitmapFactory.decodeStream(input)
                            }
                            if (src == null) return@withContext false

                            val w = context.resources.displayMetrics.widthPixels
                            val h = context.resources.displayMetrics.heightPixels
                            val m = Matrix()
                            val base = run {
                                val sx = w.toFloat() / src.width
                                val sy = h.toFloat() / src.height
                                maxOf(sx, sy)
                            }
                            val finalScale = base * scale
                            m.postScale(finalScale, finalScale)
                            m.postTranslate(offsetX, offsetY)

                            val result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                            val canvas = android.graphics.Canvas(result)
                            canvas.drawBitmap(src, m, null)
                            BackgroundUtil.save(result)
                        }
                        if (success) navigator.navigateUp()
                    }
                }
            ) {
                Icon(Icons.Rounded.Save, contentDescription = null)
            }
        }
    }
}
