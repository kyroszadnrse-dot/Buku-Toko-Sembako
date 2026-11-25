package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Coffee
import androidx.compose.material.icons.outlined.Egg
import androidx.compose.material.icons.outlined.Fastfood
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.LocalDrink
import androidx.compose.material.icons.outlined.OilBarrel
import androidx.compose.material.icons.outlined.RamenDining
import androidx.compose.material.icons.outlined.RiceBowl
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.ui.theme.NumericTextStyle
import java.io.File

fun categoryIcon(category: String): ImageVector =
  when (category) {
    "Beras" -> Icons.Outlined.RiceBowl
    "Minyak" -> Icons.Outlined.OilBarrel
    "Gula" -> Icons.Outlined.ShoppingBag
    "Telur" -> Icons.Outlined.Egg
    "Mi Instan" -> Icons.Outlined.RamenDining
    "Kopi" -> Icons.Outlined.Coffee
    "Minuman" -> Icons.Outlined.LocalDrink
    "Makanan Ringan" -> Icons.Outlined.Fastfood
    "Kebutuhan Rumah Tangga" -> Icons.Outlined.Home
    else -> Icons.Outlined.Inventory2
  }

@Composable
fun ProductThumbnail(
  photoPath: String?,
  category: String,
  contentDescription: String,
  size: Dp = 56.dp,
  modifier: Modifier = Modifier,
) {
  val file = photoPath?.takeIf { it.isNotBlank() }?.let { File(it) }?.takeIf { it.exists() }
  Box(
    modifier =
      modifier
        .size(size)
        .clip(RoundedCornerShape(14.dp))
        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)),
    contentAlignment = Alignment.Center,
  ) {
    if (file != null) {
      AsyncImage(
        model = file,
        contentDescription = contentDescription,
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize(),
      )
    } else {
      Icon(
        imageVector = categoryIcon(category),
        contentDescription = contentDescription,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(size * 0.48f),
      )
    }
  }
}

@Composable
fun StatusBadge(
  text: String,
  containerColor: Color,
  contentColor: Color,
  modifier: Modifier = Modifier,
) {
  Surface(
    color = containerColor,
    shape = RoundedCornerShape(50),
    modifier = modifier,
  ) {
    Text(
      text = text,
      style = MaterialTheme.typography.labelSmall,
      fontWeight = FontWeight.SemiBold,
      color = contentColor,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
    )
  }
}

@Composable
fun SearchInputBar(
  query: String,
  onQueryChange: (String) -> Unit,
  placeholder: String,
  modifier: Modifier = Modifier,
) {
  OutlinedTextField(
    value = query,
    onValueChange = onQueryChange,
    singleLine = true,
    placeholder = { Text(placeholder) },
    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
    trailingIcon = {
      if (query.isNotEmpty()) {
        IconButton(onClick = { onQueryChange("") }) {
          Icon(Icons.Rounded.Close, contentDescription = "Hapus pencarian")
        }
      }
    },
    shape = RoundedCornerShape(14.dp),
    modifier = modifier.fillMaxWidth(),
  )
}

@Composable
fun EmptyStateCard(
  title: String,
  description: String,
  actionLabel: String? = null,
  onAction: (() -> Unit)? = null,
  modifier: Modifier = Modifier,
) {
  Card(
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    shape = MaterialTheme.shapes.large,
    modifier = modifier.fillMaxWidth(),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Image(
        painter = painterResource(id = R.drawable.img_empty_basket),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier.size(120.dp).clip(RoundedCornerShape(20.dp)),
      )
      Spacer(modifier = Modifier.height(16.dp))
      Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
      )
      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text = description,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
      )
      if (actionLabel != null && onAction != null) {
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onAction, shape = MaterialTheme.shapes.medium) {
          Text(actionLabel)
        }
      }
    }
  }
}

@Composable
fun RupiahNumberField(
  value: String,
  onValueChange: (String) -> Unit,
  label: String,
  modifier: Modifier = Modifier,
  supportingText: String? = null,
) {
  OutlinedTextField(
    value = value,
    onValueChange = { raw -> onValueChange(raw.filter { it.isDigit() }.take(12)) },
    label = { Text(label) },
    prefix = { Text("Rp", fontWeight = FontWeight.SemiBold) },
    singleLine = true,
    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
    supportingText = supportingText?.let { { Text(it) } },
    shape = RoundedCornerShape(12.dp),
    modifier = modifier.fillMaxWidth(),
  )
}

@Composable
fun MetricRowLine(
  label: String,
  valueText: String,
  valueColor: Color = MaterialTheme.colorScheme.onSurface,
  bold: Boolean = false,
) {
  Row(
    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      text = label,
      style = if (bold) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium,
      color = if (bold) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.weight(1f),
    )
    Spacer(modifier = Modifier.width(8.dp))
    Text(
      text = valueText,
      style =
        NumericTextStyle.copy(
          fontSize = if (bold) MaterialTheme.typography.titleMedium.fontSize else MaterialTheme.typography.bodyMedium.fontSize,
          fontWeight = if (bold) FontWeight.Bold else FontWeight.SemiBold,
        ),
      color = valueColor,
    )
  }
}
