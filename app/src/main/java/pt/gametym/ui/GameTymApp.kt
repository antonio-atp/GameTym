package pt.gametym.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.edit
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.clip
import pt.gametym.R
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pt.gametym.data.*

private val Lime = Color(0xFFB6F36B)
private val Ink = Color(0xFF101420)

@Composable
fun GameTymApp() {
    val context = LocalContext.current
    val repo = remember { GameRepository(context.applicationContext) }
    val prefs = remember { context.getSharedPreferences("settings", Context.MODE_PRIVATE) }
    var dark by rememberSaveable { mutableStateOf(prefs.getBoolean("dark", true)) }
    var games by remember { mutableStateOf<List<Game>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var busy by remember { mutableStateOf(false) }
    var page by rememberSaveable { mutableStateOf("collection") }
    var editId by rememberSaveable { mutableLongStateOf(0L) }
    var deleting by remember { mutableStateOf<Game?>(null) }
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(Unit) {
        try { games = withContext(Dispatchers.IO) { repo.all() } }
        catch (e: Exception) { snackbar.showSnackbar("Não foi possível ler a coleção.") }
        finally { loading = false }
    }
    fun mutate(message: String, action: () -> Unit, after: () -> Unit = {}) {
        if (busy) return
        busy = true
        scope.launch {
            val feedback = try {
                games = withContext(Dispatchers.IO) { action(); repo.all() }
                after()
                message
            } catch (e: Exception) { "Não foi possível guardar a alteração. Tente novamente." }
            busy = false
            snackbar.showSnackbar(feedback)
        }
    }
    val colors = if (dark) darkColorScheme(primary = Lime, onPrimary = Ink, background = Ink,
        surface = Color(0xFF191F2E), surfaceVariant = Color(0xFF252D3E))
    else lightColorScheme(primary = Color(0xFF376A13), background = Color(0xFFF5F7F0), surface = Color.White)
    MaterialTheme(colorScheme = colors) {
        BackHandler(page != "collection") { if (!busy) page = "collection" }
        Scaffold(
            snackbarHost = { SnackbarHost(snackbar) },
            bottomBar = { if (page != "form") NavigationBar {
                NavigationBarItem(selected = page == "collection", onClick = { page = "collection" }, icon = { Text("▤", fontSize = 24.sp) }, label = { Text("Coleção") })
                NavigationBarItem(selected = page == "stats", onClick = { page = "stats" }, icon = { Text("▥", fontSize = 24.sp) }, label = { Text("Estatísticas") })
            } },
            floatingActionButton = { if (page == "collection" && !loading) ExtendedFloatingActionButton(
                onClick = { editId = 0; page = "form" }, containerColor = MaterialTheme.colorScheme.primary
            ) { Text("+  Adicionar jogo", fontWeight = FontWeight.Bold) } }
        ) { inset ->
            Column(Modifier.fillMaxSize().padding(inset).padding(horizontal = 20.dp)) {
                Row(Modifier.fillMaxWidth().padding(vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("G A M E T Y M", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black, fontSize = 21.sp)
                        Text("A tua coleção. O teu próximo jogo.", style = MaterialTheme.typography.bodySmall)
                    }
                    TextButton(onClick = { dark = !dark; prefs.edit { putBoolean("dark", dark) } }) { Text(if (dark) "Claro" else "Escuro") }
                }
                when {
                    loading -> CircularProgressIndicator(Modifier.padding(32.dp))
                    page == "form" -> GameForm(editId, games.find { it.id == editId }, busy,
                        onBack = { page = "collection" },
                        onSave = { game -> mutate("Jogo guardado.", { repo.save(game) }, { page = "collection" }) },
                        onDelete = { deleting = it })
                    page == "stats" -> Statistics(games)
                    else -> Collection(games, onEdit = { editId = it.id; page = "form" },
                        onFavorite = { game -> mutate("Favoritos atualizados.", { repo.save(game.copy(favorite = !game.favorite)) }) })
                }
            }
        }
        deleting?.let { game -> AlertDialog(
            onDismissRequest = { if (!busy) deleting = null }, title = { Text("Eliminar jogo?") },
            text = { Text("«${game.name}» será removido da tua coleção. Esta ação é permanente.") },
            confirmButton = { TextButton(enabled = !busy, onClick = {
                mutate("Jogo eliminado.", { repo.delete(game.id) }, { deleting = null; page = "collection" })
            }) { Text("Eliminar", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(enabled = !busy, onClick = { deleting = null }) { Text("Cancelar") } }
        ) }
    }
}

@Composable
private fun Collection(games: List<Game>, onEdit: (Game) -> Unit, onFavorite: (Game) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var status by rememberSaveable { mutableStateOf("Todos") }
    var platform by rememberSaveable { mutableStateOf("Todas") }
    var genre by rememberSaveable { mutableStateOf("Todos") }
    var favorites by rememberSaveable { mutableStateOf(false) }
    var sort by rememberSaveable { mutableStateOf("Recentes") }
    val filtered = games.filter { it.name.contains(query.trim(), ignoreCase = true) &&
        (status == "Todos" || it.status.label == status) && (platform == "Todas" || it.platform == platform) &&
        (genre == "Todos" || it.genre == genre) && (!favorites || it.favorite) }
    val visible = when(sort) { "Nome A-Z" -> filtered.sortedBy { it.name.lowercase() }; "Ano ↓" -> filtered.sortedByDescending { it.year }; "Ano ↑" -> filtered.sortedBy { it.year }; else -> filtered }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 100.dp)) {
        item {
            Text("A minha coleção", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            Text("${games.size} jogos para explorar", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp, bottom = 16.dp))
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Metric("${games.count { it.status == GameStatus.PLAYING }}", "A jogar")
                    Metric("${games.count { it.status == GameStatus.COMPLETED }}", "Concluídos")
                    Metric("${games.count { it.favorite }}", "Favoritos")
                }
            }
        }
        item { OutlinedTextField(value = query, onValueChange = { query = it }, label = { Text("Pesquisar pelo nome") }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) }
        item { Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            (listOf("Todos") + GameStatus.entries.map { it.label }).forEach { label -> FilterChip(selected = status == label, onClick = { status = label }, label = { Text(label) }) }
        } }
        item { Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Choice("Plataforma", platform, listOf("Todas") + games.map { it.platform }.distinct().sorted()) { platform = it }
            Choice("Género", genre, listOf("Todos") + games.map { it.genre }.distinct().sorted()) { genre = it }
            Choice("Ordem", sort, listOf("Recentes", "Nome A-Z", "Ano ↓", "Ano ↑")) { sort = it }
        } }
        item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("${visible.size} resultados", style = MaterialTheme.typography.labelLarge)
            FilterChip(selected = favorites, onClick = { favorites = !favorites }, label = { Text("★ Favoritos") })
        } }
        if (visible.isEmpty()) item { Column(Modifier.fillMaxWidth().padding(vertical = 28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(if (games.isEmpty()) "A próxima aventura começa aqui." else "Nenhum jogo encontrado.", fontWeight = FontWeight.Bold)
            Text(if (games.isEmpty()) "Adiciona o teu primeiro jogo no botão +." else "Experimenta outro nome ou limpa os filtros.", modifier = Modifier.padding(top = 8.dp))
            if (games.isNotEmpty()) TextButton(onClick = { query = ""; status = "Todos"; platform = "Todas"; genre = "Todos"; favorites = false }) { Text("Limpar filtros") }
        } }
        items(visible, key = { it.id }) { game ->
            Card(Modifier.fillMaxWidth().clickable { onEdit(game) }, shape = RoundedCornerShape(18.dp)) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(58.dp, 74.dp).background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                        val cover = covers[game.name.trim().lowercase()]
                        val custom = coverBitmap(game.coverUri)
                        if (custom != null) Image(custom, contentDescription = "Capa de ${game.name}", modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(10.dp)), contentScale = ContentScale.Crop)
                        else if (cover != null) Image(painterResource(cover), contentDescription = "Capa de ${game.name}", modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(10.dp)), contentScale = ContentScale.Crop)
                        else Text(game.name.take(2).uppercase(), fontSize = 24.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                    Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                        Text(game.name, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text("${game.platform} • ${game.year}", style = MaterialTheme.typography.bodySmall)
                        Text(game.genre, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(game.status.label, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 6.dp))
                    }
                    TextButton(onClick = { onFavorite(game) }, modifier = Modifier.width(48.dp), contentPadding = PaddingValues(0.dp)) {
                        Text(if (game.favorite) "★" else "☆", fontSize = 26.sp)
                    }
                }
            }
        }
    }
}

private val covers = mapOf("hades" to R.drawable.cover_hades, "celeste" to R.drawable.cover_celeste,
    "hollow knight" to R.drawable.cover_hollow_knight, "stardew valley" to R.drawable.cover_stardew_valley,
    "terraria" to R.drawable.cover_terraria, "portal 2" to R.drawable.cover_portal_2,
    "the witcher 3" to R.drawable.cover_witcher_3, "elden ring" to R.drawable.cover_elden_ring,
    "fallout 4" to R.drawable.cover_fallout_4, "cuphead" to R.drawable.cover_cuphead)

@Composable
private fun Choice(label: String, value: String, options: List<String>, select: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }) { Text("$label: $value") }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option -> DropdownMenuItem(text = { Text(option) }, onClick = { select(option); expanded = false }) }
        }
    }
}

@Composable
private fun GameForm(id: Long, game: Game?, busy: Boolean, onBack: () -> Unit, onSave: (Game) -> Unit, onDelete: (Game) -> Unit) {
    val context = LocalContext.current
    var coverUri by rememberSaveable(id) { mutableStateOf(game?.coverUri) }
    var coverError by remember { mutableStateOf<String?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                coverUri = uri.toString()
                coverError = null
            } catch (e: SecurityException) { coverError = "Não foi possível guardar o acesso à imagem. Escolhe outra imagem." }
        }
    }
    var name by rememberSaveable(id) { mutableStateOf(game?.name ?: "") }
    var platform by rememberSaveable(id) { mutableStateOf(game?.platform ?: "") }
    var genre by rememberSaveable(id) { mutableStateOf(game?.genre ?: "") }
    var year by rememberSaveable(id) { mutableStateOf(game?.year?.toString() ?: "") }
    var status by rememberSaveable(id) { mutableStateOf(game?.status?.name ?: GameStatus.BACKLOG.name) }
    var favorite by rememberSaveable(id) { mutableStateOf(game?.favorite ?: false) }
    var attempted by rememberSaveable(id) { mutableStateOf(false) }
    val errors = if (attempted) GameValidation.errors(name, platform, genre, year) else emptyMap()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        TextButton(enabled = !busy, onClick = onBack) { Text("‹  Voltar à coleção") }
        Text(if (game == null) "Novo jogo" else "Editar jogo", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Text("Guarda os detalhes da tua próxima aventura.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("Imagem de capa", style = MaterialTheme.typography.titleMedium)
        val preview = coverBitmap(coverUri)
        if (preview != null) Image(preview, contentDescription = "Pré-visualização da capa", modifier = Modifier.size(90.dp, 120.dp).clip(RoundedCornerShape(10.dp)), contentScale = ContentScale.Crop)
        OutlinedButton(enabled = !busy, onClick = { picker.launch(arrayOf("image/*")) }) { Text(if (coverUri == null) "Escolher imagem" else "Alterar imagem") }
        if (coverUri != null) TextButton(enabled = !busy, onClick = { coverUri = null }) { Text("Remover imagem") }
        coverError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        FormField("Nome", name, { name = it }, errors["name"])
        FormField("Plataforma", platform, { platform = it }, errors["platform"])
        Text("Ex.: PC, PlayStation 5, Nintendo Switch", style = MaterialTheme.typography.bodySmall)
        FormField("Género", genre, { genre = it }, errors["genre"])
        FormField("Ano de lançamento", year, { year = it }, errors["year"], true)
        Text("Estado", style = MaterialTheme.typography.titleMedium)
        GameStatus.entries.forEach { option ->
            Row(Modifier.fillMaxWidth().clickable { status = option.name }, verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = status == option.name, onClick = { status = option.name }); Text(option.label)
            }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Adicionar aos favoritos"); Switch(checked = favorite, onCheckedChange = { favorite = it })
        }
        Button(enabled = !busy, modifier = Modifier.fillMaxWidth().height(54.dp), onClick = {
            attempted = true
            if (GameValidation.errors(name, platform, genre, year).isEmpty()) onSave(Game(id, name.trim(), platform.trim(), genre.trim(), year.toInt(), GameStatus.valueOf(status), favorite, coverUri))
        }) { Text(if (busy) "A guardar…" else "Guardar jogo", fontWeight = FontWeight.Bold) }
        if (game != null) OutlinedButton(enabled = !busy, onClick = { onDelete(game) }, modifier = Modifier.fillMaxWidth()) { Text("Eliminar jogo", color = MaterialTheme.colorScheme.error) }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun coverBitmap(uri: String?): androidx.compose.ui.graphics.ImageBitmap? {
    val context = LocalContext.current
    val bitmap by produceState<androidx.compose.ui.graphics.ImageBitmap?>(null, uri) {
        value = withContext(Dispatchers.IO) {
            if (uri == null) null else try {
                val parsed = Uri.parse(uri)
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                context.contentResolver.openInputStream(parsed)?.use { BitmapFactory.decodeStream(it, null, options) }
                var sample = 1
                while (options.outWidth / sample > 600 || options.outHeight / sample > 600) sample *= 2
                val decoded = BitmapFactory.Options().apply { inSampleSize = sample }
                context.contentResolver.openInputStream(parsed)?.use { BitmapFactory.decodeStream(it, null, decoded)?.asImageBitmap() }
            } catch (e: Exception) { null }
        }
    }
    return bitmap
}

@Composable
private fun FormField(label: String, value: String, change: (String) -> Unit, error: String?, numeric: Boolean = false) {
    OutlinedTextField(value = value, onValueChange = change, label = { Text(label) }, isError = error != null,
        supportingText = if (error != null) { { Text(error) } } else null,
        keyboardOptions = KeyboardOptions(keyboardType = if (numeric) KeyboardType.Number else KeyboardType.Text),
        singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
}

@Composable
private fun Metric(value: String, label: String) { Column { Text(value, fontSize = 28.sp, fontWeight = FontWeight.Bold); Text(label, style = MaterialTheme.typography.bodySmall) } }

@Composable
private fun Statistics(games: List<Game>) {
    val completed = games.count { it.status == GameStatus.COMPLETED }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Text("O teu percurso", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Text("Cada jogo conta uma história.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(22.dp)) {
            Text("COLEÇÃO CONCLUÍDA", style = MaterialTheme.typography.labelLarge)
            Text("${if (games.isEmpty()) 0 else completed * 100 / games.size}%", fontSize = 52.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
            LinearProgressIndicator(progress = { if (games.isEmpty()) 0f else completed.toFloat() / games.size }, modifier = Modifier.fillMaxWidth())
            Text("$completed de ${games.size} jogos concluídos", modifier = Modifier.padding(top = 12.dp))
        } }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Metric("${games.size}", "Total de jogos"); Metric("${games.count { it.favorite }}", "Favoritos") }
        Text("Por estado", style = MaterialTheme.typography.titleLarge)
        GameStatus.entries.forEach { state -> Breakdown(state.label, games.count { it.status == state }, games.size) }
        Text("Por plataforma", style = MaterialTheme.typography.titleLarge)
        games.groupingBy { it.platform }.eachCount().toSortedMap().forEach { (label, count) -> Breakdown(label, count, games.size) }
        Text("Por género", style = MaterialTheme.typography.titleLarge)
        games.groupingBy { it.genre }.eachCount().toSortedMap().forEach { (label, count) -> Breakdown(label, count, games.size) }
        if (games.isEmpty()) Text("Adiciona jogos para começares a acompanhar a tua coleção.")
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun Breakdown(label: String, count: Int, total: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(label, modifier = Modifier.weight(1f)); Text(count.toString(), fontWeight = FontWeight.Bold) }
        LinearProgressIndicator(progress = { if (total == 0) 0f else count.toFloat() / total }, modifier = Modifier.fillMaxWidth())
    }
}
