package pt.gametym.data

import java.time.Year

enum class GameStatus(val label: String) { BACKLOG("Por Jogar"), PLAYING("A Jogar"), COMPLETED("Concluído") }

data class Game(
    val id: Long = 0,
    val name: String,
    val platform: String,
    val genre: String,
    val year: Int,
    val status: GameStatus = GameStatus.BACKLOG,
    val favorite: Boolean = false,
    val coverUri: String? = null
)

object GameValidation {
    fun errors(name: String, platform: String, genre: String, year: String): Map<String, String> = buildMap {
        if (name.trim().isEmpty() || name.trim().length > 100) put("name", "Introduza um nome entre 1 e 100 caracteres.")
        if (platform.trim().isEmpty() || platform.trim().length > 60) put("platform", "Introduza uma plataforma (até 60 caracteres).")
        if (genre.trim().isEmpty() || genre.trim().length > 60) put("genre", "Introduza um género (até 60 caracteres).")
        val value = year.toIntOrNull()
        if (value == null || value !in 1950..Year.now().value + 5) put("year", "Ano entre 1950 e ${Year.now().value + 5}.")
    }
}
