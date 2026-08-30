package com.nphkhiem.englishforyourchildren.data.profile

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.nphkhiem.englishforyourchildren.data.local.HelloBeDatabase
import com.nphkhiem.englishforyourchildren.domain.model.AgeBand
import com.nphkhiem.englishforyourchildren.domain.model.AvatarId
import com.nphkhiem.englishforyourchildren.domain.model.ProfileId
import com.nphkhiem.englishforyourchildren.testsupport.DomainBuilders
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChildProfileDaoTest {
    private lateinit var database: HelloBeDatabase
    private lateinit var dao: ChildProfileDao

    @Before
    fun openDatabase() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, HelloBeDatabase::class.java).build()
        dao = database.childProfileDao()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun givenAProfile_whenItIsStoredAndReadBack_thenNothingAboutTheChildHasChanged() {
        val minh = DomainBuilders.childProfile(
            id = ProfileId(FIRST),
            nickname = "Minh",
            ageBand = AgeBand.FOUR,
            avatarId = AvatarId("rabbit")
        )

        runBlocking { dao.insert(minh.toEntity(createdAt = EARLY)) }
        val readBack = runBlocking { dao.observeAll().first() }.single().toDomain()

        assertThat(readBack).isEqualTo(minh)
    }

    @Test
    fun givenSeveralProfiles_whenTheyAreObserved_thenTheyComeBackInTheOrderTheyWereAdded() {
        // Without a stored order SQLite may return rows in any order, so a child's face could move
        // between launches and the entry focus with it. The picker is the first thing a
        // pre-reader recognises, so where it sits has to be the same every time.
        runBlocking {
            dao.insert(profile(FIRST, "Minh").toEntity(createdAt = EARLY))
            dao.insert(profile(SECOND, "Lan").toEntity(createdAt = LATE))
            dao.insert(profile(THIRD, "An").toEntity(createdAt = LATER))
        }

        val order = runBlocking { dao.observeAll().first() }.map { it.nickname }

        assertThat(order).containsExactly("Minh", "Lan", "An").inOrder()
    }

    @Test
    fun givenTwoProfilesMadeInTheSameMillisecond_whenObserved_thenTheirOrderIsStillStable() {
        runBlocking {
            dao.insert(profile(SECOND, "Lan").toEntity(createdAt = EARLY))
            dao.insert(profile(FIRST, "Minh").toEntity(createdAt = EARLY))
        }

        val first = runBlocking { dao.observeAll().first() }.map { it.id }
        val second = runBlocking { dao.observeAll().first() }.map { it.id }

        assertThat(first).isEqualTo(second)
        assertThat(first).containsExactly(FIRST, SECOND).inOrder()
    }

    @Test
    fun givenAnExistingProfile_whenTheSameIdIsInsertedAgain_thenItIsRefusedRatherThanOverwritten() {
        // Creating and updating are different intentions. A create that quietly replaced a child
        // would lose one, and nothing would report it.
        runBlocking { dao.insert(profile(FIRST, "Minh").toEntity(createdAt = EARLY)) }

        val second = runCatching {
            runBlocking { dao.insert(profile(FIRST, "Lan").toEntity(createdAt = LATE)) }
        }

        assertThat(second.isFailure).isTrue()
        val stored = runBlocking { dao.observeAll().first() }.single()
        assertThat(stored.nickname).isEqualTo("Minh")
    }

    @Test
    fun givenAProfile_whenItIsUpdated_thenTheStoredRowChanges() {
        runBlocking { dao.insert(profile(FIRST, "Minh").toEntity(createdAt = EARLY)) }

        runBlocking { dao.update(profile(FIRST, "Minh Anh").toEntity(createdAt = EARLY)) }

        val stored = runBlocking { dao.observeAll().first() }.single()
        assertThat(stored.nickname).isEqualTo("Minh Anh")
    }

    @Test
    fun givenAProfile_whenItIsDeleted_thenItIsGoneAndTheOthersRemain() {
        runBlocking {
            dao.insert(profile(FIRST, "Minh").toEntity(createdAt = EARLY))
            dao.insert(profile(SECOND, "Lan").toEntity(createdAt = LATE))
        }

        runBlocking { dao.delete(FIRST) }

        val left = runBlocking { dao.observeAll().first() }.map { it.nickname }
        assertThat(left).containsExactly("Lan")
    }

    @Test
    fun givenProfiles_whenTheyAreCounted_thenTheDatabaseAnswersRatherThanTheCaller() {
        // The four-profile cap in P2-T2 asks this. Counting a Flow's latest emission would answer
        // from whatever was last collected rather than from what is on disk now.
        runBlocking {
            assertThat(dao.count()).isEqualTo(0)
            dao.insert(profile(FIRST, "Minh").toEntity(createdAt = EARLY))
            dao.insert(profile(SECOND, "Lan").toEntity(createdAt = LATE))
            assertThat(dao.count()).isEqualTo(2)
        }
    }

    @Test
    fun givenAProfileIsAdded_whenSomeoneIsAlreadyObserving_thenTheyAreToldWithoutAsking() {
        runBlocking { dao.insert(profile(FIRST, "Minh").toEntity(createdAt = EARLY)) }

        val before = runBlocking { dao.observeAll().first() }
        runBlocking { dao.insert(profile(SECOND, "Lan").toEntity(createdAt = LATE)) }
        val after = runBlocking { dao.observeAll().first() }

        assertThat(before).hasSize(1)
        assertThat(after).hasSize(2)
    }

    @Test
    fun givenARowWithAnUnknownAgeBand_whenItIsMapped_thenItFailsRatherThanGuessing() {
        // Stored as a name rather than an ordinal precisely so this breaks loudly. A row that
        // cannot become a valid child is corrupt, and corruption should reach the caregiver
        // recovery rather than appear on the picker as a child nobody created.
        val corrupt = ChildProfileEntity(
            id = FIRST,
            nickname = "Minh",
            ageBand = "SEVENTEEN",
            avatarId = "rabbit",
            createdAt = EARLY
        )

        val mapped = runCatching { corrupt.toDomain() }

        assertThat(mapped.isFailure).isTrue()
    }

    @Test
    fun givenARowWithABlankId_whenItIsMapped_thenItFailsRatherThanProducingANamelessChild() {
        val corrupt = ChildProfileEntity(
            id = "",
            nickname = "Minh",
            ageBand = AgeBand.THREE.name,
            avatarId = "rabbit",
            createdAt = EARLY
        )

        val mapped = runCatching { corrupt.toDomain() }

        assertThat(mapped.exceptionOrNull()).isInstanceOf(IllegalArgumentException::class.java)
    }

    private fun profile(id: String, nickname: String) =
        DomainBuilders.childProfile(id = ProfileId(id), nickname = nickname)

    private companion object {
        const val FIRST = "p1"
        const val SECOND = "p2"
        const val THIRD = "p3"
        const val EARLY = 1_756_000_000_000
        const val LATE = 1_756_000_001_000
        const val LATER = 1_756_000_002_000
    }
}
