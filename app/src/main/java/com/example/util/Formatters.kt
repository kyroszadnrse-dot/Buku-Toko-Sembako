package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs

private val localeId = Locale.Builder().setLanguage("in").setRegion("ID").build()

fun formatRupiah(amount: Long): String {
  val formatter = NumberFormat.getNumberInstance(localeId)
  val prefix = if (amount < 0) "-Rp" else "Rp"
  return "$prefix${formatter.format(abs(amount))}"
}

fun formatNumber(value: Long): String = NumberFormat.getNumberInstance(localeId).format(value)

fun formatPercent(value: Double): String = String.format(localeId, "%.1f%%", value)

fun formatDateShort(millis: Long): String {
  val sdf = SimpleDateFormat("dd MMM yyyy", localeId)
  return sdf.format(Date(millis))
}

fun formatDateTime(millis: Long): String {
  val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", localeId)
  return sdf.format(Date(millis))
}

fun formatTimeOnly(millis: Long): String {
  val sdf = SimpleDateFormat("HH:mm", localeId)
  return sdf.format(Date(millis))
}

fun formatDayShort(millis: Long): String {
  val sdf = SimpleDateFormat("EEE", localeId)
  return sdf.format(Date(millis))
}

fun startOfTodayMillis(): Long {
  val cal = Calendar.getInstance()
  cal.set(Calendar.HOUR_OF_DAY, 0)
  cal.set(Calendar.MINUTE, 0)
  cal.set(Calendar.SECOND, 0)
  cal.set(Calendar.MILLISECOND, 0)
  return cal.timeInMillis
}

fun startOfDaysAgoMillis(daysAgo: Int): Long {
  val cal = Calendar.getInstance()
  cal.set(Calendar.HOUR_OF_DAY, 0)
  cal.set(Calendar.MINUTE, 0)
  cal.set(Calendar.SECOND, 0)
  cal.set(Calendar.MILLISECOND, 0)
  cal.add(Calendar.DAY_OF_YEAR, -daysAgo)
  return cal.timeInMillis
}

fun startOfMonthMillis(): Long {
  val cal = Calendar.getInstance()
  cal.set(Calendar.DAY_OF_MONTH, 1)
  cal.set(Calendar.HOUR_OF_DAY, 0)
  cal.set(Calendar.MINUTE, 0)
  cal.set(Calendar.SECOND, 0)
  cal.set(Calendar.MILLISECOND, 0)
  return cal.timeInMillis
}

fun startOfYearMillis(): Long {
  val cal = Calendar.getInstance()
  cal.set(Calendar.MONTH, Calendar.JANUARY)
  cal.set(Calendar.DAY_OF_MONTH, 1)
  cal.set(Calendar.HOUR_OF_DAY, 0)
  cal.set(Calendar.MINUTE, 0)
  cal.set(Calendar.SECOND, 0)
  cal.set(Calendar.MILLISECOND, 0)
  return cal.timeInMillis
}

fun savePickedImageToInternalStorage(context: Context, uri: Uri, folder: String): String? {
  return try {
    val dir = File(context.filesDir, folder)
    if (!dir.exists()) dir.mkdirs()
    val input = context.contentResolver.openInputStream(uri) ?: return null
    val original = BitmapFactory.decodeStream(input)
    input.close()
    if (original == null) return null

    val maxSide = 640
    val ratio =
      minOf(maxSide.toFloat() / original.width, maxSide.toFloat() / original.height, 1f)
    val targetW = (original.width * ratio).toInt().coerceAtLeast(1)
    val targetH = (original.height * ratio).toInt().coerceAtLeast(1)
    val scaled = Bitmap.createScaledBitmap(original, targetW, targetH, true)
    if (scaled != original) original.recycle()

    val destFile = File(dir, "img_${System.currentTimeMillis()}.jpg")
    FileOutputStream(destFile).use { out ->
      scaled.compress(Bitmap.CompressFormat.JPEG, 82, out)
    }
    scaled.recycle()
    destFile.absolutePath
  } catch (_: Exception) {
    null
  }
}

fun sharePlainText(context: Context, title: String, content: String) {
  val intent =
    Intent(Intent.ACTION_SEND).apply {
      type = "text/plain"
      putExtra(Intent.EXTRA_SUBJECT, title)
      putExtra(Intent.EXTRA_TEXT, content)
    }
  context.startActivity(Intent.createChooser(intent, title))
}

fun shareCsvFile(context: Context, fileName: String, csvContent: String) {
  try {
    val reportsDir = File(context.cacheDir, "reports")
    if (!reportsDir.exists()) reportsDir.mkdirs()
    val file = File(reportsDir, fileName)
    file.writeText(csvContent, Charsets.UTF_8)
    val uri =
      FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent =
      Intent(Intent.ACTION_SEND).apply {
        type = "text/csv"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, fileName)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      }
    context.startActivity(Intent.createChooser(intent, "Bagikan Laporan"))
  } catch (_: Exception) {
    sharePlainText(context, fileName, csvContent)
  }
}
