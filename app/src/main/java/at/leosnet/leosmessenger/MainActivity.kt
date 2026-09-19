package at.leosnet.leosmessenger

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.app.role.RoleManager
import android.app.Service
import android.os.IBinder
import android.os.Build
import android.provider.Settings
import android.net.Uri
import android.provider.Telephony
import android.content.ContentValues
import android.telephony.SmsManager
import android.widget.Toast
import android.provider.ContactsContract
import androidx.activity.result.contract.ActivityResultContracts
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.content.ContextCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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

data class Nachricht(
    val id: Long,
    val text: String,
    val zeitMillis: Long,
    val vonMir: Boolean,
    val status: Int = 2
)

data class Chat(
    val id: Long,
    val name: String,
    val kuerzel: String,
    val nachrichten: List<Nachricht>,
    val telefon: String = ""
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LeosMessengerTheme {
                MessengerApp(applicationContext)
            }
        }
    }
}

class RespondViaMessageService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null
}

class MmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // MMS-Unterstützung folgt in einer späteren Version. Diese Komponente
        // ist nötig, damit Android Leo`s Messenger als SMS-App anbieten kann.
    }
}

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION && intent.action != Telephony.Sms.Intents.SMS_DELIVER_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isEmpty()) return

        val absender = messages.firstOrNull()?.originatingAddress.orEmpty()
        val text = messages.joinToString(separator = "") { it.messageBody.orEmpty() }
        val zeit = messages.firstOrNull()?.timestampMillis ?: System.currentTimeMillis()
        if (absender.isBlank() || text.isBlank()) return

        val chats = ladeChats(context).toMutableList()
        val index = chats.indexOfFirst { gleicheTelefonnummer(it.telefon, absender) }
        val neu = Nachricht(
            id = System.currentTimeMillis(),
            text = text,
            zeitMillis = zeit,
            vonMir = false,
            status = 2
        )

        if (index >= 0) {
            chats[index] = chats[index].copy(nachrichten = chats[index].nachrichten + neu)
        } else {
            chats.add(
                0,
                Chat(
                    id = System.currentTimeMillis(),
                    name = absender,
                    kuerzel = kuerzelAusName(absender),
                    nachrichten = listOf(neu),
                    telefon = absender
                )
            )
        }
        speichereChats(context, chats)
    }
}

private const val PREFS_NAME = "leos_messenger"
private const val PREFS_CHATS = "chats_v102"
private const val PREFS_MESSAGES_ALT = "nachrichten"

private fun startChats(): List<Chat> {
    val jetzt = System.currentTimeMillis()
    return listOf(
        Chat(
            id = 1,
            name = "Testkontakt",
            kuerzel = "T",
            nachrichten = listOf(
                Nachricht(1, "Willkommen bei Leo`s Messenger!", jetzt - 120_000, false),
                Nachricht(2, "Hier kannst du auch nur einen Teil einer Nachricht markieren und kopieren.", jetzt - 60_000, false),
                Nachricht(3, "Probiere es aus: Halte deinen Finger auf diesen Text und markiere nur die Wörter, die du kopieren möchtest.", jetzt, false)
            )
        ),
        Chat(
            id = 2,
            name = "Familie",
            kuerzel = "F",
            nachrichten = listOf(
                Nachricht(21, "Das ist ein zweiter Test-Chat für die neue Chatübersicht.", jetzt - 3_600_000, false)
            )
        ),
        Chat(
            id = 3,
            name = "Notizen",
            kuerzel = "N",
            nachrichten = listOf(
                Nachricht(31, "Hier kannst du später auch Texte ablegen und einzelne Stellen kopieren.", jetzt - 86_400_000, true)
            )
        )
    )
}

private fun ladeAlteNachrichten(context: Context): List<Nachricht>? {
    val gespeichert = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .getString(PREFS_MESSAGES_ALT, null) ?: return null
    return try {
        val array = JSONArray(gespeichert)
        buildList {
            for (i in 0 until array.length()) {
                val o = array.getJSONObject(i)
                add(
                    Nachricht(
                        id = o.getLong("id"),
                        text = o.getString("text"),
                        zeitMillis = o.getLong("zeitMillis"),
                        vonMir = o.getBoolean("vonMir"),
                        status = o.optInt("status", 2)
                    )
                )
            }
        }
    } catch (_: Exception) {
        null
    }
}

private fun ladeChats(context: Context): List<Chat> {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val gespeichert = prefs.getString(PREFS_CHATS, null)

    if (gespeichert == null) {
        val basis = startChats().toMutableList()
        val alt = ladeAlteNachrichten(context)
        if (!alt.isNullOrEmpty()) basis[0] = basis[0].copy(nachrichten = alt)
        speichereChats(context, basis)
        return basis
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
                                status = n.optInt("status", 2)
                            )
                        )
                    }
                }
                add(Chat(c.getLong("id"), c.getString("name"), c.getString("kuerzel"), msgs, c.optString("telefon", "")))
            }
        }
    } catch (_: Exception) {
        startChats()
    }
}

private fun speichereChats(context: Context, chats: List<Chat>) {
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

private fun istStandardSmsApp(context: Context): Boolean =
    Telephony.Sms.getDefaultSmsPackage(context) == context.packageName

private fun importiereSystemSms(context: Context, bestehend: List<Chat>): List<Chat> {
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
        Toast.makeText(context, "SMS-Leseberechtigung fehlt.", Toast.LENGTH_LONG).show()
        return bestehend
    }

    val chats = bestehend.toMutableList()
    var gelesen = 0
    var neuImportiert = 0

    fun kontaktName(nummer: String): String? {
        return try {
            val uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(nummer)
            )
            context.contentResolver.query(
                uri,
                arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME),
                null, null, null
            )?.use { c ->
                if (c.moveToFirst()) c.getString(0) else null
            }
        } catch (_: Exception) { null }
    }

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

        val sortiert = chats.sortedByDescending {
            it.nachrichten.maxOfOrNull { n -> n.zeitMillis } ?: 0L
        }
        speichereChats(context, sortiert)
        Toast.makeText(
            context,
            if (gelesen == 0) "Im Android-SMS-Speicher wurden keine Nachrichten gefunden."
            else "SMS-Speicher gelesen: $gelesen Nachrichten, $neuImportiert neu importiert.",
            Toast.LENGTH_LONG
        ).show()
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

@Composable
fun MessengerApp(context: Context) {
    var standardSms by remember { mutableStateOf(istStandardSmsApp(context)) }
    val chats = remember { mutableStateListOf<Chat>().apply { addAll(ladeChats(context)) } }

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
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_MMS) != PackageManager.PERMISSION_GRANTED) add(Manifest.permission.RECEIVE_MMS)
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_WAP_PUSH) != PackageManager.PERMISSION_GRANTED) add(Manifest.permission.RECEIVE_WAP_PUSH)
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
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_MMS) != PackageManager.PERMISSION_GRANTED) add(Manifest.permission.RECEIVE_MMS)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_WAP_PUSH) != PackageManager.PERMISSION_GRANTED) add(Manifest.permission.RECEIVE_WAP_PUSH)
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
    var offenerChatId by remember { mutableStateOf<Long?>(null) }
    var neuerChatDialog by remember { mutableStateOf(false) }
    var bearbeitenChat by remember { mutableStateOf<Chat?>(null) }
    var loeschenChat by remember { mutableStateOf<Chat?>(null) }

    val offenerChat = chats.firstOrNull { it.id == offenerChatId }

    if (offenerChat == null) {
        ChatUebersicht(
            chats = chats,
            onChatClick = { offenerChatId = it.id },
            onNeuerChat = { neuerChatDialog = true },
            onSmsNeuEinlesen = {
                val lesenErlaubt = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.READ_SMS
                ) == PackageManager.PERMISSION_GRANTED
                if (!lesenErlaubt) {
                    Toast.makeText(context, "SMS-Leseberechtigung fehlt.", Toast.LENGTH_LONG).show()
                } else {
                    val neu = importiereSystemSms(context, ladeChats(context))
                    chats.clear()
                    chats.addAll(neu)
                }
            },
            onBearbeiten = { bearbeitenChat = it },
            onLoeschen = { loeschenChat = it }
        )
    } else {
        BackHandler { offenerChatId = null }
        ChatAnsicht(
            chat = offenerChat,
            onZurueck = { offenerChatId = null },
            onNachrichtSenden = { text ->
                if (sendeSms(context, offenerChat.telefon, text)) {
                    val index = chats.indexOfFirst { it.id == offenerChat.id }
                    if (index >= 0) {
                        val jetzt = System.currentTimeMillis()
                        val neu = Nachricht(
                            id = jetzt,
                            text = text,
                            zeitMillis = jetzt,
                            vonMir = true,
                            status = 1
                        )
                        speichereGesendeteSmsImSystem(context, offenerChat.telefon, text, jetzt)
                        chats[index] = chats[index].copy(
                            nachrichten = chats[index].nachrichten + neu
                        )
                        speichereChats(context, chats)
                    }
                }
            }
        )
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

private fun normalisiereTelefonnummer(nummer: String): String =
    nummer.filter { it.isDigit() }.takeLast(10)

private fun gleicheTelefonnummer(a: String, b: String): Boolean {
    val na = normalisiereTelefonnummer(a)
    val nb = normalisiereTelefonnummer(b)
    return na.length >= 7 && nb.length >= 7 && na == nb
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatUebersicht(
    chats: List<Chat>,
    onChatClick: (Chat) -> Unit,
    onNeuerChat: () -> Unit,
    onSmsNeuEinlesen: () -> Unit,
    onBearbeiten: (Chat) -> Unit,
    onLoeschen: (Chat) -> Unit
) {
    var suche by remember { mutableStateOf("") }
    var hauptmenuOffen by remember { mutableStateOf(false) }
    val gefilterteChats = remember(chats, suche) {
        val q = suche.trim()
        if (q.isBlank()) chats else chats.filter { chat ->
            chat.name.contains(q, ignoreCase = true) ||
                chat.telefon.contains(q, ignoreCase = true) ||
                chat.nachrichten.any { it.text.contains(q, ignoreCase = true) }
        }
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
                                text = { Text("SMS-Verlauf neu einlesen") },
                                onClick = {
                                    hauptmenuOffen = false
                                    onSmsNeuEinlesen()
                                }
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNeuerChat) {
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
                    items(gefilterteChats, key = { it.id }) { chat ->
                        ChatZeile(
                            chat = chat,
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

@Composable
private fun ChatZeile(
    chat: Chat,
    onClick: () -> Unit,
    onBearbeiten: () -> Unit,
    onLoeschen: () -> Unit
) {
    var menuOffen by remember { mutableStateOf(false) }
    val letzte = chat.nachrichten.maxByOrNull { it.zeitMillis }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 14.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .background(MaterialTheme.colorScheme.primary, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(chat.kuerzel, color = MaterialTheme.colorScheme.onPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(chat.name, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                if (letzte != null) {
                    Text(kurzeZeit(letzte.zeitMillis), fontSize = 11.sp, color = Color.Gray)
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = letzte?.text ?: "Noch keine Nachrichten",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                        onLoeschen()
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatAnsicht(
    chat: Chat,
    onZurueck: () -> Unit,
    onNachrichtSenden: (String) -> Unit
) {
    var eingabe by remember(chat.id) { mutableStateOf("") }
    val context = androidx.compose.ui.platform.LocalContext.current
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(chat.id, chat.nachrichten.size) {
        if (chat.nachrichten.isNotEmpty()) listState.scrollToItem(chat.nachrichten.lastIndex)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    TextButton(onClick = onZurueck) {
                        Text("‹", fontSize = 32.sp)
                    }
                },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.primary, CircleShape),
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
                        TextButton(
                            onClick = {
                                val nummer = chat.telefon.filter { it.isDigit() || it == '+' }
                                context.startActivity(
                                    Intent(Intent.ACTION_DIAL, Uri.parse("tel:$nummer")).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                )
                            }
                        ) { Text("Anrufen") }
                    }
                }
            )
        },
        bottomBar = {
            Surface(tonalElevation = 4.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth().navigationBarsPadding().imePadding().padding(8.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    OutlinedTextField(
                        value = eingabe,
                        onValueChange = { eingabe = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Nachricht") },
                        maxLines = 5,
                        shape = RoundedCornerShape(22.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val text = eingabe.trim()
                            if (text.isNotEmpty()) {
                                onNachrichtSenden(text)
                                eingabe = ""
                                scope.launch {
                                    val ziel = chat.nachrichten.size
                                    if (ziel >= 0) listState.animateScrollToItem(ziel)
                                }
                            }
                        },
                        modifier = Modifier.height(56.dp)
                    ) { Text("Senden") }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 10.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
            contentPadding = PaddingValues(top = 10.dp, bottom = 10.dp)
        ) {
            items(chat.nachrichten, key = { it.id }) { nachricht ->
                val index = chat.nachrichten.indexOfFirst { it.id == nachricht.id }
                val vorher = if (index > 0) chat.nachrichten[index - 1] else null
                if (vorher == null || !gleicherTag(vorher.zeitMillis, nachricht.zeitMillis)) {
                    DatumTrenner(nachricht.zeitMillis)
                }
                NachrichtenBlase(nachricht)
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
fun NachrichtenBlase(nachricht: Nachricht) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (nachricht.vonMir) Arrangement.End else Arrangement.Start
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.82f)
                .background(
                    color = if (nachricht.vonMir) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            SelectionContainer {
                Text(
                    text = nachricht.text,
                    fontSize = 16.sp,
                    lineHeight = 21.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(Modifier.height(3.dp))
            Row(modifier = Modifier.align(Alignment.End), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(nachricht.zeitMillis)),
                    fontSize = 11.sp,
                    color = Color.Gray
                )
                if (nachricht.vonMir) {
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = if (nachricht.status >= 2) "✓✓" else "✓",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
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
