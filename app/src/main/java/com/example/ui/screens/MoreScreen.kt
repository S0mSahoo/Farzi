package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.components.PremiumCard
import com.example.ui.components.PremiumSectionHeading

@Composable
fun MoreScreen(
    onOpenCalendar: () -> Unit,
    onOpenRecurring: () -> Unit,
    onOpenAnalytics: () -> Unit,
    onOpenCategories: () -> Unit,
    onOpenExport: () -> Unit,
    onOpenSettings: () -> Unit
) {
  Box(
    modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
    contentAlignment = Alignment.TopCenter
  ) {
    LazyColumn(
      modifier = Modifier.widthIn(max = 920.dp).fillMaxSize(),
      contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
      verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
      item {
        PremiumSectionHeading(
          title = "More",
          subtitle = "The tools behind a clearer money picture."
        )
      }
      item {
        Surface(
          shape = RoundedCornerShape(26.dp),
          color = Color(0xFF163D2B),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
          ) {
            Surface(shape = RoundedCornerShape(17.dp), color = Color.White.copy(alpha = 0.10f)) {
              Icon(Icons.Default.CloudDone, contentDescription = null, tint = Color(0xFFD4EF75), modifier = Modifier.padding(13.dp).size(26.dp))
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Text("Your money, your control", style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
              Text("Backup, privacy, appearance and data tools are all kept in one place.", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.74f))
            }
          }
        }
      }
      item {
        Text("Explore", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        PremiumCard {
          Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            MoreActionRow(
              title = "Calendar",
              description = "Follow activity across days",
              icon = Icons.Default.CalendarMonth,
              accent = Color(0xFF477F63),
              onClick = onOpenCalendar
            )
            androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            MoreActionRow(
              title = "Recurring payments",
              description = "Bills, subscriptions and regular income",
              icon = Icons.Default.Repeat,
              accent = Color(0xFF718C2D),
              onClick = onOpenRecurring
            )
            androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            MoreActionRow(
              title = "Categories",
              description = "Compare spending and income sources",
              icon = Icons.Default.Category,
              accent = Color(0xFFB87918),
              onClick = onOpenCategories
            )
            androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            MoreActionRow(
              title = "Insights & analytics",
              description = "Monthly and yearly trends",
              icon = Icons.Default.TrendingUp,
              accent = Color(0xFF176B4D),
              onClick = onOpenAnalytics
            )
          }
        }
      }
      item {
        Text("App & data", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        PremiumCard {
          Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            MoreActionRow(
              title = "Backup, export & reports",
              description = "Google Drive, JSON backups and PDF statements",
              icon = Icons.Default.PictureAsPdf,
              accent = Color(0xFFD64A46),
              onClick = onOpenExport
            )
            androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            MoreActionRow(
              title = "Settings & security",
              description = "Theme, currency, app lock and notifications",
              icon = Icons.Default.Settings,
              accent = MaterialTheme.colorScheme.primary,
              onClick = onOpenSettings
            )
          }
        }
      }
      item {
        Text(
          "Paisa is most useful when you record transactions consistently. Your insights reflect the records you enter.",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(horizontal = 2.dp, vertical = 4.dp)
        )
      }
    }
  }
}

@Composable
private fun MoreActionRow(
  title: String,
  description: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  accent: Color,
  onClick: () -> Unit
) {
  Surface(
    onClick = onClick,
    shape = RoundedCornerShape(18.dp),
    color = Color.Transparent,
    modifier = Modifier.fillMaxWidth()
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 5.dp, vertical = 13.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Surface(shape = RoundedCornerShape(13.dp), color = accent.copy(alpha = 0.12f)) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(44.dp)) {
          Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(21.dp))
        }
      }
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
      }
      Icon(Icons.Default.ChevronRight, contentDescription = "Open $title", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(19.dp))
    }
  }
}
