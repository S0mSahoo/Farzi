package com.example.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.example.data.model.TransactionCategory
import com.example.data.model.TransactionItem
import com.example.data.model.TransactionType
import com.example.ui.components.DateUtils
import com.example.ui.components.IndianCurrencyFormatter
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max

object PdfReportGenerator {

  // A4 Standard Dimensions at 72 DPI: 595 x 842 points
  private const val PAGE_WIDTH = 595
  private const val PAGE_HEIGHT = 842
  private const val MARGIN = 36f
  private const val CONTENT_WIDTH = PAGE_WIDTH - (2 * MARGIN)

  fun generateReport(
    context: Context,
    userName: String,
    periodLabel: String,
    transactions: List<TransactionItem>,
    currencySymbol: String
  ): File {
    val exportDir = File(context.cacheDir, "exports")
    if (!exportDir.exists()) exportDir.mkdirs()

    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
    val safeName = if (userName.isNotBlank()) userName.replace("\\s+".toRegex(), "_") else "User"
    val file = File(exportDir, "Paisa_Statement_${safeName}_$timeStamp.pdf")

    val pdfDocument = PdfDocument()

    // Financial Totals
    var totalIncome = 0.0
    var totalExpense = 0.0
    for (tx in transactions) {
      if (tx.type == TransactionType.INCOME) totalIncome += tx.amount
      else totalExpense += tx.amount
    }
    val netSavings = totalIncome - totalExpense
    val savingsRate = if (totalIncome > 0) (netSavings / totalIncome * 100.0) else 0.0

    // Paints
    val titlePaint = Paint().apply {
      isAntiAlias = true
      color = Color.rgb(15, 23, 42) // Slate 900
      textSize = 20f
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    val subtitlePaint = Paint().apply {
      isAntiAlias = true
      color = Color.rgb(100, 116, 139) // Slate 500
      textSize = 10f
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    }

    val headerPaint = Paint().apply {
      isAntiAlias = true
      color = Color.rgb(15, 23, 42)
      textSize = 12f
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    val bodyPaint = Paint().apply {
      isAntiAlias = true
      color = Color.rgb(51, 65, 85) // Slate 700
      textSize = 9.5f
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    }

    val bodyBoldPaint = Paint().apply {
      isAntiAlias = true
      color = Color.rgb(15, 23, 42)
      textSize = 9.5f
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    val incomePaint = Paint().apply {
      isAntiAlias = true
      color = Color.rgb(16, 185, 129) // Emerald Green
      textSize = 9.5f
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    val expensePaint = Paint().apply {
      isAntiAlias = true
      color = Color.rgb(239, 68, 68) // Red
      textSize = 9.5f
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    val metaTextPaint = Paint().apply {
      isAntiAlias = true
      color = Color.rgb(71, 85, 105)
      textSize = 9f
    }

    val linePaint = Paint().apply {
      isAntiAlias = true
      color = Color.rgb(226, 232, 240) // Slate 200
      strokeWidth = 1f
    }

    val cardBgPaint = Paint().apply {
      isAntiAlias = true
      color = Color.rgb(248, 250, 252) // Slate 50
    }

    val summaryBoxPaint = Paint().apply {
      isAntiAlias = true
      color = Color.rgb(241, 245, 249) // Slate 100
    }

    val tableHeaderBgPaint = Paint().apply {
      isAntiAlias = true
      color = Color.rgb(241, 245, 249) // Slate 100
    }

    val rowAlternateBgPaint = Paint().apply {
      isAntiAlias = true
      color = Color.rgb(250, 250, 250)
    }

    val textDescPaint = TextPaint().apply {
      isAntiAlias = true
      color = Color.rgb(51, 65, 85)
      textSize = 9f
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    }

    // Pagination variables
    var pageNumber = 1
    var currentPageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
    var currentPage = pdfDocument.startPage(currentPageInfo)
    var canvas: Canvas = currentPage.canvas

    var currentY = MARGIN

    // 1. App Header Banner
    fun drawAppHeader(c: Canvas) {
      val headerHeight = 52f
      val rect = RectF(MARGIN, MARGIN, PAGE_WIDTH - MARGIN, MARGIN + headerHeight)
      val bgPaint = Paint().apply {
        color = Color.rgb(16, 185, 129) // Brand Emerald
        isAntiAlias = true
      }
      c.drawRoundRect(rect, 8f, 8f, bgPaint)

      val whiteTitlePaint = Paint().apply {
        color = Color.WHITE
        textSize = 18f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        isAntiAlias = true
      }
      c.drawText("PAISA", MARGIN + 16f, MARGIN + 28f, whiteTitlePaint)

      val whiteSubPaint = Paint().apply {
        color = Color.rgb(220, 252, 231) // light emerald
        textSize = 9.5f
        isAntiAlias = true
      }
      c.drawText("Financial Statement & Expense Report", MARGIN + 16f, MARGIN + 43f, whiteSubPaint)

      val genDateStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())
      val datePaint = Paint().apply {
        color = Color.WHITE
        textSize = 8.5f
        textAlign = Paint.Align.RIGHT
        isAntiAlias = true
      }
      c.drawText("Generated: $genDateStr", PAGE_WIDTH - MARGIN - 16f, MARGIN + 28f, datePaint)
      c.drawText("100% Offline & Private", PAGE_WIDTH - MARGIN - 16f, MARGIN + 43f, datePaint)
    }

    drawAppHeader(canvas)
    currentY = MARGIN + 64f

    // 2. Report Metadata Box
    val metaRect = RectF(MARGIN, currentY, PAGE_WIDTH - MARGIN, currentY + 44f)
    canvas.drawRoundRect(metaRect, 6f, 6f, summaryBoxPaint)
    canvas.drawRoundRect(metaRect, 6f, 6f, linePaint.apply { style = Paint.Style.STROKE })

    canvas.drawText("Prepared For: ${if (userName.isNotBlank()) userName else "Self"}", MARGIN + 12f, currentY + 18f, bodyBoldPaint)
    canvas.drawText("Period: $periodLabel", MARGIN + 12f, currentY + 34f, metaTextPaint)

    val txCountStr = "Total Records: ${transactions.size}"
    val rightMetaPaint = Paint(metaTextPaint).apply { textAlign = Paint.Align.RIGHT }
    canvas.drawText(txCountStr, PAGE_WIDTH - MARGIN - 12f, currentY + 26f, rightMetaPaint)

    currentY += 54f

    // 3. Executive Financial Summary (4 Cards)
    val cardSpacing = 8f
    val numCards = 4
    val cardWidth = (CONTENT_WIDTH - (cardSpacing * (numCards - 1))) / numCards
    val cardHeight = 52f

    // Card 1: Total Income
    var cardX = MARGIN
    var cardRect = RectF(cardX, currentY, cardX + cardWidth, currentY + cardHeight)
    canvas.drawRoundRect(cardRect, 6f, 6f, cardBgPaint)
    canvas.drawRoundRect(cardRect, 6f, 6f, linePaint)
    canvas.drawText("TOTAL INCOME", cardX + 8f, currentY + 16f, subtitlePaint)
    canvas.drawText(
      IndianCurrencyFormatter.formatWithSymbol(totalIncome, currencySymbol),
      cardX + 8f,
      currentY + 38f,
      incomePaint.apply { textSize = 11f }
    )

    // Card 2: Total Expenses
    cardX += cardWidth + cardSpacing
    cardRect = RectF(cardX, currentY, cardX + cardWidth, currentY + cardHeight)
    canvas.drawRoundRect(cardRect, 6f, 6f, cardBgPaint)
    canvas.drawRoundRect(cardRect, 6f, 6f, linePaint)
    canvas.drawText("TOTAL EXPENSES", cardX + 8f, currentY + 16f, subtitlePaint)
    canvas.drawText(
      IndianCurrencyFormatter.formatWithSymbol(totalExpense, currencySymbol),
      cardX + 8f,
      currentY + 38f,
      expensePaint.apply { textSize = 11f }
    )

    // Card 3: Net Savings
    cardX += cardWidth + cardSpacing
    cardRect = RectF(cardX, currentY, cardX + cardWidth, currentY + cardHeight)
    canvas.drawRoundRect(cardRect, 6f, 6f, cardBgPaint)
    canvas.drawRoundRect(cardRect, 6f, 6f, linePaint)
    canvas.drawText("NET SAVINGS", cardX + 8f, currentY + 16f, subtitlePaint)
    val savingsColorPaint = if (netSavings >= 0) incomePaint else expensePaint
    canvas.drawText(
      IndianCurrencyFormatter.formatWithSymbol(netSavings, currencySymbol, includeSign = true),
      cardX + 8f,
      currentY + 38f,
      savingsColorPaint.apply { textSize = 11f }
    )

    // Card 4: Savings Rate
    cardX += cardWidth + cardSpacing
    cardRect = RectF(cardX, currentY, cardX + cardWidth, currentY + cardHeight)
    canvas.drawRoundRect(cardRect, 6f, 6f, cardBgPaint)
    canvas.drawRoundRect(cardRect, 6f, 6f, linePaint)
    canvas.drawText("SAVINGS RATE", cardX + 8f, currentY + 16f, subtitlePaint)
    canvas.drawText(
      "${String.format(Locale.US, "%.1f", savingsRate)}%",
      cardX + 8f,
      currentY + 38f,
      bodyBoldPaint.apply { textSize = 11f }
    )

    currentY += cardHeight + 18f

    // Table Column Widths
    // Total CONTENT_WIDTH = 523
    val colDateWidth = 65f
    val colTypeWidth = 55f
    val colCatWidth = 110f
    val colAmountWidth = 85f
    val colDescWidth = CONTENT_WIDTH - (colDateWidth + colTypeWidth + colCatWidth + colAmountWidth) // ~208f

    val colDateX = MARGIN
    val colTypeX = colDateX + colDateWidth
    val colCatX = colTypeX + colTypeWidth
    val colDescX = colCatX + colCatWidth
    val colAmountX = PAGE_WIDTH - MARGIN

    fun drawTableHeader(c: Canvas, y: Float) {
      val headerH = 22f
      val rect = RectF(MARGIN, y, PAGE_WIDTH - MARGIN, y + headerH)
      c.drawRoundRect(rect, 4f, 4f, tableHeaderBgPaint)
      c.drawRoundRect(rect, 4f, 4f, linePaint)

      val thPaint = Paint().apply {
        isAntiAlias = true
        color = Color.rgb(30, 41, 59)
        textSize = 9f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
      }

      c.drawText("Date", colDateX + 6f, y + 14f, thPaint)
      c.drawText("Type", colTypeX + 6f, y + 14f, thPaint)
      c.drawText("Category", colCatX + 6f, y + 14f, thPaint)
      c.drawText("Description & Note", colDescX + 6f, y + 14f, thPaint)

      val thAmountPaint = Paint(thPaint).apply { textAlign = Paint.Align.RIGHT }
      c.drawText("Amount", colAmountX - 6f, y + 14f, thAmountPaint)
    }

    fun drawFooter(c: Canvas, page: Int) {
      val footerY = PAGE_HEIGHT - 24f
      c.drawLine(MARGIN, footerY - 8f, PAGE_WIDTH - MARGIN, footerY - 8f, linePaint)

      val footerTextPaint = Paint().apply {
        isAntiAlias = true
        color = Color.rgb(148, 163, 184)
        textSize = 8f
      }
      c.drawText("Paisa • Confidential Personal Financial Report", MARGIN, footerY + 4f, footerTextPaint)

      val pageNumberPaint = Paint(footerTextPaint).apply { textAlign = Paint.Align.RIGHT }
      c.drawText("Page $page", PAGE_WIDTH - MARGIN, footerY + 4f, pageNumberPaint)
    }

    // Section title
    canvas.drawText("Transaction Records", MARGIN, currentY, headerPaint)
    currentY += 8f

    drawTableHeader(canvas, currentY)
    currentY += 26f

    // 4. Draw Rows
    val sortedTransactions = transactions.sortedByDescending { it.timestamp }
    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.US)

    if (sortedTransactions.isEmpty()) {
      val emptyRect = RectF(MARGIN, currentY, PAGE_WIDTH - MARGIN, currentY + 60f)
      canvas.drawRoundRect(emptyRect, 6f, 6f, cardBgPaint)
      canvas.drawRoundRect(emptyRect, 6f, 6f, linePaint)

      val emptyPaint = Paint().apply {
        isAntiAlias = true
        color = Color.rgb(148, 163, 184)
        textSize = 10f
        textAlign = Paint.Align.CENTER
      }
      canvas.drawText("No transactions found for the selected period.", PAGE_WIDTH / 2f, currentY + 35f, emptyPaint)
      currentY += 70f
    } else {
      for ((index, item) in sortedTransactions.withIndex()) {
        // Compute Description layout for wrapping
        val descText = if (item.note.isNotBlank()) "${item.title} (${item.note})" else item.title
        val staticLayout = StaticLayout.Builder.obtain(
          descText,
          0,
          descText.length,
          textDescPaint,
          colDescWidth.toInt() - 12
        ).setAlignment(Layout.Alignment.ALIGN_NORMAL)
          .setLineSpacing(0f, 1f)
          .setIncludePad(false)
          .build()

        val rowHeight = max(24f, staticLayout.height + 12f)

        // Check if row fits in current page
        if (currentY + rowHeight > PAGE_HEIGHT - 45f) {
          drawFooter(canvas, pageNumber)
          pdfDocument.finishPage(currentPage)

          pageNumber++
          currentPageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
          currentPage = pdfDocument.startPage(currentPageInfo)
          canvas = currentPage.canvas

          currentY = MARGIN
          drawTableHeader(canvas, currentY)
          currentY += 26f
        }

        // Draw Row Background
        if (index % 2 == 1) {
          val rowRect = RectF(MARGIN, currentY - 3f, PAGE_WIDTH - MARGIN, currentY + rowHeight - 3f)
          canvas.drawRect(rowRect, rowAlternateBgPaint)
        }

        // Date
        val dateStr = dateFormat.format(Date(item.timestamp))
        canvas.drawText(dateStr, colDateX + 6f, currentY + 11f, bodyPaint)

        // Type Badge Text
        val isIncome = item.type == TransactionType.INCOME
        val typeBadgePaint = if (isIncome) incomePaint else expensePaint
        canvas.drawText(if (isIncome) "Income" else "Expense", colTypeX + 6f, currentY + 11f, typeBadgePaint)

        // Category
        val catName = if (item.category.displayName.length > 18) {
          item.category.displayName.take(16) + "..."
        } else item.category.displayName
        canvas.drawText(catName, colCatX + 6f, currentY + 11f, bodyPaint)

        // Description (Wrapped)
        canvas.save()
        canvas.translate(colDescX + 6f, currentY)
        staticLayout.draw(canvas)
        canvas.restore()

        // Amount
        val amountStr = if (isIncome) {
          "+${IndianCurrencyFormatter.formatWithSymbol(item.amount, currencySymbol)}"
        } else {
          "-${IndianCurrencyFormatter.formatWithSymbol(item.amount, currencySymbol)}"
        }
        val amountAlignPaint = Paint(if (isIncome) incomePaint else expensePaint).apply {
          textAlign = Paint.Align.RIGHT
          textSize = 9.5f
        }
        canvas.drawText(amountStr, colAmountX - 6f, currentY + 11f, amountAlignPaint)

        // Subtle row bottom divider
        canvas.drawLine(MARGIN, currentY + rowHeight - 3f, PAGE_WIDTH - MARGIN, currentY + rowHeight - 3f, linePaint)

        currentY += rowHeight
      }
    }

    // Draw footer on last page
    drawFooter(canvas, pageNumber)
    pdfDocument.finishPage(currentPage)

    // Write to File
    val out = FileOutputStream(file)
    pdfDocument.writeTo(out)
    out.flush()
    out.close()
    pdfDocument.close()

    return file
  }
}
