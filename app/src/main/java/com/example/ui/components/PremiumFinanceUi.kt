package com.example.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.model.DailySpendingPoint
import com.example.data.model.TransactionItem
import com.example.data.model.TransactionType
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen

private val DeepVioletInk = Color(0xFF2E1065)
private val LimeGlow = Color(0xFFC084FC)

fun premiumContrastForeground(backgroundColor: Color): Color {
  val luminance = backgroundColor.luminance()
  val whiteContrast = 1.05f / (luminance + 0.05f)
  val darkInk = Color(0xFF0B1410)
  val darkContrast = (luminance + 0.05f) / (darkInk.luminance() + 0.05f)
  return if (darkContrast >= whiteContrast) darkInk else Color.White
}

@Composable
fun PremiumSectionHeading(
  title: String,
  subtitle: String? = null,
  modifier: Modifier = Modifier,
  trailing: (@Composable () -> Unit)? = null
) {
  Row(
    modifier = modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Column(
      modifier = Modifier.weight(1f),
      verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
      Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground
      )
      if (!subtitle.isNullOrBlank()) {
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }
    if (trailing != null) {
      Spacer(Modifier.width(12.dp))
      trailing()
    }
  }
}

@Composable
fun PremiumPageHeader(
  title: String,
  subtitle: String? = null,
  trailing: (@Composable () -> Unit)? = null,
  modifier: Modifier = Modifier,
  stackBelowWidth: Dp = 460.dp
) {
  androidx.compose.foundation.layout.BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
    val titleBlock: @Composable () -> Unit = {
      Column(
        modifier = Modifier.fillMaxWidth().testTag("premium_page_header_title"),
        verticalArrangement = Arrangement.spacedBy(3.dp)
      ) {
        Text(
          text = title,
          style = MaterialTheme.typography.headlineSmall,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onBackground,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis
        )
        if (!subtitle.isNullOrBlank()) {
          Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
    }

    if (trailing == null) {
      titleBlock()
    } else if (maxWidth < stackBelowWidth) {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        titleBlock()
        trailing()
      }
    } else {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        Column(modifier = Modifier.weight(1f)) {
          titleBlock()
        }
        trailing()
      }
    }
  }
}

@Composable
fun PremiumCard(
  modifier: Modifier = Modifier,
  containerColor: Color = Color.Unspecified,
  content: @Composable () -> Unit
) {
  val isDark = com.example.ui.theme.isAppInDarkTheme()
  val resolvedContainerColor = if (containerColor != Color.Unspecified) {
    containerColor
  } else {
    com.example.ui.theme.resolveCardSurfaceColor()
  }

  Card(
    modifier = modifier.animateContentSize(),
    shape = RoundedCornerShape(24.dp),
    colors = CardDefaults.cardColors(containerColor = resolvedContainerColor),
    elevation = CardDefaults.cardElevation(defaultElevation = if (!isDark) 2.dp else 1.dp),
    border = androidx.compose.foundation.BorderStroke(
      1.dp,
      if (!isDark) Color(0xFFDDD6FE) else Color(0xFF8B5CF6).copy(alpha = 0.35f)
    )
  ) { content() }
}

@Composable
fun PremiumMonthNavigator(
  label: String,
  onPrevious: () -> Unit,
  onNext: () -> Unit,
  onPick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(18.dp),
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 1.dp,
    border = androidx.compose.foundation.BorderStroke(
      1.dp,
      MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
    )
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 5.dp, vertical = 5.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
      androidx.compose.material3.IconButton(onClick = onPrevious, modifier = Modifier.size(36.dp)) {
        Icon(Icons.Default.ChevronLeft, contentDescription = "Previous month", modifier = Modifier.size(20.dp))
      }
      Text(
        text = label,
        modifier = Modifier
          .clip(RoundedCornerShape(12.dp))
          .clickable(onClick = onPick)
          .padding(horizontal = 10.dp, vertical = 8.dp),
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Ellipsis,
        color = MaterialTheme.colorScheme.onSurface
      )
      androidx.compose.material3.IconButton(onClick = onNext, modifier = Modifier.size(36.dp)) {
        Icon(Icons.Default.ChevronRight, contentDescription = "Next month", modifier = Modifier.size(20.dp))
      }
    }
  }
}

@Composable
fun PremiumMetricTile(
  title: String,
  value: String,
  caption: String? = null,
  positive: Boolean? = null,
  modifier: Modifier = Modifier
) {
  PremiumCard(modifier = modifier) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
      Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.Medium
      )
      Text(
        text = value,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = when (positive) {
          true -> IncomeGreen
          false -> ExpenseRed
          null -> MaterialTheme.colorScheme.onSurface
        },
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Ellipsis
      )
      if (!caption.isNullOrBlank()) {
        Text(
          text = caption,
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }
    }
  }
}

@Composable
fun PremiumProgressTrack(
  progress: Float,
  color: Color,
  modifier: Modifier = Modifier,
  height: Dp = 7.dp,
  trackColor: Color = MaterialTheme.colorScheme.surfaceVariant
) {
  val animatedProgress by animateFloatAsState(
    targetValue = progress.coerceIn(0f, 1f),
    animationSpec = tween(durationMillis = 520, easing = FastOutSlowInEasing),
    label = "premium-progress"
  )
  Box(
    modifier = modifier
      .fillMaxWidth()
      .height(height)
      .clip(CircleShape)
      .background(trackColor)
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth(animatedProgress)
        .height(height)
        .clip(CircleShape)
        .background(color)
    )
  }
}

@Composable
fun PremiumTransactionRow(
  transaction: TransactionItem,
  currencySymbol: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  trailing: (@Composable () -> Unit)? = null
) {
  val isIncome = transaction.type == TransactionType.INCOME
  val moneyColor = if (isIncome) IncomeGreen else ExpenseRed
  val categoryColor = transaction.category.color

  Row(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(18.dp))
      .clickable(onClick = onClick)
      .padding(horizontal = 6.dp, vertical = 9.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    Surface(
      shape = RoundedCornerShape(16.dp),
      color = categoryColor.copy(alpha = 0.18f),
      border = androidx.compose.foundation.BorderStroke(1.dp, categoryColor.copy(alpha = 0.35f)),
      modifier = Modifier.size(48.dp)
    ) {
      Box(contentAlignment = Alignment.Center) {
        Icon(
          imageVector = transaction.category.icon,
          contentDescription = transaction.category.displayName,
          tint = categoryColor,
          modifier = Modifier.size(23.dp)
        )
      }
    }
    Column(
      modifier = Modifier.weight(1f),
      verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
      Text(
        text = transaction.title,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        Surface(
          shape = CircleShape,
          color = categoryColor.copy(alpha = 0.15f)
        ) {
          Text(
            text = transaction.category.displayName,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = categoryColor,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
          )
        }
        if (transaction.paymentMethod.displayName.isNotBlank()) {
          Text(
            text = "•  ${transaction.paymentMethod.displayName}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
      }
    }
    Column(
      horizontalAlignment = Alignment.End,
      verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
      Text(
        text = IndianCurrencyFormatter.formatWithSymbol(
          if (isIncome) transaction.amount else -transaction.amount,
          currencySymbol,
          includeSign = true
        ),
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Bold,
        color = moneyColor,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Ellipsis
      )
      Text(
        text = if (isIncome) "Income" else "Expense",
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Medium,
        color = moneyColor.copy(alpha = 0.85f)
      )
    }
    if (trailing != null) trailing()
  }
}

@Composable
fun PremiumEmptyState(
  title: String,
  body: String,
  modifier: Modifier = Modifier,
  action: (@Composable () -> Unit)? = null
) {
  Column(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 18.dp, vertical = 26.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(9.dp)
  ) {
    Surface(
      shape = CircleShape,
      color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f),
      modifier = Modifier.size(56.dp)
    ) {
      Box(contentAlignment = Alignment.Center) {
        Icon(
          Icons.Default.ReceiptLong,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(25.dp)
        )
      }
    }
    Text(
      text = title,
      style = MaterialTheme.typography.titleMedium,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.onSurface
    )
    Text(
      text = body,
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    if (action != null) action()
  }
}

@Composable
fun PremiumTrendChart(
  points: List<DailySpendingPoint>,
  modifier: Modifier = Modifier,
  lineColor: Color = LimeGlow,
  trackColor: Color = Color.White.copy(alpha = 0.15f),
  height: Dp = 84.dp
) {
  Canvas(modifier = modifier.fillMaxWidth().height(height)) {
    drawLine(
      color = trackColor,
      start = Offset(0f, size.height - 1f),
      end = Offset(size.width, size.height - 1f),
      strokeWidth = 1.dp.toPx()
    )
    val values = points.map { it.expense.toFloat().coerceAtLeast(0f) }
    if (values.size < 2 || values.maxOrNull() == 0f) return@Canvas
    val maxValue = values.maxOrNull() ?: 1f
    val stepX = size.width / (values.size - 1).coerceAtLeast(1)
    val verticalPadding = 6.dp.toPx()
    val usableHeight = size.height - verticalPadding * 2
    val coords = values.mapIndexed { index, value ->
      Offset(
        x = index * stepX,
        y = size.height - verticalPadding - (value / maxValue) * usableHeight
      )
    }
    val curve = Path().apply {
      moveTo(coords.first().x, coords.first().y)
      for (index in 1 until coords.size) {
        val previous = coords[index - 1]
        val current = coords[index]
        val dx = current.x - previous.x
        cubicTo(
          previous.x + dx * 0.42f,
          previous.y,
          previous.x + dx * 0.58f,
          current.y,
          current.x,
          current.y
        )
      }
    }
    drawPath(
      path = curve,
      color = lineColor,
      style = Stroke(width = 2.7.dp.toPx(), cap = StrokeCap.Round)
    )
    coords.lastOrNull()?.let {
      drawCircle(color = lineColor, radius = 4.dp.toPx(), center = it)
      drawCircle(color = DeepVioletInk, radius = 2.dp.toPx(), center = it)
    }
  }
}
