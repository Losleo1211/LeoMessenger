package at.leosnet.leosmessenger

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.telephony.SmsManager
import android.widget.Toast
import com.android.mms.transaction.PushReceiver
import com.klinker.android.send_message.Message
import com.klinker.android.send_message.Settings
import com.klinker.android.send_message.Transaction

/**
 * LeosMessenger V2.0.1 - MMS-Unterstützung.
 *
 * V2.0.1:
 * - Bildauswahl wird über ACTION_OPEN_DOCUMENT verwendet, damit die App
 *   dauerhaft Leserechte auf das gewählte Bild behalten kann.
 * - MMS-Sendeergebnis wird über einen eigenen BroadcastReceiver ausgewertet.
 */
fun sendeMms(context: Context, telefon: String, text: String, bildUri: Uri): Boolean {
    if (telefon.isBlank()) {
        Toast.makeText(context, "Für diesen Chat ist keine Telefonnummer gespeichert.", Toast.LENGTH_LONG).show()
        return false
    }

    return try {
        val original = context.contentResolver.openInputStream(bildUri)?.use { BitmapFactory.decodeStream(it) }
            ?: throw IllegalArgumentException("Bild konnte nicht gelesen werden")
        val bild = verkleinereFuerMms(original)

        val settings = Settings().apply {
            useSystemSending = true
            deliveryReports = false
        }

        val transaction = Transaction(context, settings)
        transaction.setExplicitBroadcastForSentMms(
            Intent(context, MmsSendStatusReceiver::class.java)
        )

        val message = Message(text, telefon).apply {
            setImage(bild)
        }

        transaction.sendNewMessage(message, Transaction.NO_THREAD_ID)
        Toast.makeText(context, "MMS wird gesendet …", Toast.LENGTH_SHORT).show()
        true
    } catch (e: Exception) {
        Toast.makeText(
            context,
            "MMS konnte nicht gestartet werden: ${e.message ?: e.javaClass.simpleName}",
            Toast.LENGTH_LONG
        ).show()
        false
    }
}

private fun verkleinereFuerMms(bitmap: Bitmap, maxKante: Int = 1280): Bitmap {
    val max = maxOf(bitmap.width, bitmap.height)
    if (max <= maxKante) return bitmap
    val faktor = maxKante.toFloat() / max.toFloat()
    return Bitmap.createScaledBitmap(
        bitmap,
        (bitmap.width * faktor).toInt().coerceAtLeast(1),
        (bitmap.height * faktor).toInt().coerceAtLeast(1),
        true
    )
}

/**
 * Rückmeldung des Android-MMS-Stacks.
 * RESULT_OK = vom System/Netz zum Versand angenommen.
 * Andere Codes werden sichtbar gemeldet, damit ein Fehler nicht mehr
 * unbemerkt als "gesendet" erscheint.
 */
class MmsSendStatusReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val meldung = when (resultCode) {
            Activity.RESULT_OK -> "MMS gesendet ✓"
            SmsManager.MMS_ERROR_UNSPECIFIED -> "MMS nicht gesendet: unbekannter Fehler"
            SmsManager.MMS_ERROR_INVALID_APN -> "MMS nicht gesendet: APN/MMS-Einstellungen prüfen"
            SmsManager.MMS_ERROR_UNABLE_CONNECT_MMS -> "MMS nicht gesendet: keine MMS-Datenverbindung"
            SmsManager.MMS_ERROR_HTTP_FAILURE -> "MMS nicht gesendet: Server-/HTTP-Fehler"
            SmsManager.MMS_ERROR_IO_ERROR -> "MMS nicht gesendet: Ein-/Ausgabefehler"
            SmsManager.MMS_ERROR_RETRY -> "MMS konnte noch nicht gesendet werden – bitte erneut versuchen"
            SmsManager.MMS_ERROR_CONFIGURATION_ERROR -> "MMS nicht gesendet: Mobilfunk-Konfiguration fehlerhaft"
            else -> "MMS nicht gesendet (Fehlercode $resultCode)"
        }

        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, meldung, Toast.LENGTH_LONG).show()
        }

        Handler(Looper.getMainLooper()).postDelayed({
            try {
                val aktuell = importiereSystemMms(context, ladeChats(context))
                speichereChats(context, aktuell)
                context.sendBroadcast(Intent(ACTION_SMS_CHANGED).setPackage(context.packageName))
            } catch (_: Exception) {
                // Beim nächsten Öffnen/Resume wird erneut synchronisiert.
            }
        }, 1000L)
    }
}

/** Empfang von WAP-Push für die Standard-SMS-App. */
class MmsReceiver : PushReceiver()

/** Nach einem MMS-Download den Android-MMS-Speicher neu einlesen. */
class MmsChangedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        Handler(Looper.getMainLooper()).postDelayed({
            try {
                val aktuell = importiereSystemMms(context, ladeChats(context))
                speichereChats(context, aktuell)
                context.sendBroadcast(Intent(ACTION_SMS_CHANGED).setPackage(context.packageName))
            } catch (_: Exception) {
                // Beim nächsten Öffnen/Resume wird erneut synchronisiert.
            }
        }, 1200L)
    }
}
