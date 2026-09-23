package at.leosnet.leosmessenger
import androidx.compose.animation.core.FastOutSlowInEasing
import kotlinx.coroutines.delay

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.app.role.RoleManager
import android.app.Service
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import androidx.core.app.NotificationCompat
import android.os.IBinder
import android.os.Build
import android.provider.Settings
import android.net.Uri
import android.provider.Telephony
import android.content.ContentValues
import android.telephony.SmsManager
import android.graphics.BitmapFactory
import android.widget.Toast
import android.widget.EditText
import android.view.Gravity
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.text.InputType
import android.text.Editable
import android.text.TextWatcher
import android.provider.ContactsContract
import androidx.activity.result.contract.ActivityResultContracts
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.core.content.ContextCompat
import androidx.core.view.OnReceiveContentListener
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsAnimationCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.inputmethod.EditorInfoCompat
import androidx.core.view.inputmethod.InputConnectionCompat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.clip
import androidx.compose.ui.zIndex
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import at.leosnet.leosmessenger.ui.theme.LeosMessengerTheme
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * V2.0.16: Rich Content + stabile IME-Behandlung für Chat und Eingabezeile.
 * Gboard und andere IMEs sehen dadurch bereits beim Öffnen des Eingabefeldes,
 * dass Leo's Messenger Bilder, GIFs und Sticker akzeptiert.
 */
private class RichContentEditText(
    context: Context,
    private val onTextValueChanged: (String) -> Unit,
    private val onRichContent: (Uri, String?) -> Unit
) : EditText(context) {

    companion object {
        private val RICH_MIME_TYPES = arrayOf(
            "image/gif",
            "image/webp",
            "image/png",
            "image/jpeg",
            "image/*"
        )
    }

    private var internalTextUpdate = false

    init {
        // TextWatcher erst NACH dem EditText-Konstruktor registrieren.
        // Dadurch kann Android waehrend super(...) keinen Callback aufrufen,
        // bevor onTextValueChanged initialisiert ist (Crash-Fix V2.0.9).
        addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (!internalTextUpdate) {
                    onTextValueChanged(s?.toString().orEmpty())
                }
            }
            override fun afterTextChanged(s: Editable?) = Unit
        })

        // Offizielle AndroidX Receive-Content-API. Diese Registrierung ist wichtig,
        // damit EditorInfoCompat die akzeptierten MIME-Typen an Gboard melden kann.
        ViewCompat.setOnReceiveContentListener(
            this,
            RICH_MIME_TYPES,
            OnReceiveContentListener { _, payload ->
                val clip = payload.clip
                val description = clip.description

                for (i in 0 until clip.itemCount) {
                    val uri = clip.getItemAt(i).uri ?: continue
                    val resolverMime = try {
                        context.contentResolver.getType(uri)
                    } catch (_: Exception) {
                        null
                    }

                    var mime = resolverMime
                    if (mime == null || !mime.startsWith("image/")) {
                        for (m in 0 until description.mimeTypeCount) {
                            val candidate = description.getMimeType(m)
                            if (candidate.startsWith("image/")) {
                                mime = candidate
                                break
                            }
                        }
                    }

                    if (mime?.startsWith("image/") == true) {
                        onRichContent(uri, mime)
                        return@OnReceiveContentListener null
                    }
                }

                // Nicht von uns behandelter Inhalt wird an Android zurückgegeben.
                payload
            }
        )
    }

    fun setTextFromCompose(value: String) {
        if (text?.toString() == value) return
        internalTextUpdate = true
        setText(value)
        setSelection(value.length.coerceAtMost(text?.length ?: 0))
        internalTextUpdate = false
    }

    override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection? {
        val base = super.onCreateInputConnection(outAttrs) ?: return null

        // V2.0.11: Die MIME-Typen nicht nur indirekt aus dem Receive-Content-
        // Listener lesen, sondern Gboard bei JEDEM InputConnection-Aufbau
        // ausdrücklich mitteilen. Das ist auf manchen OEM-/Gboard-Versionen
        // nötig, damit GIF- und Sticker-Schaltflächen aktiviert werden.
        EditorInfoCompat.setContentMimeTypes(outAttrs, RICH_MIME_TYPES)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            outAttrs.contentMimeTypes = RICH_MIME_TYPES
        }

        return InputConnectionCompat.createWrapper(this, base, outAttrs)
    }
}

data class Nachricht(
    val id: Long,
    val text: String,
    val zeitMillis: Long,
    val vonMir: Boolean,
    val status: Int = 2,
    val mmsBildUri: String? = null,
    val mmsMime: String? = null
)

data class Chat(
    val id: Long,
    val name: String,
    val kuerzel: String,
    val nachrichten: List<Nachricht>,
    val telefon: String = ""
)

class MainActivity : ComponentActivity() {
    private var eingehenderSmsIntent by mutableStateOf<Intent?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        eingehenderSmsIntent = intent
        enableEdgeToEdge()
        setContent {
            LeosMessengerTheme {
                MessengerApp(
                    context = applicationContext,
                    smsIntent = eingehenderSmsIntent,
                    onSmsIntentVerarbeitet = { eingehenderSmsIntent = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        eingehenderSmsIntent = intent
    }
}

class RespondViaMessageService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null
}

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION && intent.action != Telephony.Sms.Intents.SMS_DELIVER_ACTION) return
        val pendingResult = goAsync()

        try {
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isEmpty()) return

        val absender = messages.firstOrNull()?.originatingAddress.orEmpty()
        val text = messages.joinToString(separator = "") { it.messageBody.orEmpty() }
        val zeit = messages.firstOrNull()?.timestampMillis ?: System.currentTimeMillis()
        if (absender.isBlank() || text.isBlank()) return

        val chats = ladeChats(context).toMutableList()

        // V1.6.1: Liegt der Chat dieses Absenders im Papierkorb, wird er bei
        // einer neuen SMS automatisch vollständig wiederhergestellt.
        val papierkorb = ladePapierkorb(context).toMutableList()
        val papierkorbIndex = papierkorb.indexOfFirst { gleicheTelefonnummer(it.telefon, absender) }
        if (papierkorbIndex >= 0) {
            val alterChat = papierkorb.removeAt(papierkorbIndex)
            val vorhandenerIndex = chats.indexOfFirst { gleicheTelefonnummer(it.telefon, absender) }
            if (vorhandenerIndex >= 0) {
                val aktiv = chats[vorhandenerIndex]
                val zusammen = (alterChat.nachrichten + aktiv.nachrichten)
                    .distinctBy { n -> "${n.vonMir}|${n.zeitMillis}|${n.text}" }
                    .sortedBy { it.zeitMillis }
                val kontakt = findeKontaktName(context, absender)
                val name = kontakt ?: if (aktiv.name != aktiv.telefon) aktiv.name else alterChat.name
                chats[vorhandenerIndex] = aktiv.copy(
                    name = name,
                    kuerzel = kuerzelAusName(name),
                    nachrichten = zusammen
                )
            } else {
                val kontakt = findeKontaktName(context, absender)
                chats.add(
                    0,
                    alterChat.copy(
                        name = kontakt ?: alterChat.name,
                        kuerzel = kuerzelAusName(kontakt ?: alterChat.name),
                        telefon = absender
                    )
                )
            }
            speicherePapierkorb(context, papierkorb)
        }

        val index = chats.indexOfFirst { gleicheTelefonnummer(it.telefon, absender) }

        // Schutz gegen doppelte System-Broadcasts: dieselbe eingehende SMS
        // darf nur einmal in den Chat übernommen werden.
        val bereitsVorhanden = chats.any { chat ->
            gleicheTelefonnummer(chat.telefon, absender) &&
                chat.nachrichten.any { n ->
                    !n.vonMir &&
                    n.text == text &&
                    kotlin.math.abs(n.zeitMillis - zeit) < 5000L
                }
        }
        if (bereitsVorhanden) return

        val neu = Nachricht(
            id = System.currentTimeMillis(),
            text = text,
            zeitMillis = zeit,
            vonMir = false,
            status = 2
        )

        if (index >= 0) {
            val kontakt = findeKontaktName(context, absender)
            chats[index] = chats[index].copy(
                name = kontakt ?: chats[index].name,
                kuerzel = kuerzelAusName(kontakt ?: chats[index].name),
                nachrichten = chats[index].nachrichten + neu
            )
        } else {
            chats.add(
                0,
                Chat(
                    id = System.currentTimeMillis(),
                    name = findeKontaktName(context, absender) ?: absender,
                    kuerzel = kuerzelAusName(findeKontaktName(context, absender) ?: absender),
                    nachrichten = listOf(neu),
                    telefon = absender
                )
            )
        }
        speichereChats(context, chats)
        markiereUngelesen(context, absender)
        context.sendBroadcast(Intent(ACTION_SMS_CHANGED).setPackage(context.packageName))
        zeigeSmsBenachrichtigung(context, absender, text)
        } finally {
            pendingResult.finish()
        }
    }
}

private const val PREFS_NAME = "leos_messenger"
private const val PREFS_CHATS = "chats_v102"
private const val PREFS_FARBE = "einstellung_farbe"
private const val PREFS_FARBE_EMPFANG = "farbe_empfang"
private const val PREFS_FARBE_GESENDET = "farbe_gesendet"
private const val PREFS_FARBE_UEBERSICHT = "farbe_uebersicht"
private const val PREFS_DESIGN = "einstellung_design"
private const val PREFS_CHAT_ANIMATION = "chat_oeffnen_animation"
private const val PREFS_SCHRIFTGROESSE = "schriftgroesse"
private const val PREFS_PAPIERKORB = "papierkorb_chats"
private const val PREFS_SORTIERUNG = "chat_sortierung"

private fun ladeSortierung(context: Context): String =
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .getString(PREFS_SORTIERUNG, "ZEIT_AB") ?: "ZEIT_AB"

private fun speichereSortierung(context: Context, wert: String) {
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit().putString(PREFS_SORTIERUNG, wert).apply()
}

private fun ladeSchriftgroesse(context: Context): Int =
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getInt(PREFS_SCHRIFTGROESSE, 16)

private fun speichereSchriftgroesse(context: Context, wert: Int) {
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putInt(PREFS_SCHRIFTGROESSE, wert).apply()
}
private const val PREFS_NOTIFY = "notify_enabled"
private const val PREFS_NOTIFY_POPUP = "notify_popup"
private const val PREFS_NOTIFY_SOUND = "notify_sound"
private const val PREFS_NOTIFY_VIBRATE = "notify_vibrate"
private const val PREFS_NOTIFY_PREVIEW = "notify_preview"
private const val PREFS_ACTIVE_PHONE = "active_phone"
private const val PREFS_UNREAD = "unread_total"
private const val PREFS_UNREAD_PHONES = "unread_phones"

private fun ungeleseneNummern(context: Context): Set<String> =
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .getStringSet(PREFS_UNREAD_PHONES, emptySet())?.toSet() ?: emptySet()

private fun chatIstUngelesen(context: Context, telefon: String): Boolean {
    val n = normalisiereTelefonnummer(telefon)
    return n.isNotBlank() && ungeleseneNummern(context).contains(n)
}

private fun markiereUngelesen(context: Context, telefon: String) {
    val n = normalisiereTelefonnummer(telefon)
    if (n.isBlank()) return
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    if (gleicheTelefonnummer(prefs.getString(PREFS_ACTIVE_PHONE, "").orEmpty(), telefon)) return
    val set = prefs.getStringSet(PREFS_UNREAD_PHONES, emptySet())?.toMutableSet() ?: mutableSetOf()
    set.add(n)
    prefs.edit().putStringSet(PREFS_UNREAD_PHONES, set).apply()
}
const val ACTION_SMS_CHANGED = "at.leosnet.leosmessenger.SMS_CHANGED"

private fun notifyBool(context: Context, key: String, standard: Boolean = true): Boolean =
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getBoolean(key, standard)

private fun setNotifyBool(context: Context, key: String, value: Boolean) {
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putBoolean(key, value).apply()
}

private fun zeigeSmsBenachrichtigung(context: Context, nummer: String, text: String) {
    if (!notifyBool(context, PREFS_NOTIFY)) return
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    if (gleicheTelefonnummer(prefs.getString(PREFS_ACTIVE_PHONE, "").orEmpty(), nummer)) return
    if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return

    val popup = notifyBool(context, PREFS_NOTIFY_POPUP)
    val sound = notifyBool(context, PREFS_NOTIFY_SOUND)
    val vibrate = notifyBool(context, PREFS_NOTIFY_VIBRATE)
    val preview = notifyBool(context, PREFS_NOTIFY_PREVIEW)
    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val channelId = "sms_v138_${if (popup) "high" else "normal"}_${if (sound) "s" else "silent"}_${if (vibrate) "v" else "nov"}"
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val importance = if (popup) NotificationManager.IMPORTANCE_HIGH else NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(channelId, "Neue SMS", importance).apply {
            description = "Benachrichtigungen für neue SMS"
            enableVibration(vibrate)
            if (sound) {
                val soundUri = android.provider.Settings.System.DEFAULT_NOTIFICATION_URI
                val audioAttributes = android.media.AudioAttributes.Builder()
                    .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
                setSound(soundUri, audioAttributes)
            } else {
                setSound(null, null)
            }
            setShowBadge(true)
        }
        manager.createNotificationChannel(channel)
    }
    val unread = prefs.getInt(PREFS_UNREAD, 0) + 1
    prefs.edit().putInt(PREFS_UNREAD, unread).apply()
    val openIntent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${Uri.encode(nummer)}"), context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
    }
    val pending = PendingIntent.getActivity(context, normalisiereTelefonnummer(nummer).hashCode(), openIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    val gespeicherterChatName = ladeChats(context)
        .firstOrNull { gleicheTelefonnummer(it.telefon, nummer) }
        ?.name
        ?.takeIf { it.isNotBlank() && !gleicheTelefonnummer(it, nummer) }
    val name = gespeicherterChatName ?: findeKontaktName(context, nummer) ?: nummer
    val notification = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(android.R.drawable.sym_action_chat)
        .setContentTitle(name)
        .setContentText(if (preview) text else "Neue Nachricht")
        .setStyle(if (preview) NotificationCompat.BigTextStyle().bigText(text) else null)
        .setContentIntent(pending)
        .setAutoCancel(true)
        .setCategory(NotificationCompat.CATEGORY_MESSAGE)
        .setPriority(if (popup) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
        .setSound(if (sound) android.provider.Settings.System.DEFAULT_NOTIFICATION_URI else null)
        .setVibrate(if (vibrate) longArrayOf(0, 180, 120, 180) else longArrayOf(0))
        .setNumber(unread)
        .build()
    manager.notify(normalisiereTelefonnummer(nummer).hashCode(), notification)
}

private fun chatAlsGelesen(context: Context, telefon: String) {
    if (telefon.isBlank()) return
    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    manager.cancel(normalisiereTelefonnummer(telefon).hashCode())
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val set = prefs.getStringSet(PREFS_UNREAD_PHONES, emptySet())?.toMutableSet() ?: mutableSetOf()
    set.remove(normalisiereTelefonnummer(telefon))
    prefs.edit().putStringSet(PREFS_UNREAD_PHONES, set).putInt(PREFS_UNREAD, set.size).apply()
}

private fun ladeFarbe(context: Context): String =
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(PREFS_FARBE, "Blau") ?: "Blau"

private fun speichereFarbe(context: Context, wert: String) {
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putString(PREFS_FARBE, wert).apply()
}

private fun ladeEinzelFarbe(context: Context, key: String, standard: String): String =
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(key, standard) ?: standard
private fun speichereEinzelFarbe(context: Context, key: String, wert: String) {
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putString(key, wert).apply()
}

private fun ladeDesign(context: Context): String =
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(PREFS_DESIGN, "Wie jetzt") ?: "Wie jetzt"

private fun speichereDesign(context: Context, wert: String) {
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putString(PREFS_DESIGN, wert).apply()
}
private fun ladeChatAnimation(context: Context): String =
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(PREFS_CHAT_ANIMATION, "Seitlich") ?: "Seitlich"
private fun speichereChatAnimation(context: Context, wert: String) {
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putString(PREFS_CHAT_ANIMATION, wert).apply()
}

private fun farbeAuswahl(name: String): Color = when (name) {
    "Weiß" -> Color(0xFFF5F5F5)
    "Hellgrau" -> Color(0xFFCFD8DC)
    "Grau" -> Color(0xFF90A4AE)
    "Dunkelgrau" -> Color(0xFF455A64)
    "Hellblau" -> Color(0xFF64B5F6)
    "Blau" -> Color(0xFF1976D2)
    "Dunkelblau" -> Color(0xFF0D47A1)
    "Türkis" -> Color(0xFF00A99D)
    "Hellgrün" -> Color(0xFF9CCC65)
    "Grün" -> Color(0xFF2E7D32)
    "Gelb" -> Color(0xFFFFC107)
    "Orange" -> Color(0xFFF57C00)
    "Rot" -> Color(0xFFD32F2F)
    "Rosa" -> Color(0xFFEC407A)
    "Violett" -> Color(0xFF7B1FA2)
    else -> Color(0xFF1976D2)
}

private fun kontrastFarbe(hintergrund: Color): Color {
    val helligkeit = 0.299 * hintergrund.red + 0.587 * hintergrund.green + 0.114 * hintergrund.blue
    return if (helligkeit > 0.60) Color(0xFF111111) else Color.White
}

private fun sekundaerTextFarbe(hintergrund: Color): Color =
    kontrastFarbe(hintergrund).copy(alpha = 0.72f)

fun ladeChats(context: Context): List<Chat> {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val gespeichert = prefs.getString(PREFS_CHATS, null)

    if (gespeichert == null) {
        // V1.7.0: Keine künstlichen Test-Chats mehr bei einer Neuinstallation.
        return emptyList()
    }

    return try {
        val chatArray = JSONArray(gespeichert)
        buildList {
            for (i in 0 until chatArray.length()) {
                val c = chatArray.getJSONObject(i)
                val msgArray = c.getJSONArray("nachrichten")
                val msgs = buildList {
                    for (j in 0 until msgArray.length()) {
                        val n = msgArray.getJSONObject(j)
                        add(
                            Nachricht(
                                id = n.getLong("id"),
                                text = n.getString("text"),
                                zeitMillis = n.getLong("zeitMillis"),
                                vonMir = n.getBoolean("vonMir"),
                                status = n.optInt("status", 2),
                                mmsBildUri = n.optString("mmsBildUri", "").takeIf { it.isNotBlank() },
                                mmsMime = n.optString("mmsMime", "").takeIf { it.isNotBlank() }
                            )
                        )
                    }
                }
                add(Chat(c.getLong("id"), c.getString("name"), c.getString("kuerzel"), msgs, c.optString("telefon", "")))
            }
        }
    } catch (_: Exception) {
        // Beschädigte/ungültige lokale Chatdaten erzeugen keine Demo-Chats.
        emptyList()
    }
}

fun speichereChats(context: Context, chats: List<Chat>) {
    val chatArray = JSONArray()
    chats.forEach { chat ->
        val msgArray = JSONArray()
        chat.nachrichten.forEach { n ->
            msgArray.put(
                JSONObject().apply {
                    put("id", n.id)
                    put("text", n.text)
                    put("zeitMillis", n.zeitMillis)
                    put("vonMir", n.vonMir)
                    put("status", n.status)
                    put("mmsBildUri", n.mmsBildUri ?: "")
                    put("mmsMime", n.mmsMime ?: "")
                }
            )
        }
        chatArray.put(
            JSONObject().apply {
                put("id", chat.id)
                put("name", chat.name)
                put("kuerzel", chat.kuerzel)
                put("telefon", chat.telefon)
                put("nachrichten", msgArray)
            }
        )
    }
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit().putString(PREFS_CHATS, chatArray.toString()).apply()
}

private fun chatListeAlsJson(chats: List<Chat>): String {
    val a = JSONArray()
    chats.forEach { c ->
        val ma = JSONArray()
        c.nachrichten.forEach { n -> ma.put(JSONObject().apply {
            put("id", n.id); put("text", n.text); put("zeitMillis", n.zeitMillis); put("vonMir", n.vonMir); put("status", n.status)
            put("mmsBildUri", n.mmsBildUri ?: ""); put("mmsMime", n.mmsMime ?: "")
        }) }
        a.put(JSONObject().apply {
            put("id", c.id); put("name", c.name); put("kuerzel", c.kuerzel); put("telefon", c.telefon); put("nachrichten", ma)
        })
    }
    return a.toString()
}
private fun chatListeAusJson(json: String?): List<Chat> {
    if (json.isNullOrBlank()) return emptyList()
    return try {
        val a=JSONArray(json)
        buildList {
            for(i in 0 until a.length()) {
                val c=a.getJSONObject(i); val ma=c.getJSONArray("nachrichten")
                val ms=buildList {
                    for(j in 0 until ma.length()) {
                        val n=ma.getJSONObject(j)
                        add(Nachricht(
                            n.getLong("id"), n.getString("text"), n.getLong("zeitMillis"),
                            n.getBoolean("vonMir"), n.optInt("status", 2),
                            n.optString("mmsBildUri", "").takeIf { it.isNotBlank() },
                            n.optString("mmsMime", "").takeIf { it.isNotBlank() }
                        ))
                    }
                }
                add(Chat(c.getLong("id"),c.getString("name"),c.getString("kuerzel"),ms,c.optString("telefon","")))
            }
        }
    } catch(_:Exception){ emptyList() }
}
private fun ladePapierkorb(context: Context)=chatListeAusJson(context.getSharedPreferences(PREFS_NAME,Context.MODE_PRIVATE).getString(PREFS_PAPIERKORB,null))
private fun speicherePapierkorb(context: Context,chats:List<Chat>){
    context.getSharedPreferences(PREFS_NAME,Context.MODE_PRIVATE).edit().putString(PREFS_PAPIERKORB,chatListeAlsJson(chats)).apply()
}
private fun aktualisiereKontaktNamen(context: Context,chats:List<Chat>)=chats.map { c ->
    if(c.telefon.isBlank()) c else findeKontaktName(context,c.telefon)?.takeIf{it.isNotBlank()}?.let{ c.copy(name=it,kuerzel=kuerzelAusName(it)) } ?: c
}


private fun istStandardSmsApp(context: Context): Boolean =
    Telephony.Sms.getDefaultSmsPackage(context) == context.packageName

private fun importiereSystemSms(context: Context, bestehend: List<Chat>, meldungAnzeigen: Boolean = true): List<Chat> {
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
        Toast.makeText(context, "SMS-Leseberechtigung fehlt.", Toast.LENGTH_LONG).show()
        return bestehend
    }

    val chats = bestehend.toMutableList()
    var gelesen = 0
    var neuImportiert = 0

    fun kontaktName(nummer: String): String? = findeKontaktName(context, nummer)

    fun importiere(uri: Uri, vonMir: Boolean) {
        val projection = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE
        )
        context.contentResolver.query(
            uri,
            projection,
            null,
            null,
            "${Telephony.Sms.DATE} ASC"
        )?.use { cursor ->
            val idIx = cursor.getColumnIndexOrThrow(Telephony.Sms._ID)
            val adrIx = cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
            val bodyIx = cursor.getColumnIndexOrThrow(Telephony.Sms.BODY)
            val dateIx = cursor.getColumnIndexOrThrow(Telephony.Sms.DATE)

            while (cursor.moveToNext()) {
                gelesen++
                val nummer = cursor.getString(adrIx).orEmpty().trim()
                val body = cursor.getString(bodyIx).orEmpty()
                if (nummer.isBlank() || body.isBlank()) continue

                val systemId = cursor.getLong(idIx)
                val zeit = cursor.getLong(dateIx)
                // Inbox und Sent können dieselbe _ID besitzen. Richtung in die ID einbauen.
                val nachrichtId = -(systemId * 10L + if (vonMir) 2L else 1L)

                var ci = chats.indexOfFirst { gleicheTelefonnummer(it.telefon, nummer) }
                if (ci < 0) {
                    val name = kontaktName(nummer) ?: nummer
                    val neueChatId = -(kotlin.math.abs(nummer.hashCode().toLong()) + 10_000L)
                    chats.add(Chat(neueChatId, name, kuerzelAusName(name), emptyList(), nummer))
                    ci = chats.lastIndex
                }

                val schonVorhanden = chats[ci].nachrichten.any { n ->
                    n.id == nachrichtId ||
                        (n.text == body && n.vonMir == vonMir && kotlin.math.abs(n.zeitMillis - zeit) < 15_000L)
                }
                if (!schonVorhanden) {
                    val neu = Nachricht(nachrichtId, body, zeit, vonMir, 2)
                    chats[ci] = chats[ci].copy(
                        nachrichten = (chats[ci].nachrichten + neu).sortedBy { it.zeitMillis }
                    )
                    neuImportiert++
                }
            }
        }
    }

    return try {
        // Bewusst getrennt lesen: einige Android-/Hersteller-Versionen liefern über
        // content://sms nicht zuverlässig den kompletten historischen Bestand.
        importiere(Telephony.Sms.Inbox.CONTENT_URI, false)
        importiere(Telephony.Sms.Sent.CONTENT_URI, true)

        val sortiert = chats.map { chat ->
            val kontakt = findeKontaktName(context, chat.telefon)
            if (!kontakt.isNullOrBlank()) chat.copy(name = kontakt, kuerzel = kuerzelAusName(kontakt)) else chat
        }.sortedByDescending {
            it.nachrichten.maxOfOrNull { n -> n.zeitMillis } ?: 0L
        }
        speichereChats(context, sortiert)
        if (meldungAnzeigen) {
            Toast.makeText(
                context,
                if (gelesen == 0) "Im Android-SMS-Speicher wurden keine Nachrichten gefunden."
                else "SMS-Speicher gelesen: $gelesen Nachrichten, $neuImportiert neu importiert.",
                Toast.LENGTH_LONG
            ).show()
        }
        sortiert
    } catch (e: Exception) {
        Toast.makeText(
            context,
            "SMS-Importfehler: ${e.javaClass.simpleName}: ${e.message ?: "unbekannt"}",
            Toast.LENGTH_LONG
        ).show()
        bestehend
    }
}

private fun speichereGesendeteSmsImSystem(context: Context, telefon: String, text: String, zeit: Long) {
    if (!istStandardSmsApp(context)) return
    try {
        val values = ContentValues().apply {
            put(Telephony.Sms.ADDRESS, telefon)
            put(Telephony.Sms.BODY, text)
            put(Telephony.Sms.DATE, zeit)
            put(Telephony.Sms.TYPE, Telephony.Sms.MESSAGE_TYPE_SENT)
            put(Telephony.Sms.READ, 1)
        }
        context.contentResolver.insert(Telephony.Sms.Sent.CONTENT_URI, values)
    } catch (_: Exception) { }
}

fun importiereSystemMms(context: Context, bestehend: List<Chat>): List<Chat> {
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
        return bestehend
    }

    val chats = bestehend.toMutableList()
    try {
        val uri = Uri.parse("content://mms")
        val projection = arrayOf("_id", "date", "msg_box")
        context.contentResolver.query(uri, projection, null, null, "date ASC")?.use { cursor ->
            val idIx = cursor.getColumnIndexOrThrow("_id")
            val dateIx = cursor.getColumnIndexOrThrow("date")
            val boxIx = cursor.getColumnIndexOrThrow("msg_box")

            while (cursor.moveToNext()) {
                val mmsId = cursor.getLong(idIx)
                val zeit = cursor.getLong(dateIx) * 1000L
                val box = cursor.getInt(boxIx)
                val vonMir = box == 2 || box == 4 || box == 5 || box == 6

                var nummer = ""
                context.contentResolver.query(
                    Uri.parse("content://mms/$mmsId/addr"),
                    arrayOf("address", "type"), null, null, null
                )?.use { ac ->
                    val aIx = ac.getColumnIndex("address")
                    val tIx = ac.getColumnIndex("type")
                    while (ac.moveToNext()) {
                        val type = if (tIx >= 0) ac.getInt(tIx) else 0
                        val adr = if (aIx >= 0) ac.getString(aIx).orEmpty() else ""
                        if ((!vonMir && type == 137) || (vonMir && type == 151)) {
                            if (adr.isNotBlank() && adr != "insert-address-token") {
                                nummer = adr
                                break
                            }
                        }
                    }
                }
                if (nummer.isBlank()) continue

                var text = ""
                var bildUri: String? = null
                var bildMime: String? = null
                context.contentResolver.query(
                    Uri.parse("content://mms/$mmsId/part"),
                    arrayOf("_id", "ct", "text"), null, null, null
                )?.use { pc ->
                    val pIdIx = pc.getColumnIndex("_id")
                    val ctIx = pc.getColumnIndex("ct")
                    val txIx = pc.getColumnIndex("text")
                    while (pc.moveToNext()) {
                        val partId = if (pIdIx >= 0) pc.getLong(pIdIx) else -1L
                        val ct = if (ctIx >= 0) pc.getString(ctIx).orEmpty() else ""
                        if (ct == "text/plain") {
                            val direct = if (txIx >= 0) pc.getString(txIx).orEmpty() else ""
                            if (direct.isNotBlank()) text = direct
                            else if (partId >= 0) {
                                try {
                                    context.contentResolver.openInputStream(Uri.parse("content://mms/part/$partId"))?.bufferedReader()?.use { br ->
                                        text = br.readText()
                                    }
                                } catch (_: Exception) { }
                            }
                        } else if (ct.startsWith("image/") && bildUri == null && partId >= 0) {
                            bildUri = "content://mms/part/$partId"
                            bildMime = ct
                        }
                    }
                }

                if (text.isBlank() && bildUri == null) continue
                val localId = -(9_000_000_000L + mmsId * 10L + if (vonMir) 2L else 1L)
                var ci = chats.indexOfFirst { gleicheTelefonnummer(it.telefon, nummer) }
                if (ci < 0) {
                    val name = findeKontaktName(context, nummer) ?: nummer
                    chats.add(Chat(-(kotlin.math.abs(nummer.hashCode().toLong()) + 20_000L), name, kuerzelAusName(name), emptyList(), nummer))
                    ci = chats.lastIndex
                }
                if (chats[ci].nachrichten.none { it.id == localId }) {
                    val neu = Nachricht(localId, text, zeit, vonMir, 2, bildUri, bildMime)
                    chats[ci] = chats[ci].copy(nachrichten = (chats[ci].nachrichten + neu).sortedBy { it.zeitMillis })
                }
            }
        }
    } catch (_: Exception) { }
    return chats.sortedByDescending { it.nachrichten.maxOfOrNull { n -> n.zeitMillis } ?: 0L }
}

@Composable
fun MessengerApp(context: Context, smsIntent: Intent? = null, onSmsIntentVerarbeitet: () -> Unit = {}) {
    var standardSms by remember { mutableStateOf(istStandardSmsApp(context)) }
    val chats = remember { mutableStateListOf<Chat>().apply { addAll(ladeChats(context)) } }
    val lifecycleOwner = LocalLifecycleOwner.current

    // V1.0.12: Sobald die App wieder in den Vordergrund kommt, den Android-SMS-Speicher
    // leise synchronisieren. So erscheinen SMS, die zwischenzeitlich von Android gespeichert
    // wurden, ohne dass der Benutzer jedes Mal den manuellen Import starten muss.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME &&
                istStandardSmsApp(context) &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
            ) {
                val smsSync = importiereSystemSms(context, ladeChats(context), meldungAnzeigen = false)
                val synchronisiert = importiereSystemMms(context, smsSync)
                chats.clear()
                chats.addAll(synchronisiert)
                val mitNamen = aktualisiereKontaktNamen(context, chats)
                chats.clear()
                chats.addAll(mitNamen)
                speichereChats(context, chats)
            }

            // V1.7.0: Nach Rückkehr aus der Android-Kontakterstellung die
            // Kontaktnamen unabhängig vom SMS-Import erneut einlesen.
            // Ein kurzer zweiter Durchlauf fängt Geräte ab, bei denen der
            // ContactsProvider den neuen Kontakt erst leicht verzögert liefert.
            if (event == Lifecycle.Event.ON_RESUME &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
            ) {
                fun kontaktNamenNeuEinlesen() {
                    val mitNamen = aktualisiereKontaktNamen(context, chats)
                    chats.clear()
                    chats.addAll(mitNamen)
                    speichereChats(context, chats)
                }

                kontaktNamenNeuEinlesen()
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    kontaktNamenNeuEinlesen()
                }, 400)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // V1.6.1: Diese Zustände müssen VOR dem SMS-Receiver deklariert sein,
    // weil der Receiver sie beim Eingang einer Nachricht verwendet.
    var offenerChatId by remember { mutableStateOf<Long?>(null) }
    var empfangsAnimationId by remember { mutableLongStateOf(Long.MIN_VALUE) }

    // V1.2.0: Eingehende SMS sofort in die Compose-Oberfläche übernehmen,
    // auch wenn Leo`s Messenger bereits im Vordergrund geöffnet ist.
    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                if (intent?.action == ACTION_SMS_CHANGED) {
                    val aktuell = ladeChats(context)

                    // V1.6.1: Eingehende SMS nicht mehr durch clear()+addAll()
                    // übernehmen. Das hat den geöffneten Chat teilweise neu
                    // aufgebaut und damit den Animationszustand zurückgesetzt.
                    // Bestehende Chats werden gezielt aktualisiert.
                    val neueIds = aktuell.map { it.id }.toSet()
                    for (neu in aktuell) {
                        val index = chats.indexOfFirst { alt ->
                            alt.id == neu.id ||
                            (alt.telefon.isNotBlank() && gleicheTelefonnummer(alt.telefon, neu.telefon))
                        }
                        if (index >= 0) {
                            chats[index] = neu
                        } else {
                            chats.add(neu)
                        }
                    }
                    chats.removeAll { alt ->
                        alt.id !in neueIds &&
                        aktuell.none { neu ->
                            alt.telefon.isNotBlank() && gleicheTelefonnummer(alt.telefon, neu.telefon)
                        }
                    }

                    // Die zuletzt empfangene Nachricht ausdrücklich an ChatAnsicht melden.
                    val offen = offenerChatId
                    val aktualisierterOffenerChat = aktuell.firstOrNull { it.id == offen }
                        ?: aktuell.firstOrNull { neu ->
                            val bisher = chats.firstOrNull { it.id == offen }
                            bisher != null &&
                            bisher.telefon.isNotBlank() &&
                            gleicheTelefonnummer(bisher.telefon, neu.telefon)
                        }
                    val empfangen = aktualisierterOffenerChat
                        ?.nachrichten
                        ?.lastOrNull { !it.vonMir }
                    if (empfangen != null) {
                        empfangsAnimationId = empfangen.id
                    }
                }
            }
        }
        ContextCompat.registerReceiver(
            context, receiver, IntentFilter(ACTION_SMS_CHANGED), ContextCompat.RECEIVER_NOT_EXPORTED
        )
        onDispose {
            try { context.unregisterReceiver(receiver) } catch (_: Exception) { }
        }
    }

    val berechtigungsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { ergebnis ->
        val lesenErlaubt = ergebnis[Manifest.permission.READ_SMS] == true ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
        if (istStandardSmsApp(context) && lesenErlaubt) {
            chats.clear()
            chats.addAll(importiereSystemSms(context, ladeChats(context)))
        }
    }

    val standardSmsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        standardSms = istStandardSmsApp(context)
        if (standardSms) {
            val fehlen = buildList {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) add(Manifest.permission.SEND_SMS)
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED) add(Manifest.permission.RECEIVE_SMS)
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) add(Manifest.permission.READ_SMS)
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) add(Manifest.permission.READ_CONTACTS)
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_MMS) != PackageManager.PERMISSION_GRANTED) add(Manifest.permission.RECEIVE_MMS)
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_WAP_PUSH) != PackageManager.PERMISSION_GRANTED) add(Manifest.permission.RECEIVE_WAP_PUSH)
                if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) add(Manifest.permission.POST_NOTIFICATIONS)
            }
            if (fehlen.isNotEmpty()) {
                berechtigungsLauncher.launch(fehlen.toTypedArray())
            } else {
                chats.clear()
                chats.addAll(importiereSystemSms(context, ladeChats(context)))
            }
        }
    }

    LaunchedEffect(Unit) {
        // Wichtig: Ab Android 10 zuerst die SMS-Rolle anfordern.
        // Die geschützten SMS-Berechtigungen werden erst NACH erfolgreicher
        // Auswahl als Standard-SMS-App angefragt.
        if (istStandardSmsApp(context)) {
            standardSms = true
            val fehlen = buildList {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) add(Manifest.permission.SEND_SMS)
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED) add(Manifest.permission.RECEIVE_SMS)
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) add(Manifest.permission.READ_SMS)
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) add(Manifest.permission.READ_CONTACTS)
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_MMS) != PackageManager.PERMISSION_GRANTED) add(Manifest.permission.RECEIVE_MMS)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_WAP_PUSH) != PackageManager.PERMISSION_GRANTED) add(Manifest.permission.RECEIVE_WAP_PUSH)
                if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) add(Manifest.permission.POST_NOTIFICATIONS)
            }
            if (fehlen.isNotEmpty()) {
                berechtigungsLauncher.launch(fehlen.toTypedArray())
            } else {
                chats.clear()
                chats.addAll(importiereSystemSms(context, ladeChats(context)))
            }
        }
    }

    LaunchedEffect(standardSms) {
        if (!standardSms) {
            val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val roleManager = context.getSystemService(RoleManager::class.java)
                if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_SMS) && !roleManager.isRoleHeld(RoleManager.ROLE_SMS)) {
                    roleManager.createRequestRoleIntent(RoleManager.ROLE_SMS)
                } else null
            } else {
                Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT).putExtra(
                    Telephony.Sms.Intents.EXTRA_PACKAGE_NAME,
                    context.packageName
                )
            }
            if (intent != null) standardSmsLauncher.launch(intent)
        }
    }
    var neuerChatDialog by remember { mutableStateOf(false) }
    var bearbeitenChat by remember { mutableStateOf<Chat?>(null) }
    var loeschenChat by remember { mutableStateOf<Chat?>(null) }
    var einstellungenOffen by remember { mutableStateOf(false) }
    var hilfeOffen by remember { mutableStateOf(false) }
    var papierkorbOffen by remember { mutableStateOf(false) }

    val papierkorb = remember { mutableStateListOf<Chat>().apply { addAll(ladePapierkorb(context)) } }
    var schriftgroesse by remember { mutableIntStateOf(ladeSchriftgroesse(context)) }
    var farbName by remember { mutableStateOf(ladeFarbe(context)) }
    var farbeEmpfang by remember { mutableStateOf(ladeEinzelFarbe(context, PREFS_FARBE_EMPFANG, "Hellblau")) }
    var farbeGesendet by remember { mutableStateOf(ladeEinzelFarbe(context, PREFS_FARBE_GESENDET, "Gelb")) }
    var farbeUebersicht by remember { mutableStateOf(ladeEinzelFarbe(context, PREFS_FARBE_UEBERSICHT, farbName)) }
    var designName by remember { mutableStateOf(ladeDesign(context)) }
    var chatAnimation by remember { mutableStateOf(ladeChatAnimation(context)) }
    val akzentFarbe = farbeAuswahl(farbeUebersicht)

    // V1.1.7: Aufrufe aus Kontakte/Telefon mit sms: oder smsto: direkt in den passenden Chat leiten.
    LaunchedEffect(smsIntent) {
        val intent = smsIntent
        if (intent != null && intent.action == Intent.ACTION_SENDTO) {
            val data = intent.data
            val scheme = data?.scheme?.lowercase(Locale.getDefault())
            if (scheme == "sms" || scheme == "smsto" || scheme == "mms" || scheme == "mmsto") {
                val rohNummer = data?.schemeSpecificPart.orEmpty().substringBefore("?")
                val nummer = Uri.decode(rohNummer).trim()
                if (nummer.isNotBlank()) {
                    var index = chats.indexOfFirst { gleicheTelefonnummer(it.telefon, nummer) }
                    if (index < 0) {
                        val kontaktName = findeKontaktName(context, nummer) ?: nummer
                        val neuerChat = Chat(
                            id = System.currentTimeMillis(),
                            name = kontaktName,
                            kuerzel = kuerzelAusName(kontaktName),
                            nachrichten = emptyList(),
                            telefon = nummer
                        )
                        chats.add(0, neuerChat)
                        speichereChats(context, chats)
                        index = 0
                    }
                    offenerChatId = chats[index].id
                }
            }
            onSmsIntentVerarbeitet()
        }
    }

    val offenerChat = chats.firstOrNull { it.id == offenerChatId }

    DisposableEffect(offenerChat?.telefon) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val telefon = offenerChat?.telefon.orEmpty()
        prefs.edit().putString(PREFS_ACTIVE_PHONE, telefon).apply()
        if (telefon.isNotBlank()) chatAlsGelesen(context, telefon)
        onDispose {
            if (prefs.getString(PREFS_ACTIVE_PHONE, "") == telefon) prefs.edit().putString(PREFS_ACTIVE_PHONE, "").apply()
        }
    }

    // V1.7.0: Die Übersicht bleibt immer vollständig hinter dem Chat bestehen.
    // Dadurch gleitet/blendet der Chat über der echten Übersicht ein, statt über
    // einem leeren/hellen Zwischenframe.
    Box(modifier = Modifier.fillMaxSize()) {
        ChatUebersicht(
            chats = chats,
            onChatClick = { offenerChatId = it.id },
            onNeuerChat = { neuerChatDialog = true },
            akzentFarbe = akzentFarbe,
            onEinstellungen = { einstellungenOffen = true },
            onHilfe = { hilfeOffen = true },
            onPapierkorb = { papierkorbOffen = true },
            onSmsNeuEinlesen = {
                val lesenErlaubt = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.READ_SMS
                ) == PackageManager.PERMISSION_GRANTED
                if (!lesenErlaubt) {
                    Toast.makeText(context, "SMS-Leseberechtigung fehlt.", Toast.LENGTH_LONG).show()
                } else {
                    val smsNeu = importiereSystemSms(context, ladeChats(context))
                    val neu = importiereSystemMms(context, smsNeu)
                    chats.clear()
                    chats.addAll(neu)
                    speichereChats(context, chats)
                }
            },
            onBearbeiten = { bearbeitenChat = it },
            onLoeschen = { chat ->
                papierkorb.removeAll { gleicheTelefonnummer(it.telefon, chat.telefon) }
                papierkorb.add(0, chat)
                chats.removeAll { it.id == chat.id || gleicheTelefonnummer(it.telefon, chat.telefon) }
                speichereChats(context, chats)
                speicherePapierkorb(context, papierkorb)
            }
        )

        if (offenerChat != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(50f)
            ) {
                ChatAnsicht(
            chat = offenerChat,
            akzentFarbe = akzentFarbe,
            designName = designName,
            farbeEmpfang = farbeAuswahl(farbeEmpfang),
            farbeGesendet = farbeAuswahl(farbeGesendet),
            schriftgroesse = schriftgroesse,
            empfangsAnimationId = empfangsAnimationId,
            oeffnenAnimation = chatAnimation,
            onZurueck = { offenerChatId = null },
            onKontaktAktualisieren = {
                val index = chats.indexOfFirst { it.id == offenerChat.id }
                if (index >= 0) {
                    findeKontaktName(context, chats[index].telefon)?.takeIf { it.isNotBlank() }?.let { name ->
                        chats[index] = chats[index].copy(name = name, kuerzel = kuerzelAusName(name))
                        speichereChats(context, chats)
                    }
                }
            },
            onNachrichtLoeschen = { nachricht ->
                val index = chats.indexOfFirst { it.id == offenerChat.id }
                if (index >= 0) {
                    chats[index] = chats[index].copy(
                        nachrichten = chats[index].nachrichten.filterNot { it.id == nachricht.id }
                    )
                    speichereChats(context, chats)
                }
            },
            onNachrichtSenden = { text, bildUri ->
                val erfolgreich = if (bildUri != null) {
                    sendeMms(context, offenerChat.telefon, text, bildUri)
                } else {
                    sendeSms(context, offenerChat.telefon, text)
                }
                if (erfolgreich) {
                    val index = chats.indexOfFirst { it.id == offenerChat.id }
                    if (index >= 0) {
                        val jetzt = System.currentTimeMillis()
                        val neu = Nachricht(
                            id = jetzt,
                            text = text,
                            zeitMillis = jetzt,
                            vonMir = true,
                            status = 1,
                            mmsBildUri = bildUri?.toString(),
                            mmsMime = bildUri?.let { context.contentResolver.getType(it) ?: "image/*" }
                        )
                        if (bildUri == null) {
                            speichereGesendeteSmsImSystem(context, offenerChat.telefon, text, jetzt)
                        }
                        chats[index] = chats[index].copy(
                            nachrichten = chats[index].nachrichten + neu
                        )
                        speichereChats(context, chats)
                    }
                }
                erfolgreich
            }
                )
            }
        }
    }

    if (neuerChatDialog) {
        ChatNameDialog(
            titel = "Neuer Chat",
            startName = "",
            startTelefon = "",
            bestaetigenText = "Anlegen",
            onAbbrechen = { neuerChatDialog = false },
            onBestaetigen = { name, telefon ->
                val id = System.currentTimeMillis()
                chats.add(
                    0,
                    Chat(
                        id = id,
                        name = name,
                        kuerzel = kuerzelAusName(name),
                        nachrichten = emptyList(),
                        telefon = telefon
                    )
                )
                speichereChats(context, chats)
                neuerChatDialog = false
                offenerChatId = id
            }
        )
    }

    bearbeitenChat?.let { chat ->
        ChatNameDialog(
            titel = "Chat umbenennen",
            startName = chat.name,
            startTelefon = chat.telefon,
            bestaetigenText = "Speichern",
            onAbbrechen = { bearbeitenChat = null },
            onBestaetigen = { name, telefon ->
                val index = chats.indexOfFirst { it.id == chat.id }
                if (index >= 0) {
                    chats[index] = chats[index].copy(
                        name = name,
                        kuerzel = kuerzelAusName(name),
                        telefon = telefon
                    )
                    speichereChats(context, chats)
                }
                bearbeitenChat = null
            }
        )
    }

    if (papierkorbOffen) {
        AlertDialog(
            onDismissRequest = { papierkorbOffen = false },
            title = { Text("Papierkorb") },
            text = {
                Column(Modifier.fillMaxWidth().heightIn(max = 520.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (papierkorb.isEmpty()) Text("Der Papierkorb ist leer.")
                    papierkorb.toList().forEach { chat ->
                        Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Text(chat.name, fontWeight = FontWeight.Bold)
                            if(chat.telefon.isNotBlank()) Text(chat.telefon, fontSize = 12.sp, color = Color.Gray)
                            Row {
                                TextButton(onClick = {
                                    val aktivIndex = chats.indexOfFirst { gleicheTelefonnummer(it.telefon, chat.telefon) }
                                    if (aktivIndex >= 0) {
                                        val aktiv = chats[aktivIndex]
                                        val zusammen = (chat.nachrichten + aktiv.nachrichten)
                                            .distinctBy { n -> "${n.vonMir}|${n.zeitMillis}|${n.text}" }
                                            .sortedBy { it.zeitMillis }
                                        chats[aktivIndex] = aktiv.copy(nachrichten = zusammen)
                                    } else {
                                        chats.add(0, chat)
                                    }
                                    papierkorb.removeAll { gleicheTelefonnummer(it.telefon, chat.telefon) }
                                    speichereChats(context,chats); speicherePapierkorb(context,papierkorb)
                                }) { Text("Wiederherstellen") }
                                TextButton(onClick = {
                                    papierkorb.removeAll{it.id==chat.id}; speicherePapierkorb(context,papierkorb)
                                }) { Text("Endgültig löschen") }
                            }
                            HorizontalDivider()
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick={papierkorbOffen=false}) { Text("Schließen") } },
            dismissButton = { if(papierkorb.isNotEmpty()) TextButton(onClick={papierkorb.clear();speicherePapierkorb(context,papierkorb)}) { Text("Papierkorb leeren") } }
        )
    }

    if (hilfeOffen) {
        AlertDialog(
            onDismissRequest = { hilfeOffen = false },
            title = { Text("Hilfe – Leo`s Messenger") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 520.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Nachrichten", fontWeight = FontWeight.Bold)
                    Text("Tippe auf einen Chat, um ihn zu öffnen. Halte Text gedrückt, um einzelne Textstellen zu markieren und zu kopieren.")
                    Text("Nachricht löschen", fontWeight = FontWeight.Bold)
                    Text("Wische eine einzelne Nachricht nach rechts. Nach ausreichendem Wischen wird nur diese Nachricht aus Leo`s Messenger gelöscht.")
                    Text("Ungelesen", fontWeight = FontWeight.Bold)
                    Text("Neue, noch nicht geöffnete Chats werden in der Übersicht deutlich markiert. Beim Öffnen wird die Markierung entfernt.")
                    Text("Einstellungen", fontWeight = FontWeight.Bold)
                    Text("Unter Einstellungen kannst du Farben, Sprechblasen, Schriftgröße und Benachrichtigungen anpassen. Über „Sortierung“ im Hauptmenü ordnest du Chats nach Zeit oder Name.")
                    Text("SMS-Verlauf", fontWeight = FontWeight.Bold)
                    Text("Mit „SMS/MMS-Verlauf neu einlesen“ kannst du den Android-SMS-Speicher erneut synchronisieren.")
                }
            },
            confirmButton = { TextButton(onClick = { hilfeOffen = false }) { Text("OK") } }
        )
    }

    if (einstellungenOffen) {
        EinstellungenDialog(
            aktuelleFarbeEmpfang = farbeEmpfang,
            aktuelleFarbeGesendet = farbeGesendet,
            aktuelleFarbeUebersicht = farbeUebersicht,
            aktuellesDesign = designName,
            aktuelleChatAnimation = chatAnimation,
            aktuelleSchriftgroesse = schriftgroesse,
            notifyEnabledStart = notifyBool(context, PREFS_NOTIFY),
            notifyPopupStart = notifyBool(context, PREFS_NOTIFY_POPUP),
            notifySoundStart = notifyBool(context, PREFS_NOTIFY_SOUND),
            notifyVibrateStart = notifyBool(context, PREFS_NOTIFY_VIBRATE),
            notifyPreviewStart = notifyBool(context, PREFS_NOTIFY_PREVIEW),
            onAbbrechen = { einstellungenOffen = false },
            onSpeichern = { empfang, gesendet, uebersicht, design, animation, textSize, nEnabled, nPopup, nSound, nVibrate, nPreview ->
                farbeEmpfang = empfang
                farbeGesendet = gesendet
                farbeUebersicht = uebersicht
                farbName = uebersicht
                designName = design
                chatAnimation = animation
                schriftgroesse = textSize
                speichereSchriftgroesse(context, textSize)
                speichereEinzelFarbe(context, PREFS_FARBE_EMPFANG, empfang)
                speichereEinzelFarbe(context, PREFS_FARBE_GESENDET, gesendet)
                speichereEinzelFarbe(context, PREFS_FARBE_UEBERSICHT, uebersicht)
                speichereFarbe(context, uebersicht)
                speichereDesign(context, design)
                speichereChatAnimation(context, animation)
                setNotifyBool(context, PREFS_NOTIFY, nEnabled)
                setNotifyBool(context, PREFS_NOTIFY_POPUP, nPopup)
                setNotifyBool(context, PREFS_NOTIFY_SOUND, nSound)
                setNotifyBool(context, PREFS_NOTIFY_VIBRATE, nVibrate)
                setNotifyBool(context, PREFS_NOTIFY_PREVIEW, nPreview)
                einstellungenOffen = false
            }
        )
    }

    loeschenChat?.let { chat ->
        AlertDialog(
            onDismissRequest = { loeschenChat = null },
            title = { Text("Chat löschen?") },
            text = { Text("Der Chat „${chat.name}“ und alle darin gespeicherten Nachrichten werden gelöscht.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        chats.removeAll { it.id == chat.id }
                        speichereChats(context, chats)
                        loeschenChat = null
                    }
                ) { Text("Löschen") }
            },
            dismissButton = {
                TextButton(onClick = { loeschenChat = null }) { Text("Abbrechen") }
            }
        )
    }
}

private fun kanonischeTelefonnummer(nummer: String): String {
    var n = nummer.trim().filter { it.isDigit() }
    if (n.startsWith("0043")) n = "0" + n.drop(4)
    else if (n.startsWith("43") && !n.startsWith("0")) n = "0" + n.drop(2)
    return n.trimStart('0').let { if (it.isBlank()) n else it }
}

private fun findeKontaktName(context: Context, nummer: String): String? {
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) return null
    val varianten = linkedSetOf(nummer.trim())
    val ziffern = nummer.filter { it.isDigit() }
    if (ziffern.startsWith("43")) varianten.add("0" + ziffern.drop(2))
    if (ziffern.startsWith("0043")) varianten.add("0" + ziffern.drop(4))
    if (ziffern.startsWith("0")) varianten.add("+43" + ziffern.drop(1))
    for (v in varianten) {
        try {
            val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(v))
            context.contentResolver.query(uri, arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) return c.getString(0)
            }
        } catch (_: Exception) { }
    }

    // V1.7.0: Fallback für Geräte/Kontakte, bei denen PhoneLookup einen gerade
    // neu angelegten Kontakt wegen abweichender Schreibweise nicht sofort findet.
    try {
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )
        context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            projection,
            null,
            null,
            null
        )?.use { c ->
            val nameIndex = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numberIndex = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            while (c.moveToNext()) {
                val kontaktNummer = if (numberIndex >= 0) c.getString(numberIndex).orEmpty() else ""
                if (gleicheTelefonnummer(nummer, kontaktNummer)) {
                    val name = if (nameIndex >= 0) c.getString(nameIndex) else null
                    if (!name.isNullOrBlank()) return name
                }
            }
        }
    } catch (_: Exception) { }

    return null
}

private fun normalisiereTelefonnummer(nummer: String): String = kanonischeTelefonnummer(nummer)

private fun gleicheTelefonnummer(a: String, b: String): Boolean {
    val na = kanonischeTelefonnummer(a)
    val nb = kanonischeTelefonnummer(b)
    if (na.isBlank() || nb.isBlank()) return false
    if (na == nb) return true
    // Fallback für internationale Schreibweisen, aber nur mit ausreichend Ziffern.
    return na.length >= 7 && nb.length >= 7 && (na.endsWith(nb) || nb.endsWith(na))
}

private fun sendeSms(context: Context, telefon: String, text: String): Boolean {
    if (telefon.isBlank()) {
        Toast.makeText(context, "Für diesen Chat ist keine Telefonnummer gespeichert.", Toast.LENGTH_LONG).show()
        return false
    }
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
        Toast.makeText(context, "Bitte zuerst die SMS-Berechtigung erlauben.", Toast.LENGTH_LONG).show()
        return false
    }
    return try {
        @Suppress("DEPRECATION")
        val smsManager = SmsManager.getDefault()
        val teile = smsManager.divideMessage(text)
        if (teile.size > 1) {
            smsManager.sendMultipartTextMessage(telefon, null, teile, null, null)
        } else {
            smsManager.sendTextMessage(telefon, null, text, null, null)
        }
        true
    } catch (e: Exception) {
        Toast.makeText(context, "SMS konnte nicht gesendet werden: ${e.message ?: "unbekannter Fehler"}", Toast.LENGTH_LONG).show()
        false
    }
}

private fun kuerzelAusName(name: String): String {
    val teile = name.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    return when {
        teile.isEmpty() -> "?"
        teile.size == 1 -> teile[0].take(1).uppercase(Locale.getDefault())
        else -> (teile.first().take(1) + teile.last().take(1)).uppercase(Locale.getDefault())
    }
}

@Composable
private fun ChatNameDialog(
    titel: String,
    startName: String,
    startTelefon: String,
    bestaetigenText: String,
    onAbbrechen: () -> Unit,
    onBestaetigen: (String, String) -> Unit
) {
    var name by remember(startName) { mutableStateOf(startName) }
    var telefon by remember(startTelefon) { mutableStateOf(startTelefon) }
    val context = androidx.compose.ui.platform.LocalContext.current

    val kontaktWaehler = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val uri = result.data?.data ?: return@rememberLauncherForActivityResult
        context.contentResolver.query(
            uri,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            ),
            null, null, null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val nummerIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                if (nameIndex >= 0) name = cursor.getString(nameIndex) ?: name
                if (nummerIndex >= 0) telefon = cursor.getString(nummerIndex) ?: telefon
            }
        }
    }

    AlertDialog(
        onDismissRequest = onAbbrechen,
        title = { Text(titel) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    label = { Text("Name") },
                    placeholder = { Text("z. B. Gabi") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = telefon,
                    onValueChange = { telefon = it },
                    singleLine = true,
                    label = { Text("Telefonnummer") },
                    placeholder = { Text("z. B. +43 660 ...") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = {
                        kontaktWaehler.launch(
                            Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Aus Telefonbuch wählen")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onBestaetigen(name.trim(), telefon.trim()) },
                enabled = name.isNotBlank()
            ) { Text(bestaetigenText) }
        },
        dismissButton = {
            TextButton(onClick = onAbbrechen) { Text("Abbrechen") }
        }
    )
}

@Composable
private fun EinstellungenDialog(
    aktuelleFarbeEmpfang: String,
    aktuelleFarbeGesendet: String,
    aktuelleFarbeUebersicht: String,
    aktuellesDesign: String,
    aktuelleChatAnimation: String,
    aktuelleSchriftgroesse: Int,
    notifyEnabledStart: Boolean,
    notifyPopupStart: Boolean,
    notifySoundStart: Boolean,
    notifyVibrateStart: Boolean,
    notifyPreviewStart: Boolean,
    onAbbrechen: () -> Unit,
    onSpeichern: (String, String, String, String, String, Int, Boolean, Boolean, Boolean, Boolean, Boolean) -> Unit
) {
    val farben = listOf(
        "Weiß", "Hellgrau", "Grau", "Dunkelgrau",
        "Hellblau", "Blau", "Dunkelblau", "Türkis",
        "Hellgrün", "Grün", "Gelb", "Orange", "Rot", "Rosa", "Violett"
    )
    val designs = listOf("Wie jetzt", "Sprechblasen")
    val chatAnimationen = listOf("Keine", "Seitlich", "Von unten", "Zoom", "Überblenden", "Zoom + Überblenden")
    var empfang by remember(aktuelleFarbeEmpfang) { mutableStateOf(aktuelleFarbeEmpfang) }
    var gesendet by remember(aktuelleFarbeGesendet) { mutableStateOf(aktuelleFarbeGesendet) }
    var uebersicht by remember(aktuelleFarbeUebersicht) { mutableStateOf(aktuelleFarbeUebersicht) }
    var design by remember(aktuellesDesign) { mutableStateOf(aktuellesDesign) }
    var chatAnimation by remember(aktuelleChatAnimation) { mutableStateOf(aktuelleChatAnimation) }
    var textSize by remember(aktuelleSchriftgroesse) { mutableIntStateOf(aktuelleSchriftgroesse) }
    var nEnabled by remember { mutableStateOf(notifyEnabledStart) }
    var nPopup by remember { mutableStateOf(notifyPopupStart) }
    var nSound by remember { mutableStateOf(notifySoundStart) }
    var nVibrate by remember { mutableStateOf(notifyVibrateStart) }
    var nPreview by remember { mutableStateOf(notifyPreviewStart) }

    @Composable fun Farbreihe(titel: String, wert: String, setzen: (String) -> Unit) {
        Text(titel, fontWeight = FontWeight.SemiBold)
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            farben.chunked(8).forEach { zeile ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    zeile.forEach { f ->
                        val farbe = farbeAuswahl(f)
                        Box(Modifier.size(30.dp).background(farbe, CircleShape).clickable { setzen(f) }, contentAlignment = Alignment.Center) {
                            if (wert == f) Text("✓", color = kontrastFarbe(farbe), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
    AlertDialog(
        onDismissRequest = onAbbrechen,
        title = { Text("Einstellungen") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 560.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                Text("Farben", fontWeight = FontWeight.Bold)
                Farbreihe("Empfangene Nachrichten", empfang) { empfang = it }
                Farbreihe("Gesendete Nachrichten", gesendet) { gesendet = it }
                Farbreihe("Chatübersicht", uebersicht) { uebersicht = it }
                HorizontalDivider()
                Text("Design", fontWeight = FontWeight.Bold)
                designs.forEach { eintrag ->
                    Row(Modifier.fillMaxWidth().clickable { design = eintrag }, verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = design == eintrag, onClick = { design = eintrag })
                        Text(if (eintrag == "Wie jetzt") "Standard" else eintrag)
                    }
                }
                HorizontalDivider()
                Text("Chat öffnen – Animation", fontWeight = FontWeight.Bold)
                chatAnimationen.forEach { eintrag ->
                    Row(Modifier.fillMaxWidth().clickable { chatAnimation = eintrag }, verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = chatAnimation == eintrag, onClick = { chatAnimation = eintrag })
                        Text(eintrag)
                    }
                }
                HorizontalDivider()
                Text("Schriftgröße", fontWeight = FontWeight.Bold)
                Text("$textSize pt")
                Slider(
                    value = textSize.toFloat(),
                    onValueChange = { textSize = it.toInt().coerceIn(12, 24) },
                    valueRange = 12f..24f,
                    steps = 11
                )
                Text("Beispiel: So sieht eine Nachricht aus.", fontSize = textSize.sp)
                HorizontalDivider()
                Text("Benachrichtigungen", fontWeight = FontWeight.Bold)
                @Composable fun Schalter(text: String, checked: Boolean, enabled: Boolean = true, change: (Boolean) -> Unit) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text, modifier = Modifier.weight(1f))
                        Switch(checked = checked, onCheckedChange = change, enabled = enabled)
                    }
                }
                Schalter("Benachrichtigungen", nEnabled) { nEnabled = it }
                Schalter("Pop-up / Heads-up", nPopup, nEnabled) { nPopup = it }
                Schalter("Ton", nSound, nEnabled) { nSound = it }
                Schalter("Vibration", nVibrate, nEnabled) { nVibrate = it }
                Schalter("Nachrichtenvorschau", nPreview, nEnabled) { nPreview = it }
            }
        },
        confirmButton = { TextButton(onClick = { onSpeichern(empfang, gesendet, uebersicht, design, chatAnimation, textSize, nEnabled, nPopup, nSound, nVibrate, nPreview) }) { Text("Speichern") } },
        dismissButton = { TextButton(onClick = onAbbrechen) { Text("Abbrechen") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatUebersicht(
    chats: List<Chat>,
    onChatClick: (Chat) -> Unit,
    onNeuerChat: () -> Unit,
    akzentFarbe: Color,
    onEinstellungen: () -> Unit,
    onHilfe: () -> Unit,
    onPapierkorb: () -> Unit,
    onSmsNeuEinlesen: () -> Unit,
    onBearbeiten: (Chat) -> Unit,
    onLoeschen: (Chat) -> Unit
) {
    var suche by remember { mutableStateOf("") }
    val context = androidx.compose.ui.platform.LocalContext.current
    var hauptmenuOffen by remember { mutableStateOf(false) }
    var sortiermenuOffen by remember { mutableStateOf(false) }
    var sortierung by remember { mutableStateOf(ladeSortierung(context)) }
    var infoOffen by remember { mutableStateOf(false) }
    val appVersion = remember(context) {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "2.0.18"
        } catch (_: Exception) {
            "2.0.18"
        }
    }
    val scope = rememberCoroutineScope()
    val q = suche.trim()
    val basisChats = chats.toList()
    val gefilterteBasis = if (q.isBlank()) basisChats else basisChats.filter { chat ->
        chat.name.contains(q, ignoreCase = true) ||
            chat.telefon.contains(q, ignoreCase = true) ||
            chat.nachrichten.any { it.text.contains(q, ignoreCase = true) }
    }
    val gefilterteChats = when (sortierung) {
        "ZEIT_AUF" -> gefilterteBasis.sortedBy { it.nachrichten.maxOfOrNull { n -> n.zeitMillis } ?: 0L }
        "NAME_AUF" -> gefilterteBasis.sortedBy { it.name.lowercase(Locale.getDefault()) }
        "NAME_AB" -> gefilterteBasis.sortedByDescending { it.name.lowercase(Locale.getDefault()) }
        else -> gefilterteBasis.sortedByDescending { it.nachrichten.maxOfOrNull { n -> n.zeitMillis } ?: 0L }
    }

    if (infoOffen) {
        val anzahlNachrichten = chats.sumOf { it.nachrichten.size }
        AlertDialog(
            onDismissRequest = { infoOffen = false },
            title = { Text("Leo`s Messenger V$appVersion") },
            text = {
                Text(
                    "${chats.size} Chats · $anzahlNachrichten Nachrichten\n\n" +
                        "Aktuelle Version: V$appVersion\n\n" +
                        "SMS/MMS, Bildanhänge sowie Gboard-GIF-/Sticker-Unterstützung. " +
                        "Die Eingabeleiste und der Nachrichtenbereich berücksichtigen die Bildschirmtastatur gemeinsam."
                )
            },
            confirmButton = {
                TextButton(onClick = { infoOffen = false }) { Text("OK") }
            }
        )
    }

    if (sortiermenuOffen) {
        val optionen = listOf(
            "ZEIT_AB" to "Zeit – neueste zuerst",
            "ZEIT_AUF" to "Zeit – älteste zuerst",
            "NAME_AUF" to "Name – A bis Z",
            "NAME_AB" to "Name – Z bis A"
        )
        AlertDialog(
            onDismissRequest = { sortiermenuOffen = false },
            title = { Text("Chatübersicht sortieren") },
            text = {
                Column {
                    optionen.forEach { (wert, text) ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable {
                                sortierung = wert
                                speichereSortierung(context, wert)
                                sortiermenuOffen = false
                            },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = sortierung == wert,
                                onClick = {
                                    sortierung = wert
                                    speichereSortierung(context, wert)
                                    sortiermenuOffen = false
                                }
                            )
                            Text(text)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { sortiermenuOffen = false }) { Text("Schließen") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Leo`s Messenger", fontWeight = FontWeight.Bold)
                        Text("Nachrichten", fontSize = 12.sp)
                    }
                },
                actions = {
                    Box {
                        TextButton(onClick = { hauptmenuOffen = true }) {
                            Text("⋮", fontSize = 24.sp)
                        }
                        DropdownMenu(
                            expanded = hauptmenuOffen,
                            onDismissRequest = { hauptmenuOffen = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Einstellungen") },
                                onClick = {
                                    hauptmenuOffen = false
                                    onEinstellungen()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Sortierung") },
                                onClick = {
                                    hauptmenuOffen = false
                                    sortiermenuOffen = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("SMS/MMS-Verlauf neu einlesen") },
                                onClick = {
                                    hauptmenuOffen = false
                                    onSmsNeuEinlesen()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Papierkorb") },
                                onClick = { hauptmenuOffen = false; onPapierkorb() }
                            )
                            DropdownMenuItem(
                                text = { Text("Hilfe") },
                                onClick = {
                                    hauptmenuOffen = false
                                    onHilfe()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Info zu V$appVersion") },
                                onClick = {
                                    hauptmenuOffen = false
                                    infoOffen = true
                                }
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNeuerChat, containerColor = akzentFarbe, contentColor = Color.White) {
                Text("+", fontSize = 28.sp)
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = suche,
                onValueChange = { suche = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                singleLine = true,
                label = { Text("Suchen") },
                placeholder = { Text("Name, Nummer oder Nachricht") },
                trailingIcon = {
                    if (suche.isNotEmpty()) {
                        TextButton(onClick = { suche = "" }) { Text("×", fontSize = 22.sp) }
                    }
                }
            )

            if (chats.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Noch keine Chats", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(8.dp))
                        Text("Tippe auf +, um deinen ersten Chat anzulegen.", color = Color.Gray)
                    }
                }
            } else if (gefilterteChats.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Keine Treffer für „$suche“", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 2.dp, bottom = 88.dp)
                ) {
                    items(
                        gefilterteChats.size,
                        key = { index ->
                            val c = gefilterteChats[index]
                            // V1.6.1: index is deliberately included as a final
                            // uniqueness guard. Some legacy/imported chats can have
                            // the same id and an empty phone number.
                            "chat_${c.id}_${normalisiereTelefonnummer(c.telefon)}_${index}"
                        }
                    ) { index ->
                        val chat = gefilterteChats[index]
                        var letzteY by remember(chat.id) { mutableFloatStateOf(Float.NaN) }
                        var verschiebungY by remember(chat.id) { mutableFloatStateOf(0f) }
                        val animierteVerschiebungY by animateFloatAsState(
                            targetValue = verschiebungY,
                            animationSpec = tween(
                                durationMillis = 480,
                                easing = androidx.compose.animation.core.FastOutSlowInEasing
                            ),
                            label = "chatSortierVerschiebung"
                        )

                        Column(
                            modifier = Modifier
                                .graphicsLayer { translationY = animierteVerschiebungY }
                                .onGloballyPositioned { coords ->
                                    val neuY = coords.positionInRoot().y
                                    if (!letzteY.isNaN()) {
                                        val delta = letzteY - neuY
                                        if (kotlin.math.abs(delta) > 1f) {
                                            verschiebungY = delta
                                            scope.launch {
                                                kotlinx.coroutines.delay(16)
                                                verschiebungY = 0f
                                            }
                                        }
                                    }
                                    letzteY = neuY
                                }
                        ) {
                            ChatZeile(
                                chat = chat,
                                akzentFarbe = akzentFarbe,
                                ungelesen = chatIstUngelesen(context, chat.telefon),
                                onClick = { onChatClick(chat) },
                                onBearbeiten = { onBearbeiten(chat) },
                                onLoeschen = { onLoeschen(chat) }
                            )
                            HorizontalDivider(modifier = Modifier.padding(start = 78.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatZeile(
    chat: Chat,
    akzentFarbe: Color,
    ungelesen: Boolean,
    onClick: () -> Unit,
    onBearbeiten: () -> Unit,
    onLoeschen: () -> Unit
) {
    var menuOffen by remember { mutableStateOf(false) }
    var swipeX by remember(chat.id) { mutableFloatStateOf(0f) }
    var wirdGeloescht by remember(chat.id) { mutableStateOf(false) }
    var zeileSichtbar by remember(chat.id) { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    val loeschOffset by animateFloatAsState(
        targetValue = if (wirdGeloescht) 900f else 0f,
        animationSpec = tween(520),
        label = "chatLoeschOffset"
    )
    val loeschY by animateFloatAsState(
        targetValue = if (wirdGeloescht) (-90f) else 0f,
        animationSpec = tween(520),
        label = "chatLoeschY"
    )
    val loeschRotation by animateFloatAsState(
        targetValue = if (wirdGeloescht) 16f else 0f,
        animationSpec = tween(520),
        label = "chatLoeschRotation"
    )
    val loeschScale by animateFloatAsState(
        targetValue = if (wirdGeloescht) 0.55f else 1f,
        animationSpec = tween(520),
        label = "chatLoeschScale"
    )
    val loeschAlpha by animateFloatAsState(
        targetValue = if (wirdGeloescht) 0f else 1f,
        animationSpec = tween(520),
        label = "chatLoeschAlpha"
    )
    val letzte = chat.nachrichten.maxByOrNull { it.zeitMillis }

    fun animiertLoeschen() {
        if (wirdGeloescht) return
        wirdGeloescht = true
        scope.launch {
            // 1. Chat fliegt wie bisher nach rechts weg.
            kotlinx.coroutines.delay(520)
            // 2. Danach wird sein Platz weich auf 0 zusammengezogen.
            zeileSichtbar = false
            kotlinx.coroutines.delay(380)
            // 3. Erst jetzt tatsächlich in den Papierkorb verschieben.
            onLoeschen()
        }
    }

    AnimatedVisibility(
        visible = zeileSichtbar,
        exit = shrinkVertically(
            animationSpec = tween(380),
            shrinkTowards = Alignment.Top
        ) + fadeOut(animationSpec = tween(220))
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
        if (swipeX > 18f) {
            Row(
                modifier = Modifier
                    .matchParentSize()
                    .background(MaterialTheme.colorScheme.error)
                    .clickable { animiertLoeschen() }
                    .padding(start = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                Text("🗑", fontSize = 25.sp, color = MaterialTheme.colorScheme.onError)
                Spacer(Modifier.width(8.dp))
                Text("Papierkorb", color = MaterialTheme.colorScheme.onError, fontWeight = FontWeight.Bold)
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .offset(
                    x = ((swipeX / 3f) + loeschOffset).dp,
                    y = loeschY.dp
                )
                .graphicsLayer {
                    alpha = loeschAlpha
                    rotationZ = loeschRotation
                    scaleX = loeschScale
                    scaleY = loeschScale
                }
                .background(
                    if (ungelesen) akzentFarbe.copy(alpha = 0.16f)
                    else MaterialTheme.colorScheme.surface
                )
                .pointerInput(chat.id) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            val loeschen = swipeX > 90f
                            swipeX = 0f
                            if (loeschen) animiertLoeschen()
                        },
                        onDragCancel = { swipeX = 0f },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            swipeX = (swipeX + dragAmount).coerceIn(0f, 300f)
                        }
                    )
                }
                .clickable(onClick = onClick)
                .padding(start = 14.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .background(akzentFarbe, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(chat.kuerzel, color = kontrastFarbe(akzentFarbe), fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(chat.name, fontSize = 17.sp, fontWeight = if (ungelesen) FontWeight.Bold else FontWeight.SemiBold, modifier = Modifier.weight(1f))
                if (ungelesen) Box(Modifier.padding(horizontal = 6.dp).size(14.dp).background(akzentFarbe, CircleShape))
                if (letzte != null) {
                    Text(
                        kurzeZeit(letzte.zeitMillis),
                        fontSize = 11.sp,
                        color = if (ungelesen) akzentFarbe else Color.Gray,
                        fontWeight = if (ungelesen) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = letzte?.let { if (it.text.isNotBlank()) it.text else if (!it.mmsBildUri.isNullOrBlank()) "📷 Bild (MMS)" else "Nachricht" } ?: "Noch keine Nachrichten",
                fontSize = 14.sp,
                fontWeight = if (ungelesen) FontWeight.SemiBold else FontWeight.Normal,
                color = if (ungelesen) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Box {
            TextButton(onClick = { menuOffen = true }, contentPadding = PaddingValues(8.dp)) {
                Text("⋮", fontSize = 24.sp)
            }
            DropdownMenu(expanded = menuOffen, onDismissRequest = { menuOffen = false }) {
                DropdownMenuItem(
                    text = { Text("Umbenennen") },
                    onClick = {
                        menuOffen = false
                        onBearbeiten()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Löschen") },
                    onClick = {
                        menuOffen = false
                        animiertLoeschen()
                    }
                )
            }
        }
    }
    }
}

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatAnsicht(
    chat: Chat,
    akzentFarbe: Color,
    designName: String,
    farbeEmpfang: Color,
    farbeGesendet: Color,
    schriftgroesse: Int,
    empfangsAnimationId: Long,
    oeffnenAnimation: String,
    onZurueck: () -> Unit,
    onKontaktAktualisieren: () -> Unit,
    onNachrichtLoeschen: (Nachricht) -> Unit,
    onNachrichtSenden: (String, Uri?) -> Boolean
) {
    var eingabe by remember(chat.id) { mutableStateOf("") }
    var anhangUri by remember(chat.id) { mutableStateOf<Uri?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val bildLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: Exception) { }
            anhangUri = uri
        }
    }

    // V1.7.0: Die Kontakterstellung mit Ergebnis starten. Beim Zurückkehren
    // wird der gerade angelegte Name gezielt für den offenen Chat neu gelesen.
    val kontaktErstellenLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        onKontaktAktualisieren()
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            onKontaktAktualisieren()
        }, 500)
    }

    val listState = rememberLazyListState()
    var chatMenueOffen by remember { mutableStateOf(false) }

    fun kontaktOeffnen() {
        if (chat.telefon.isBlank()) return
        try {
            val lookupUri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(chat.telefon)
            )
            context.contentResolver.query(
                lookupUri,
                arrayOf(
                    ContactsContract.PhoneLookup.LOOKUP_KEY,
                    ContactsContract.PhoneLookup._ID
                ),
                null, null, null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val lookupKey = cursor.getString(0)
                    val contactId = cursor.getLong(1)
                    val contactUri = ContactsContract.Contacts.getLookupUri(contactId, lookupKey)
                    context.startActivity(Intent(Intent.ACTION_VIEW, contactUri))
                } else {
                    Toast.makeText(context, "Kontakt nicht gefunden.", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (_: Exception) {
            Toast.makeText(context, "Kontakt konnte nicht geöffnet werden.", Toast.LENGTH_SHORT).show()
        }
    }
    val scope = rememberCoroutineScope()
    var overlayNachricht by remember(chat.id) { mutableStateOf<Nachricht?>(null) }
    var overlaySichtbar by remember(chat.id) { mutableStateOf(false) }

    // V1.6.1: tatsächliche Y-Position der neuen Nachricht im Chat messen.
    // Kein Raten mehr mit "oben" oder "unten".
    var chatBereichY by remember(chat.id) { mutableFloatStateOf(0f) }
    var zielNachrichtY by remember(chat.id) { mutableFloatStateOf(Float.NaN) }
    var letzteBekannteNachrichtId by remember(chat.id) {
        mutableLongStateOf(chat.nachrichten.lastOrNull()?.id ?: Long.MIN_VALUE)
    }

    var letzteAnimierteNachricht by remember(chat.id) {
        mutableLongStateOf(chat.nachrichten.lastOrNull()?.id ?: Long.MIN_VALUE)
    }

    // V1.6.1: Neue Nachricht schon in derselben Compose-Phase als "fliegend"
    // erkennen. Dadurch wird das echte Listenelement sofort unsichtbar und
    // kann nicht mehr für einen Frame unten aufblitzen.
    val aktuelleLetzte = chat.nachrichten.lastOrNull()
    val empfangExplizitNeu =
        aktuelleLetzte != null &&
        !aktuelleLetzte.vonMir &&
        aktuelleLetzte.id == empfangsAnimationId &&
        aktuelleLetzte.id != letzteAnimierteNachricht

    val wartetAufEinflug =
        aktuelleLetzte != null &&
        (
            empfangExplizitNeu ||
            (
                letzteBekannteNachrichtId != Long.MIN_VALUE &&
                aktuelleLetzte.id != letzteBekannteNachrichtId
            )
        )

    LaunchedEffect(chat.id, aktuelleLetzte?.id) {
        val letzte = aktuelleLetzte ?: return@LaunchedEffect
        if (wartetAufEinflug) {
            zielNachrichtY = Float.NaN
            overlayNachricht = letzte
            overlaySichtbar = true

            // V1.6.1: Bei Empfang vorhandenen Verlauf während des Einflugs
            // sichtbar weich nach oben schieben.
            if (!letzte.vonMir && chat.nachrichten.size > 1) {
                scope.launch {
                    kotlinx.coroutines.delay(60)
                    listState.animateScrollToItem(chat.nachrichten.lastIndex)
                }
            }

            kotlinx.coroutines.delay(650)
            overlaySichtbar = false
            overlayNachricht = null
            letzteBekannteNachrichtId = letzte.id
            letzteAnimierteNachricht = letzte.id
        } else {
            listState.scrollToItem(chat.nachrichten.lastIndex)
            letzteBekannteNachrichtId = letzte.id
            letzteAnimierteNachricht = letzte.id
        }
    }

    var chatSichtbar by remember(chat.id) { mutableStateOf(false) }
    val chatAnimProgress by animateFloatAsState(
        targetValue = if (chatSichtbar || oeffnenAnimation == "Keine") 1f else 0f,
        animationSpec = tween(620, easing = FastOutSlowInEasing),
        label = "chatOeffnenAnimation"
    )
    LaunchedEffect(chat.id, oeffnenAnimation) {
        chatSichtbar = false
        if (oeffnenAnimation == "Keine") chatSichtbar = true
        else {
            delay(90)
            chatSichtbar = true
        }
    }

    var zurueckLaeuft by remember(chat.id) { mutableStateOf(false) }

    fun animiertZurueck() {
        if (zurueckLaeuft) return
        zurueckLaeuft = true
        if (oeffnenAnimation == "Keine") {
            onZurueck()
        } else {
            // Erst den Chat über der weiterhin sichtbaren Übersicht hinaus animieren.
            chatSichtbar = false
            val rueckDauer = when (oeffnenAnimation) {
                "Überblenden", "Zoom", "Zoom + Überblenden" -> 680L
                else -> 580L
            }
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                onZurueck()
            }, rueckDauer)
        }
    }

    // V1.7.0: Auch Android-Zurück / Zurück-Geste zuerst animieren.
    BackHandler(enabled = true) { animiertZurueck() }

    // V2.0.19: IME-Bewegung direkt mit der echten Android-Tastaturanimation
    // synchronisieren. Kein eigener Tween mehr: Die Eingabezeile und der
    // Chatbereich folgen jedem Animations-Frame von Gboard. Dadurch bleibt
    // der letzte Chatinhalt oberhalb der Tastatur und die Bewegung wirkt
    // deutlich flüssiger, ohne dass die Tastatur den Chat überdeckt.
    val density = androidx.compose.ui.platform.LocalDensity.current
    val rootView = LocalView.current
    var imeLiftPx by remember(chat.id) { mutableIntStateOf(0) }

    DisposableEffect(rootView, chat.id) {
        fun updateIme(insets: WindowInsetsCompat?) {
            if (insets == null) return
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            val nav = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            imeLiftPx = (ime - nav).coerceAtLeast(0)
        }

        // Aktuellen Zustand übernehmen, falls die Tastatur schon sichtbar ist.
        updateIme(ViewCompat.getRootWindowInsets(rootView))

        val callback = object : WindowInsetsAnimationCompat.Callback(
            WindowInsetsAnimationCompat.Callback.DISPATCH_MODE_CONTINUE_ON_SUBTREE
        ) {
            override fun onProgress(
                insets: WindowInsetsCompat,
                runningAnimations: MutableList<WindowInsetsAnimationCompat>
            ): WindowInsetsCompat {
                updateIme(insets)
                return insets
            }

            override fun onEnd(animation: WindowInsetsAnimationCompat) {
                updateIme(ViewCompat.getRootWindowInsets(rootView))
            }
        }

        ViewCompat.setWindowInsetsAnimationCallback(rootView, callback)
        ViewCompat.requestApplyInsets(rootView)

        onDispose {
            ViewCompat.setWindowInsetsAnimationCallback(rootView, null)
        }
    }

    val imeLift = with(density) { imeLiftPx.toDp() }
    val keyboardOpen = imeLiftPx > 0

    LaunchedEffect(keyboardOpen, chat.nachrichten.size) {
        if (keyboardOpen && chat.nachrichten.isNotEmpty()) {
            // Kurz nach Beginn der IME-Animation den Nachrichtenverlauf
            // weich an die neue sichtbare Höhe anpassen.
            delay(20)
            listState.animateScrollToItem(chat.nachrichten.lastIndex)
        }
    }

    Scaffold(
        modifier = Modifier.graphicsLayer {
            when (oeffnenAnimation) {
                "Seitlich" -> translationX = (1f - chatAnimProgress) * size.width * 1.15f
                "Von unten" -> translationY = (1f - chatAnimProgress) * size.height * 1.15f
                "Zoom" -> {
                    // V1.7.0: Deutlich sichtbarer Zoom in beide Richtungen.
                    scaleX = 0.76f + 0.24f * chatAnimProgress
                    scaleY = 0.76f + 0.24f * chatAnimProgress
                    alpha = 0.72f + 0.28f * chatAnimProgress
                }
                "Überblenden" -> {
                    // Die Übersicht liegt darunter und wird beim Zurückgehen
                    // während des Ausblendens kontinuierlich freigelegt.
                    alpha = chatAnimProgress
                }
                "Zoom + Überblenden" -> {
                    scaleX = 0.76f + 0.24f * chatAnimProgress
                    scaleY = 0.76f + 0.24f * chatAnimProgress
                    alpha = chatAnimProgress
                }
            }
        },
        topBar = {
            TopAppBar(
                navigationIcon = {
                    TextButton(onClick = { animiertZurueck() }) {
                        Text("‹", fontSize = 32.sp)
                    }
                },
                title = {
                    Row(
                        modifier = Modifier.clickable(enabled = chat.telefon.isNotBlank()) {
                            kontaktOeffnen()
                        },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(40.dp).background(akzentFarbe, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(chat.kuerzel, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(chat.name, fontWeight = FontWeight.Bold)
                            Text(if (chat.telefon.isNotBlank()) chat.telefon else "lokaler Chat", fontSize = 12.sp)
                        }
                    }
                },
                actions = {
                    if (chat.telefon.isNotBlank()) {
                        val unbekannt = chat.name == chat.telefon ||
                            chat.name.filter { it.isDigit() } == chat.telefon.filter { it.isDigit() }

                        Box {
                            TextButton(onClick = { chatMenueOffen = true }) {
                                Text("⋮", fontSize = 24.sp)
                            }
                            DropdownMenu(
                                expanded = chatMenueOffen,
                                onDismissRequest = { chatMenueOffen = false }
                            ) {
                                if (!unbekannt) {
                                    DropdownMenuItem(
                                        text = { Text("Kontakt öffnen") },
                                        onClick = {
                                            chatMenueOffen = false
                                            kontaktOeffnen()
                                        }
                                    )
                                } else {
                                    DropdownMenuItem(
                                        text = { Text("Kontakt erstellen") },
                                        onClick = {
                                            chatMenueOffen = false
                                            val intent = Intent(ContactsContract.Intents.Insert.ACTION).apply {
                                                type = ContactsContract.RawContacts.CONTENT_TYPE
                                                putExtra(ContactsContract.Intents.Insert.PHONE, chat.telefon)
                                            }
                                            kontaktErstellenLauncher.launch(intent)
                                        }
                                    )
                                }
                                DropdownMenuItem(
                                    text = { Text("Anrufen") },
                                    onClick = {
                                        chatMenueOffen = false
                                        val nummer = chat.telefon.filter { it.isDigit() || it == '+' }
                                        context.startActivity(
                                            Intent(Intent.ACTION_DIAL, Uri.parse("tel:$nummer"))
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 4.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .offset(y = -imeLift)
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(
                            WindowInsets.safeDrawing.only(
                                WindowInsetsSides.Horizontal
                            )
                        )
                        .padding(start = 10.dp, end = 10.dp, top = 2.dp, bottom = 6.dp)
                ) {
                    if (anhangUri != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            MmsBildVorschau(anhangUri.toString(), Modifier.size(48.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Bild für MMS ausgewählt", modifier = Modifier.weight(1f), fontSize = 13.sp)
                            TextButton(onClick = { anhangUri = null }) { Text("Entfernen") }
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clickable { bildLauncher.launch(arrayOf("image/*")) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("📎", fontSize = 20.sp)
                        }

                        Spacer(Modifier.width(3.dp))

                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp),
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                            color = Color.Transparent
                        ) {
                            val textFarbe = MaterialTheme.colorScheme.onSurface.toArgb()
                            val hintFarbe = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()

                            AndroidView(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 10.dp),
                                factory = { viewContext ->
                                    RichContentEditText(
                                        context = viewContext,
                                        onTextValueChanged = { eingabe = it },
                                        onRichContent = { uri, mime ->
                                            anhangUri = uri
                                            Toast.makeText(
                                                context,
                                                when {
                                                    mime == "image/gif" -> "GIF für MMS übernommen"
                                                    mime == "image/webp" -> "Sticker/Bild für MMS übernommen"
                                                    else -> "Bild für MMS übernommen"
                                                },
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    ).apply {
                                        setSingleLine(true)
                                        maxLines = 1
                                        gravity = Gravity.CENTER_VERTICAL
                                        inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                                        setBackgroundColor(android.graphics.Color.TRANSPARENT)
                                        setPadding(0, 0, 0, 0)
                                        textSize = 16f
                                    }
                                },
                                update = { editText ->
                                    editText.setTextFromCompose(eingabe)
                                    editText.hint = if (anhangUri == null) "Nachricht" else "Text zur MMS"
                                    editText.setTextColor(textFarbe)
                                    editText.setHintTextColor(hintFarbe)
                                }
                            )
                        }

                        Spacer(Modifier.width(3.dp))

                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .clickable {
                                    val text = eingabe.trim()
                                    val bild = anhangUri
                                    if (text.isNotEmpty() || bild != null) {
                                        if (onNachrichtSenden(text, bild)) {
                                            eingabe = ""
                                            anhangUri = null
                                            scope.launch {
                                                val ziel = chat.nachrichten.size
                                                if (ziel >= 0) listState.animateScrollToItem(ziel)
                                            }
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(if (anhangUri == null) "➤" else "M", fontSize = 16.sp)
                        }
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(bottom = imeLift)
                .onGloballyPositioned { coords ->
                    chatBereichY = coords.positionInRoot().y
                }
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 10.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp),
                contentPadding = PaddingValues(top = 10.dp, bottom = 10.dp)
            ) {
                items(chat.nachrichten.size, key = { pos -> "msg_${chat.id}_${chat.nachrichten[pos].id}_${pos}" }) { pos ->
                    val nachricht = chat.nachrichten[pos]
                    val vorher = if (pos > 0) chat.nachrichten[pos - 1] else null
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { coords ->
                                if (
                                    (overlaySichtbar && overlayNachricht?.id == nachricht.id) ||
                                    (wartetAufEinflug && aktuelleLetzte?.id == nachricht.id)
                                ) {
                                    zielNachrichtY = coords.positionInRoot().y - chatBereichY
                                }
                            }
                            .graphicsLayer {
                                alpha = if (
                                    (overlaySichtbar && overlayNachricht?.id == nachricht.id) ||
                                    (wartetAufEinflug && aktuelleLetzte?.id == nachricht.id)
                                ) 0f else 1f
                            }
                    ) {
                        if (vorher == null || !gleicherTag(vorher.zeitMillis, nachricht.zeitMillis)) DatumTrenner(nachricht.zeitMillis)
                        NachrichtenBlase(
                            nachricht, akzentFarbe, designName, farbeEmpfang, farbeGesendet,
                            schriftgroesse = schriftgroesse,
                            onNachrichtLoeschen = { onNachrichtLoeschen(nachricht) },
                            einflugAnimation = false
                        )
                    }
                }
            }

            val fliegend = overlayNachricht
            if (overlaySichtbar && fliegend != null && !zielNachrichtY.isNaN()) {
                var gestartet by remember(fliegend.id) { mutableStateOf(false) }
                val x by animateDpAsState(
                    targetValue = if (gestartet) 0.dp else if (fliegend.vonMir) (-150).dp else 150.dp,
                    animationSpec = tween(520), label = "overlayX"
                )
                val yEinflug by animateDpAsState(
                    targetValue = if (gestartet) 0.dp else (-300).dp,
                    animationSpec = tween(
                        durationMillis = 650,
                        easing = androidx.compose.animation.core.FastOutSlowInEasing
                    ),
                    label = "overlayY"
                )
                LaunchedEffect(fliegend.id) { gestartet = true }

                // Die Blase wird direkt über die unsichtbare echte Nachricht gelegt.
                // yEinflug ist nur noch der animierte Abstand von oben zu GENAU
                // dieser gemessenen Zielposition.
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(100f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp)
                            .offset(
                                x = x,
                                y = (zielNachrichtY / androidx.compose.ui.platform.LocalDensity.current.density).dp + yEinflug
                            )
                    ) {
                        NachrichtenBlase(
                            fliegend, akzentFarbe, designName, farbeEmpfang, farbeGesendet,
                            schriftgroesse = schriftgroesse,
                            onNachrichtLoeschen = {},
                            einflugAnimation = false
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DatumTrenner(zeitMillis: Long) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
            Text(
                text = datumText(zeitMillis),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun NachrichtenBlase(
    nachricht: Nachricht,
    akzentFarbe: Color,
    designName: String,
    farbeEmpfang: Color,
    farbeGesendet: Color,
    schriftgroesse: Int,
    onNachrichtLoeschen: () -> Unit,
    einflugAnimation: Boolean = true
) {
    val blasenFarbe = if (nachricht.vonMir) farbeGesendet else farbeEmpfang
    val sprechblase = designName == "Sprechblasen"
    val scope = rememberCoroutineScope()
    var sichtbar by remember(nachricht.id) { mutableStateOf(false) }
    val alpha by animateFloatAsState(
        targetValue = if (!einflugAnimation || sichtbar) 1f else 0f,
        animationSpec = tween(320),
        label = "nachrichtAlpha"
    )
    val scale by animateFloatAsState(
        targetValue = if (!einflugAnimation || sichtbar) 1f else 0.88f,
        animationSpec = tween(320),
        label = "nachrichtScale"
    )
    // V1.6.1: Empfang kommt deutlich von rechts oben, gesendet von links oben.
    val startX = if (nachricht.vonMir) (-110).dp else 110.dp
    val einflugX by animateDpAsState(
        targetValue = if (!einflugAnimation || sichtbar) 0.dp else startX,
        animationSpec = tween(480),
        label = "nachrichtEinflugX"
    )
    val einflugY by animateDpAsState(
        targetValue = if (!einflugAnimation || sichtbar) 0.dp else (-260).dp,
        animationSpec = tween(480),
        label = "nachrichtEinflugY"
    )
    LaunchedEffect(nachricht.id) { sichtbar = true }

    var dragX by remember(nachricht.id) { mutableFloatStateOf(0f) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            // Während des Einfliegens über den bereits vorhandenen Nachrichten zeichnen.
            .zIndex(if (sichtbar && (einflugX != 0.dp || einflugY != 0.dp)) 10f else 0f)
            .graphicsLayer {
                this.alpha = alpha
                scaleX = scale
                scaleY = scale
            }
            .pointerInput(nachricht.id) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (dragX > 120f) {
                            sichtbar = false
                            scope.launch {
                                kotlinx.coroutines.delay(180)
                                onNachrichtLoeschen()
                            }
                        }
                        dragX = 0f
                    },
                    onDragCancel = { dragX = 0f },
                    onHorizontalDrag = { _, dragAmount ->
                        dragX = (dragX + dragAmount).coerceAtLeast(0f)
                    }
                )
            }
            .offset(x = einflugX + (dragX / 3f).dp, y = einflugY),
        horizontalArrangement = if (nachricht.vonMir) Arrangement.End else Arrangement.Start
    ) {
        if (!sprechblase) {
            if (!nachricht.vonMir) {
                // Standard Empfang: kompakte Blase, Zeit rechts außen.
                Row(verticalAlignment = Alignment.Bottom) {
                    NachrichtenInhalt(
                        nachricht = nachricht,
                        akzentFarbe = akzentFarbe,
                        blasenFarbe = blasenFarbe,
                        modifier = Modifier.widthIn(max = 300.dp),
                        form = RoundedCornerShape(15.dp),
                        schriftgroesse = schriftgroesse,
                        zeitInnen = false
                    )
                    Spacer(Modifier.width(7.dp))
                    NachrichtenZeitAussen(nachricht, blasenFarbe)
                }
            } else {
                // Standard Gesendet: Zeit/Status links außen, kompakte Blase rechts.
                Row(verticalAlignment = Alignment.Bottom) {
                    NachrichtenZeitAussen(nachricht, blasenFarbe)
                    Spacer(Modifier.width(7.dp))
                    NachrichtenInhalt(
                        nachricht = nachricht,
                        akzentFarbe = akzentFarbe,
                        blasenFarbe = blasenFarbe,
                        modifier = Modifier.widthIn(max = 300.dp),
                        form = RoundedCornerShape(15.dp),
                        schriftgroesse = schriftgroesse,
                        zeitInnen = false
                    )
                }
            }
        } else if (!nachricht.vonMir) {
            // Empfang: kleine, separat gezeichnete Spitze links oben + runde Blase.
            Row(verticalAlignment = Alignment.Bottom) {
                Canvas(
                    modifier = Modifier
                        .align(Alignment.Top)
                        .padding(top = 2.dp)
                        .width(23.dp)
                        .height(19.dp)
                        .offset(x = 4.dp)
                ) {
                    val p = Path().apply {
                        moveTo(0f, 0f)
                        lineTo(size.width, size.height * 0.42f)
                        lineTo(size.width, size.height)
                        close()
                    }
                    drawPath(p, color = blasenFarbe)
                }
                NachrichtenInhalt(
                    nachricht = nachricht,
                    akzentFarbe = akzentFarbe,
                    blasenFarbe = blasenFarbe,
                    modifier = Modifier.widthIn(max = 300.dp),
                    form = RoundedCornerShape(18.dp),
                    zeitInnen = false,
                    schriftgroesse = schriftgroesse
                )
                Spacer(Modifier.width(6.dp))
                NachrichtenZeitAussen(nachricht, blasenFarbe)
            }
        } else {
            // Gesendet: runde Blase + kleine, separat gezeichnete Spitze rechts unten.
            Row(verticalAlignment = Alignment.Bottom) {
                NachrichtenZeitAussen(nachricht, blasenFarbe)
                Spacer(Modifier.width(6.dp))
                NachrichtenInhalt(
                    nachricht = nachricht,
                    akzentFarbe = akzentFarbe,
                    blasenFarbe = blasenFarbe,
                    modifier = Modifier.widthIn(max = 300.dp),
                    form = RoundedCornerShape(18.dp),
                    zeitInnen = false,
                    schriftgroesse = schriftgroesse
                )
                Canvas(
                    modifier = Modifier
                        .padding(bottom = 2.dp)
                        .width(23.dp)
                        .height(19.dp)
                        .offset(x = (-4).dp)
                ) {
                    val p = Path().apply {
                        moveTo(0f, 0f)
                        lineTo(0f, size.height * 0.58f)
                        lineTo(size.width, size.height)
                        close()
                    }
                    drawPath(p, color = blasenFarbe)
                }
            }
        }
    }
}

@Composable
private fun NachrichtenInhalt(
    nachricht: Nachricht,
    akzentFarbe: Color,
    blasenFarbe: Color,
    modifier: Modifier,
    form: androidx.compose.ui.graphics.Shape,
    zeitInnen: Boolean = true,
    schriftgroesse: Int = 16
) {
    Column(
        modifier = modifier
            .background(blasenFarbe, form)
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        if (!nachricht.mmsBildUri.isNullOrBlank()) {
            MmsBildVorschau(
                uriText = nachricht.mmsBildUri,
                modifier = Modifier.widthIn(max = 280.dp).heightIn(max = 280.dp)
            )
            if (nachricht.text.isNotBlank()) Spacer(Modifier.height(6.dp))
        }
        if (nachricht.text.isNotBlank()) {
            SelectionContainer {
                Text(
                    nachricht.text,
                    fontSize = schriftgroesse.sp,
                    lineHeight = (schriftgroesse + 5).sp,
                    color = kontrastFarbe(blasenFarbe)
                )
            }
        }
        if (zeitInnen) {
            Spacer(Modifier.height(3.dp))
            Row(
                modifier = Modifier.align(Alignment.End),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(nachricht.zeitMillis)),
                    fontSize = 11.sp,
                    color = sekundaerTextFarbe(blasenFarbe)
                )
                if (nachricht.vonMir) {
                    Spacer(Modifier.width(4.dp))
                    Text(
                        if (nachricht.status >= 2) "✓✓" else "✓",
                        fontSize = 12.sp,
                        color = kontrastFarbe(blasenFarbe),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun NachrichtenZeitAussen(nachricht: Nachricht, blasenFarbe: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = 4.dp)
    ) {
        Text(
            SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(nachricht.zeitMillis)),
            fontSize = 11.sp,
            color = blasenFarbe,
            fontWeight = FontWeight.Medium
        )
        if (nachricht.vonMir) {
            Spacer(Modifier.width(3.dp))
            Text(
                if (nachricht.status >= 2) "✓✓" else "✓",
                fontSize = 12.sp,
                color = blasenFarbe,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun MmsBildVorschau(uriText: String, modifier: Modifier = Modifier) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var gross by remember(uriText) { mutableStateOf(false) }
    val bitmap = remember(uriText) {
        try {
            context.contentResolver.openInputStream(Uri.parse(uriText))?.use { BitmapFactory.decodeStream(it) }
        } catch (_: Exception) { null }
    }
    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "MMS-Bild",
            contentScale = ContentScale.Crop,
            modifier = modifier.clip(RoundedCornerShape(12.dp)).clickable { gross = true }
        )
        if (gross) {
            AlertDialog(
                onDismissRequest = { gross = false },
                confirmButton = { TextButton(onClick = { gross = false }) { Text("Schließen") } },
                text = {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "MMS-Bild groß",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxWidth().heightIn(max = 620.dp)
                    )
                }
            )
        }
    } else {
        Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
            Text("📷 MMS-Bild", modifier = Modifier.padding(12.dp))
        }
    }
}

private fun kurzeZeit(zeitMillis: Long): String {
    return if (gleicherTag(zeitMillis, System.currentTimeMillis())) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(zeitMillis))
    } else {
        SimpleDateFormat("dd.MM.", Locale.getDefault()).format(Date(zeitMillis))
    }
}

private fun gleicherTag(a: Long, b: Long): Boolean {
    val ca = Calendar.getInstance().apply { timeInMillis = a }
    val cb = Calendar.getInstance().apply { timeInMillis = b }
    return ca.get(Calendar.ERA) == cb.get(Calendar.ERA) &&
        ca.get(Calendar.YEAR) == cb.get(Calendar.YEAR) &&
        ca.get(Calendar.DAY_OF_YEAR) == cb.get(Calendar.DAY_OF_YEAR)
}

private fun datumText(zeitMillis: Long): String {
    val tag = Calendar.getInstance().apply { timeInMillis = zeitMillis }
    val heute = Calendar.getInstance()
    val gestern = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
    return when {
        gleicherTag(tag.timeInMillis, heute.timeInMillis) -> "Heute"
        gleicherTag(tag.timeInMillis, gestern.timeInMillis) -> "Gestern"
        else -> SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(zeitMillis))
    }
}
