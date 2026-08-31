package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CategoryAnalytics
import com.example.data.model.DailySpendingTrend
import com.example.data.model.DayOfWeekBreakdown
import com.example.ui.theme.AccentIndigo
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate700
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoryDonutChart(
  categories: List<CategoryAnalytics>,
  totalExpense: Double,
  currencySymbol: String,
  modifier: Modifier = Modifier
) {
  var selectedCategory by remember { mutableStateOf<CategoryAnalytics?>(null) }
  val animationProgress = remember { Animatable(0f) }

  LaunchedEffect(categories) {
    animationProgress.snapTo(0f)
    animationProgress.animateTo(
      targetValue = 1f,
      animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
    )
  }

  Column(
    modifier = modifier.fillMaxWidth(),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    if (categories.isEmpty() || totalExpense <= 0) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(180.dp),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = "No expense drafts in this period",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    } else {
      // Donut canvas with center text
      Box(
        modifier = Modifier
          .size(220.dp)
          .padding(8.dp),
        contentAlignment = Alignment.Center
      ) {
        Canvas(
          modifier = Modifier
            .size(200.dp)
            .pointerInput(categories) {
              detectTapGestures { tapOffset ->
                val center = Offset(size.width / 2f, size.height / 2f)
                val dx = tapOffset.x - center.x
                val dy = tapOffset.y - center.y
                val distance = sqrt(dx * dx + dy * dy)
                val outerRadius = size.width / 2f
                val innerRadius = outerRadius - 38.dp.toPx()

                if (distance in innerRadius..outerRadius) {
                  var touchAngle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                  if (touchAngle < 0) touchAngle += 360f
                  touchAngle = (touchAngle + 90f) % 360f // Adjust for -90 start angle

                  var currentSweep = 0f
                  for (cat in categories) {
                    val sweep = cat.percentage * 360f
                    if (touchAngle in currentSweep..(currentSweep + sweep)) {
                      selectedCategory = if (selectedCategory == cat) null else cat
                      break
                    }
                    currentSweep += sweep
                  }
                } else {
                  selectedCategory = null
                }
              }
            }
        ) {
          val strokeWidth = 32.dp.toPx()
          val radius = (size.minDimension - strokeWidth) / 2f
          val topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)
          val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)

          var startAngle = -90f
          val totalAnimatedSweep = 360f * animationProgress.value

          categories.forEach { cat ->
            val sweepAngle = cat.percentage * totalAnimatedSweep
            val isSelected = selectedCategory == cat
            val arcStroke = if (isSelected) strokeWidth + 6.dp.toPx() else strokeWidth

            drawArc(
              color = if (selectedCategory == null || isSelected) cat.color else cat.color.copy(alpha = 0.35f),
              startAngle = startAngle,
              sweepAngle = sweepAngle.coerceAtLeast(1.5f),
              useCenter = false,
              topLeft = topLeft,
              size = arcSize,
              style = Stroke(width = arcStroke, cap = StrokeCap.Butt)
            )

            startAngle += sweepAngle
          }
        }

        // Center content
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center,
          modifier = Modifier.padding(16.dp)
        ) {
          if (selectedCategory != null) {
            Text(
              text = selectedCategory!!.category.displayName,
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
              color = selectedCategory!!.color,
              maxLines = 1
            )
            Text(
              text = formatCurrency(selectedCategory!!.totalAmount, currencySymbol),
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              text = "${String.format("%.1f", selectedCategory!!.percentage * 100)}%",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          } else {
            Text(
              text = "Total Expenses",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
              text = formatCurrency(totalExpense, currencySymbol),
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              text = "${categories.size} categories",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Category interactive legend chips
      FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        categories.take(6).forEach { cat ->
          val isSelected = selectedCategory == cat
          Surface(
            shape = RoundedCornerShape(20.dp),
            color = if (isSelected) cat.color.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            border = androidx.compose.foundation.BorderStroke(
              1.dp,
              if (isSelected) cat.color else Color.Transparent
            ),
            modifier = Modifier
              .clip(RoundedCornerShape(20.dp))
              .clickable {
                selectedCategory = if (selectedCategory == cat) null else cat
              }
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Box(
                modifier = Modifier
                  .size(8.dp)
                  .clip(CircleShape)
                  .background(cat.color)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = cat.category.displayName,
                style = MaterialTheme.typography.labelSmall.copy(
                  fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                  fontSize = 11.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = "${String.format("%.0f", cat.percentage * 100)}%",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontWeight = FontWeight.Bold,
                  fontSize = 11.sp
                ),
                color = cat.color
              )
            }
          }
        }
      }
    }
  }
}

@Composable
fun DailySpendingBarChart(
  trends: List<DailySpendingTrend>,
  currencySymbol: String,
  modifier: Modifier = Modifier
) {
  var selectedTrend by remember { mutableStateOf<DailySpendingTrend?>(null) }
  val animationProgress = remember { Animatable(0f) }

  LaunchedEffect(trends) {
    animationProgress.snapTo(0f)
    animationProgress.animateTo(
      targetValue = 1f,
      animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing)
    )
  }

  val maxVal = remember(trends) {
    trends.maxOfOrNull { maxOf(it.expenseAmount, it.incomeAmount) }?.coerceAtLeast(50.0) ?: 100.0
  }

  Column(modifier = modifier.fillMaxWidth()) {
    // Top tooltip / selected info banner
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(bottom = 8.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      if (selectedTrend != null) {
        Text(
          text = "${selectedTrend!!.dayLabel}: Exp -${formatCurrency(selectedTrend!!.expenseAmount, currencySymbol)}" +
              if (selectedTrend!!.incomeAmount > 0) " • Inc +${formatCurrency(selectedTrend!!.incomeAmount, currencySymbol)}" else "",
          style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
          color = MaterialTheme.colorScheme.primary
        )
      } else {
        Text(
          text = "Daily Draft Activity",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(ExpenseRed))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Expense", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(IncomeGreen))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Income", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
        }
      }
    }

    if (trends.isEmpty()) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(130.dp),
        contentAlignment = Alignment.Center
      ) {
        Text("No activity recorded for this period", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
    } else {
      // Bar Chart Container
      Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 14.dp)
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .height(110.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
          ) {
            trends.forEach { trend ->
              val isSelected = selectedTrend == trend
              val expHeightFraction = ((trend.expenseAmount / maxVal).toFloat() * animationProgress.value).coerceIn(0.04f, 1f)
              val incHeightFraction = ((trend.incomeAmount / maxVal).toFloat() * animationProgress.value).coerceIn(0.04f, 1f)

              Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier
                  .weight(1f)
                  .height(110.dp)
                  .clickable {
                    selectedTrend = if (selectedTrend == trend) null else trend
                  }
                  .padding(horizontal = 2.dp)
              ) {
                Row(
                  horizontalArrangement = Arrangement.spacedBy(2.dp),
                  verticalAlignment = Alignment.Bottom,
                  modifier = Modifier.weight(1f, fill = false)
                ) {
                  // Expense Bar
                  if (trend.expenseAmount > 0 || trend.incomeAmount == 0.0) {
                    Box(
                      modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(if (trend.incomeAmount > 0) 0.5f else 1f)
                        .height((90.dp * expHeightFraction))
                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                        .background(if (isSelected) ExpenseRed else ExpenseRed.copy(alpha = 0.85f))
                    )
                  }
                  // Income Bar
                  if (trend.incomeAmount > 0) {
                    Box(
                      modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(if (trend.expenseAmount > 0) 0.5f else 1f)
                        .height((90.dp * incHeightFraction))
                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                        .background(if (isSelected) IncomeGreen else IncomeGreen.copy(alpha = 0.85f))
                    )
                  }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Day Label
                Text(
                  text = trend.dayLabel.split(" ").lastOrNull() ?: trend.dayLabel,
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                  ),
                  color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                  maxLines = 1
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
fun DayOfWeekSpendingCard(
  breakdown: List<DayOfWeekBreakdown>,
  currencySymbol: String,
  modifier: Modifier = Modifier
) {
  val maxSpend = breakdown.maxOfOrNull { it.totalSpent }?.coerceAtLeast(1.0) ?: 1.0

  Column(
    modifier = modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    breakdown.forEach { item ->
      val progress = (item.totalSpent / maxSpend).toFloat().coerceIn(0f, 1f)
      val isPeak = item.totalSpent == maxSpend && item.totalSpent > 0

      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = item.shortName,
          style = MaterialTheme.typography.bodySmall.copy(
            fontWeight = if (isPeak) FontWeight.Bold else FontWeight.Medium,
            fontSize = 12.sp
          ),
          color = if (isPeak) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
          modifier = Modifier.width(36.dp)
        )

        Spacer(modifier = Modifier.width(8.dp))

        Box(
          modifier = Modifier
            .weight(1f)
            .height(10.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
          Box(
            modifier = Modifier
              .fillMaxWidth(progress)
              .height(10.dp)
              .clip(RoundedCornerShape(6.dp))
              .background(if (isPeak) AccentIndigo else MaterialTheme.colorScheme.primary.copy(alpha = 0.75f))
          )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Text(
          text = formatCurrency(item.totalSpent, currencySymbol),
          style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = if (isPeak) FontWeight.Bold else FontWeight.Medium,
            fontSize = 11.sp
          ),
          color = if (isPeak) AccentIndigo else MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = TextAlign.End,
          modifier = Modifier.width(65.dp)
        )
      }
    }
  }
}
