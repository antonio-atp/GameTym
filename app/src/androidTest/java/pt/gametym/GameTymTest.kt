package pt.gametym

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import pt.gametym.data.*

class GameTymTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun repositoryPersistsEditsAndDeletes() {
        val target = InstrumentationRegistry.getInstrumentation().targetContext
        // Isola a base de dados de teste da coleção que pertence ao utilizador.
        val isolated = object : android.content.ContextWrapper(target) {
            override fun getDatabasePath(name: String) = target.getDatabasePath("test_$name")
            override fun openOrCreateDatabase(name: String, mode: Int, factory: android.database.sqlite.SQLiteDatabase.CursorFactory?) =
                super.openOrCreateDatabase("test_$name", mode, factory)
            override fun openOrCreateDatabase(name: String, mode: Int, factory: android.database.sqlite.SQLiteDatabase.CursorFactory?, errorHandler: android.database.DatabaseErrorHandler?) =
                super.openOrCreateDatabase("test_$name", mode, factory, errorHandler)
        }
        target.deleteDatabase("test_gametym.db")
        val repo = GameRepository(isolated)
        repo.all().forEach { repo.delete(it.id) }
        repo.save(Game(name = "Hades", platform = "PC", genre = "Ação", year = 2020))
        val saved = repo.all().single()
        repo.save(saved.copy(name = "Hades editado", status = GameStatus.COMPLETED, favorite = true, coverUri = "content://example/image/1"))
        repo.close()
        val reopened = GameRepository(isolated)
        assertEquals("Hades editado", reopened.all().single().name)
        assertTrue(reopened.all().single().favorite)
        assertEquals("content://example/image/1", reopened.all().single().coverUri)
        assertEquals(GameStatus.COMPLETED, reopened.all().single().status)
        reopened.delete(saved.id)
        assertTrue(reopened.all().isEmpty())
        reopened.close()
        target.deleteDatabase("test_gametym.db")
    }

    @Test fun formRejectsEmptyInputAndNavigationWorks() {
        compose.waitUntil(15000) { compose.onAllNodesWithText("+  Adicionar jogo").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("+  Adicionar jogo").performClick()
        compose.onNodeWithText("Guardar jogo").performScrollTo().performClick()
        compose.onNodeWithText("Introduza um nome entre 1 e 100 caracteres.").assertExists()
        compose.onNodeWithText("‹  Voltar à coleção").performScrollTo().performClick()
        compose.onNodeWithText("Estatísticas").performClick()
        compose.onNodeWithText("O teu percurso").assertIsDisplayed()
    }

    @Test fun collectionWorkflowAndEvidence() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        GameRepository(context).use { repo ->
            // Apenas no emulador de teste: as capturas usam uma coleção de demonstração.
            repo.all().forEach { repo.delete(it.id) }
            listOf(
                Game(name = "Hollow Knight", platform = "Nintendo Switch", genre = "Metroidvania", year = 2017, status = GameStatus.BACKLOG),
                Game(name = "Celeste", platform = "PC", genre = "Plataformas", year = 2018, status = GameStatus.COMPLETED, favorite = true),
                Game(name = "The Legend of Zelda: Breath of the Wild", platform = "Nintendo Switch", genre = "Aventura", year = 2017, status = GameStatus.COMPLETED),
                Game(name = "Hades", platform = "PC", genre = "Ação", year = 2020, status = GameStatus.PLAYING, favorite = true)
            ).forEach { repo.save(it) }
        }
        compose.activityRule.scenario.recreate()
        compose.waitUntil(15000) { compose.onAllNodesWithText("Hades").fetchSemanticsNodes().isNotEmpty() }
        capture("01-colecao")
        compose.onNodeWithText("Pesquisar pelo nome").performTextInput("Hades")
        compose.onNode(hasText("Hades") and !hasSetTextAction()).assertExists()
        compose.onNodeWithText("Celeste").assertDoesNotExist()
        capture("02-pesquisa")
        compose.onNode(hasText("Hades") and !hasSetTextAction()).performClick()
        capture("03-editar")
        compose.onNodeWithText("Nome", substring = false).performTextReplacement("Hades II")
        compose.onNodeWithText("Guardar jogo").performScrollTo().performClick()
        compose.waitUntil(10000) { compose.onAllNodesWithText("A minha coleção").fetchSemanticsNodes().isNotEmpty() }
        compose.waitUntil(15000) { compose.onAllNodesWithText("Jogo guardado.").fetchSemanticsNodes().isEmpty() }
        compose.onNodeWithText("Pesquisar pelo nome").performTextClearance()
        compose.onNodeWithText("Hades II").assertExists()
        compose.onNodeWithText("Hades II").performClick()
        compose.onNodeWithText("Eliminar jogo").performScrollTo().performClick()
        compose.onNodeWithText("Cancelar").performClick()
        compose.onNodeWithText("Editar jogo").assertExists()
        compose.onNodeWithText("Eliminar jogo").performScrollTo().performClick()
        compose.onNodeWithText("Eliminar", substring = false).performClick()
        compose.waitUntil(10000) { compose.onAllNodesWithText("A minha coleção").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Hades II").assertDoesNotExist()
        compose.onNodeWithText("Estatísticas").performClick()
        capture("04-estatisticas")
        compose.onNodeWithText("Claro", substring = false).performClick()
        capture("05-modo-claro")
        compose.onNodeWithText("Escuro", substring = false).performClick()
    }

    private fun capture(name: String) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val activity = compose.activity
            (activity.getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager)
                .hideSoftInputFromWindow(activity.window.decorView.windowToken, 0)
            activity.window.decorView.clearFocus()
        }
        compose.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val directory = java.io.File(instrumentation.targetContext.getExternalFilesDir(null), "evidencias").apply { mkdirs() }
        val bitmap = instrumentation.uiAutomation.takeScreenshot()
        java.io.File(directory, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        // Galeria pública do emulador: as evidências sobrevivem à desinstalação automática do teste.
        if (android.os.Build.VERSION.SDK_INT >= 29) {
            val values = android.content.ContentValues().apply {
                put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, "$name.png")
                put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(android.provider.MediaStore.Images.Media.RELATIVE_PATH, "Pictures/GameTym")
            }
            val resolver = instrumentation.targetContext.contentResolver
            val uri = requireNotNull(resolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values))
            resolver.openOutputStream(uri)!!.use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
        bitmap.recycle()
    }
}
