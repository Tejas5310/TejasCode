package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.model.Certificate
import com.example.model.PaymentTransaction
import com.example.model.PlanConfig
import com.example.model.SettlementRecord
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CertificateExportHelper {

    fun generateQrBitmap(content: String, sizePx: Int = 360): Bitmap? {
        return try {
            val bitMatrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx)
            val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
            for (x in 0 until sizePx) {
                for (y in 0 until sizePx) {
                    bmp.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
                }
            }
            bmp
        } catch (_: Exception) {
            null
        }
    }

    fun exportCertificatePdfAndShare(context: Context, cert: Certificate): Result<Unit> {
        return try {
            val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val safeFileName = "TejasCode_Certificate_${cert.verificationCode.replace(Regex("[^a-zA-Z0-9_-]"), "_")}.pdf"
            val pdfFile = File(exportDir, safeFileName)

            val doc = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(842, 595, 1).create() // A4 Landscape
            val page = doc.startPage(pageInfo)
            val canvas: Canvas = page.canvas

            // Background Navy
            val bgPaint = Paint().apply { color = Color.parseColor("#05091B") }
            canvas.drawRect(0f, 0f, 842f, 595f, bgPaint)

            // Border frame
            val borderPaint = Paint().apply {
                color = Color.parseColor("#00D2FF")
                style = Paint.Style.STROKE
                strokeWidth = 6f
            }
            canvas.drawRect(24f, 24f, 818f, 571f, borderPaint)

            val accentPaint = Paint().apply {
                color = Color.parseColor("#A855F7")
                style = Paint.Style.STROKE
                strokeWidth = 2f
            }
            canvas.drawRect(36f, 36f, 806f, 559f, accentPaint)

            val titlePaint = Paint().apply {
                color = Color.parseColor("#00D2FF")
                textSize = 34f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }
            canvas.drawText("TejasCode Verified Certificate", 72f, 110f, titlePaint)

            val subPaint = Paint().apply {
                color = Color.parseColor("#94A3B8")
                textSize = 16f
                isAntiAlias = true
            }
            canvas.drawText("Learn • Code • Grow — Official Completion Credential", 72f, 145f, subPaint)

            val labelPaint = Paint().apply {
                color = Color.parseColor("#CBD5E1")
                textSize = 18f
                isAntiAlias = true
            }
            canvas.drawText("THIS CERTIFIES THAT", 72f, 220f, labelPaint)

            val namePaint = Paint().apply {
                color = Color.WHITE
                textSize = 40f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }
            canvas.drawText(cert.recipientName.ifBlank { "TejasCode Student" }, 72f, 275f, namePaint)

            canvas.drawText("has successfully completed the track", 72f, 325f, labelPaint)

            val trackPaint = Paint().apply {
                color = Color.parseColor("#F59E0B")
                textSize = 28f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }
            canvas.drawText(cert.trackTitle, 72f, 372f, trackPaint)

            val dateStr = cert.earnedAt?.toDate()?.let {
                SimpleDateFormat("dd MMM yyyy", Locale.US).format(it)
            } ?: SimpleDateFormat("dd MMM yyyy", Locale.US).format(Date())

            canvas.drawText("Issued: $dateStr", 72f, 455f, subPaint)
            canvas.drawText("Verification ID: ${cert.verificationCode}", 72f, 485f, subPaint)

            val verifyUrl = "https://tejascode.dev/verify/${cert.verificationCode}"
            val qr = generateQrBitmap(verifyUrl, 150)
            if (qr != null) {
                canvas.drawBitmap(qr, 620f, 370f, null)
                canvas.drawText("Scan to verify", 645f, 540f, subPaint)
            }

            doc.finishPage(page)
            FileOutputStream(pdfFile).use { doc.writeTo(it) }
            doc.close()

            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", pdfFile)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "TejasCode Certificate — ${cert.trackTitle}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Save or Share Certificate PDF"))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun exportPaymentsCsvAndShare(
        context: Context,
        transactions: List<PaymentTransaction>,
        settlements: List<SettlementRecord>
    ): Result<Unit> {
        return try {
            val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val file = File(exportDir, "tejascode_payments_settlements_${System.currentTimeMillis()}.csv")
            val sb = StringBuilder()
            sb.appendLine("RECORD_TYPE,ID,USER_OR_BANK,PROVIDER,INTERVAL,GROSS,FEE,TAX,NET,CURRENCY,STATUS,REF")
            transactions.forEach { tx ->
                sb.appendLine(
                    "TRANSACTION,${tx.transactionId},${tx.userId},${tx.provider},${tx.billingInterval}," +
                        "${PlanConfig.formatMinorAmount(tx.grossAmountMinor, "")}," +
                        "${PlanConfig.formatMinorAmount(tx.gatewayFeeMinor, "")}," +
                        "${PlanConfig.formatMinorAmount(tx.taxMinor, "")}," +
                        "${PlanConfig.formatMinorAmount(tx.netAmountMinor, "")}," +
                        "${tx.currency},${tx.status},${tx.invoiceRef}"
                )
            }
            settlements.forEach { st ->
                sb.appendLine(
                    "SETTLEMENT,${st.settlementId},${st.bankAccountMasked},${st.provider},payout," +
                        "${PlanConfig.formatMinorAmount(st.grossAmountMinor, "")}," +
                        "${PlanConfig.formatMinorAmount(st.gatewayFeeMinor, "")}," +
                        "${PlanConfig.formatMinorAmount(st.gstOnFeeMinor, "")}," +
                        "${PlanConfig.formatMinorAmount(st.netPayoutMinor, "")}," +
                        "${st.currency},${st.status},${st.utrNumber.orEmpty()}"
                )
            }
            file.writeText(sb.toString())

            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "TejasCode Payments & Bank Settlements Report")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Export Payments CSV"))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
