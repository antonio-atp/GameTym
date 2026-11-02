package pt.gametym.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/** SQLite é local e transacional; não necessita de conta nem ligação à Internet. */
class GameRepository(context: Context) : SQLiteOpenHelper(context, "gametym.db", null, 3) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""CREATE TABLE games (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            name TEXT NOT NULL, platform TEXT NOT NULL, genre TEXT NOT NULL,
            year INTEGER NOT NULL, status TEXT NOT NULL, favorite INTEGER NOT NULL DEFAULT 0, cover_uri TEXT)
        """.trimIndent())
        seed(db)
    }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2 && db.rawQuery("SELECT COUNT(*) FROM games", null).use { it.moveToFirst(); it.getInt(0) == 0 }) seed(db)
        if (oldVersion < 3) db.execSQL("ALTER TABLE games ADD COLUMN cover_uri TEXT")
    }

    private fun seed(db: SQLiteDatabase) {
        val games = listOf(
            Game(name="Hades", platform="PC", genre="Ação", year=2020, status=GameStatus.PLAYING, favorite=true),
            Game(name="Celeste", platform="PC", genre="Plataformas", year=2018, status=GameStatus.COMPLETED),
            Game(name="Hollow Knight", platform="Nintendo Switch", genre="Metroidvania", year=2017),
            Game(name="Stardew Valley", platform="PC", genre="Simulação", year=2016),
            Game(name="Terraria", platform="PC", genre="Aventura", year=2011),
            Game(name="Portal 2", platform="PC", genre="Puzzle", year=2011, status=GameStatus.COMPLETED),
            Game(name="The Witcher 3", platform="PlayStation", genre="RPG", year=2015),
            Game(name="Elden Ring", platform="PlayStation", genre="RPG", year=2022, status=GameStatus.PLAYING),
            Game(name="Fallout 4", platform="Xbox", genre="RPG", year=2015),
            Game(name="Cuphead", platform="Xbox", genre="Plataformas", year=2017))
        games.forEach { game -> db.insertOrThrow("games", null, ContentValues().apply {
            put("name", game.name); put("platform", game.platform); put("genre", game.genre); put("year", game.year)
            put("status", game.status.name); put("favorite", if (game.favorite) 1 else 0)
        }) }
    }

    fun all(): List<Game> = readableDatabase.query("games", null, null, null, null, null, "id DESC").use { c ->
        buildList {
            while (c.moveToNext()) add(Game(
                c.getLong(c.getColumnIndexOrThrow("id")), c.getString(c.getColumnIndexOrThrow("name")),
                c.getString(c.getColumnIndexOrThrow("platform")), c.getString(c.getColumnIndexOrThrow("genre")),
                c.getInt(c.getColumnIndexOrThrow("year")), GameStatus.valueOf(c.getString(c.getColumnIndexOrThrow("status"))),
                c.getInt(c.getColumnIndexOrThrow("favorite")) == 1, c.getString(c.getColumnIndexOrThrow("cover_uri"))
            ))
        }
    }
    fun save(game: Game) {
        require(GameValidation.errors(game.name, game.platform, game.genre, game.year.toString()).isEmpty())
        val values = ContentValues().apply {
            put("name", game.name.trim()); put("platform", game.platform.trim()); put("genre", game.genre.trim())
            put("year", game.year); put("status", game.status.name); put("favorite", if (game.favorite) 1 else 0)
            put("cover_uri", game.coverUri)
        }
        if (game.id == 0L) check(writableDatabase.insertOrThrow("games", null, values) > 0)
        else check(writableDatabase.update("games", values, "id=?", arrayOf(game.id.toString())) == 1)
    }
    fun delete(id: Long) { check(writableDatabase.delete("games", "id=?", arrayOf(id.toString())) == 1) }
}
