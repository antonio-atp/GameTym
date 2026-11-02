package pt.gametym.data

import org.junit.Assert.*
import org.junit.Test

class GameValidationTest {
    @Test fun validGameHasNoErrors() { assertTrue(GameValidation.errors("Hades", "PC", "Ação", "2020").isEmpty()) }
    @Test fun rejectsBlankFieldsAndNonNumericYear() { assertEquals(setOf("name", "platform", "genre", "year"), GameValidation.errors("  ", "", " ", "abc").keys) }
    @Test fun rejectsUnreasonableYears() {
        assertTrue(GameValidation.errors("A", "PC", "RPG", "1949").containsKey("year"))
        assertTrue(GameValidation.errors("A", "PC", "RPG", "9999").containsKey("year"))
    }
    @Test fun rejectsExcessivelyLongName() { assertTrue(GameValidation.errors("a".repeat(101), "PC", "RPG", "2020").containsKey("name")) }
}
