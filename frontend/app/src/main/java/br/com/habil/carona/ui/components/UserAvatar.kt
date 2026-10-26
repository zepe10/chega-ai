package br.com.habil.carona.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import br.com.habil.carona.ui.util.initials
import coil.compose.AsyncImage
import java.io.File

@Composable
fun UserAvatar(
    name: String,
    photoPath: String,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
) {
    Surface(
        modifier = modifier.size(size),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
    ) {
        if (photoPath.isNotBlank()) {
            AsyncImage(
                model = File(photoPath),
                contentDescription = "Foto de perfil de $name",
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(size),
            )
        } else {
            Box(contentAlignment = Alignment.Center) {
                Text(name.initials(), style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}
