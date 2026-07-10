package io.github.beduality.terracotta.core.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.assertThrows

/**
 * Property-based tests for enum fromId() methods across all three enums.
 *
 * Tests universal invariants (round-trip, case normalization, invalid input rejection)
 * in a parametric way without requiring external PBT libraries.
 *
 * **Validates: Requirements 2.1, 2.2, 2.3, 2.4, 2.5**
 */
class EnumFromIdPropertyTest {
    /**
     * Describes an enum entry for parametric testing — stores the entry identity,
     * its canonical ID, and the fromId lookup function.
     */
    private data class EnumEntry(
        val enumName: String,
        val entryName: String,
        val id: String,
        val fromId: (String) -> Any,
        val expectedEntry: Any,
        val allIds: List<String>,
    )

    private val allEnumEntries: List<EnumEntry> =
        buildList {
            TerracottaEnvironment.entries.forEach { entry ->
                add(
                    EnumEntry(
                        enumName = "TerracottaEnvironment",
                        entryName = entry.name,
                        id = entry.id,
                        fromId = { TerracottaEnvironment.fromId(it) },
                        expectedEntry = entry,
                        allIds = TerracottaEnvironment.entries.map { it.id },
                    ),
                )
            }
            TerracottaLoader.entries.forEach { entry ->
                add(
                    EnumEntry(
                        enumName = "TerracottaLoader",
                        entryName = entry.name,
                        id = entry.id,
                        fromId = { TerracottaLoader.fromId(it) },
                        expectedEntry = entry,
                        allIds = TerracottaLoader.entries.map { it.id },
                    ),
                )
            }
            TerracottaReleaseType.entries.forEach { entry ->
                add(
                    EnumEntry(
                        enumName = "TerracottaReleaseType",
                        entryName = entry.name,
                        id = entry.id,
                        fromId = { TerracottaReleaseType.fromId(it) },
                        expectedEntry = entry,
                        allIds = TerracottaReleaseType.entries.map { it.id },
                    ),
                )
            }
        }

    /**
     * Property 1: Enum fromId round-trip
     *
     * For every entry in each enum, `fromId(entry.id)` returns that entry.
     */
    @TestFactory
    fun `Property 1 - fromId round-trip holds for all entries`(): List<DynamicTest> =
        allEnumEntries.map { entry ->
            DynamicTest.dynamicTest("${entry.enumName}.fromId(\"${entry.id}\") == ${entry.entryName}") {
                val result = entry.fromId(entry.id)
                assertEquals(entry.expectedEntry, result)
            }
        }

    /**
     * Property 2: Enum fromId case normalization
     *
     * For any case variation of a valid ID, `fromId()` returns the same result.
     */
    @TestFactory
    fun `Property 2 - fromId case normalization holds for all case variations`(): List<DynamicTest> =
        allEnumEntries.flatMap { entry ->
            val caseVariations = generateCaseVariations(entry.id)
            caseVariations.map { variation ->
                DynamicTest.dynamicTest(
                    "${entry.enumName}.fromId(\"$variation\") == ${entry.entryName}",
                ) {
                    val result = entry.fromId(variation)
                    assertEquals(entry.expectedEntry, result)
                }
            }
        }

    /**
     * Property 3: Enum fromId invalid input rejection
     *
     * Invalid strings throw IllegalArgumentException with message containing
     * the input and all supported values.
     */
    @TestFactory
    fun `Property 3 - fromId rejects invalid inputs with descriptive message`(): List<DynamicTest> {
        val invalidInputs =
            listOf(
                "invalid_value",
                "not_a_real_id",
                "123",
                "null",
                "INVALID",
            )

        data class EnumFromId(
            val name: String,
            val fromId: (String) -> Any,
            val allIds: List<String>,
        )

        val enumFromIds =
            listOf(
                EnumFromId(
                    name = "TerracottaEnvironment",
                    fromId = { TerracottaEnvironment.fromId(it) },
                    allIds = TerracottaEnvironment.entries.map { it.id },
                ),
                EnumFromId(
                    name = "TerracottaLoader",
                    fromId = { TerracottaLoader.fromId(it) },
                    allIds = TerracottaLoader.entries.map { it.id },
                ),
                EnumFromId(
                    name = "TerracottaReleaseType",
                    fromId = { TerracottaReleaseType.fromId(it) },
                    allIds = TerracottaReleaseType.entries.map { it.id },
                ),
            )

        return enumFromIds.flatMap { enum ->
            invalidInputs.map { invalidInput ->
                DynamicTest.dynamicTest(
                    "${enum.name}.fromId(\"$invalidInput\") throws IllegalArgumentException",
                ) {
                    val ex =
                        assertThrows<IllegalArgumentException> {
                            enum.fromId(invalidInput)
                        }
                    val message = ex.message!!
                    assertTrue(
                        message.contains(invalidInput),
                        "Exception message should contain '$invalidInput' but was: $message",
                    )
                    enum.allIds.forEach { supportedId ->
                        assertTrue(
                            message.contains(supportedId),
                            "Exception message should contain supported value '$supportedId' but was: $message",
                        )
                    }
                }
            }
        }
    }

    /**
     * Generates multiple case variations of a string to test case normalization.
     */
    private fun generateCaseVariations(input: String): List<String> {
        val variations = mutableListOf<String>()
        // All uppercase
        variations.add(input.uppercase())
        // First char uppercase
        variations.add(input.replaceFirstChar { it.uppercase() })
        // Alternating case (even=upper, odd=lower)
        variations.add(
            input.mapIndexed { index, c ->
                if (index % 2 == 0) c.uppercaseChar() else c.lowercaseChar()
            }.joinToString(""),
        )
        // All uppercase except last char
        if (input.length > 1) {
            variations.add(input.dropLast(1).uppercase() + input.last().lowercaseChar())
        }
        return variations.distinct()
    }
}
