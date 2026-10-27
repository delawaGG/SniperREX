package com.sniperrex.app.presentation.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sniperrex.app.R
import com.sniperrex.app.presentation.theme.CyberLime

/**
 * Logo dimuat langsung sebagai bitmap dari resource PNG, bukan lewat
 * painterResource. Jalur ini tidak menyentuh VectorDrawable / adaptive-icon
 * XML, jadi tidak bisa melempar IllegalArgumentException di perangkat mana pun.
 */
@Composable
fun rememberLogoBitmap(): ImageBitmap? {
    val context = LocalContext.current
    return remember {
        runCatching {
            BitmapFactory.decodeResource(context.resources, R.drawable.logo_sniperrex)
                ?.asImageBitmap()
        }.getOrNull()
    }
}

/** Logo aplikasi; jatuh ke monogram "SR" kalau decode bitmap gagal. */
@Composable
fun LogoMark(modifier: Modifier = Modifier) {
    val bitmap = rememberLogoBitmap()
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = null,
            modifier = modifier,
            contentScale = ContentScale.Crop,
        )
    } else {
        Box(modifier = modifier.size(34.dp), contentAlignment = Alignment.Center) {
            Text(
                text = "SR",
                color = CyberLime,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
            )
        }
    }
}
