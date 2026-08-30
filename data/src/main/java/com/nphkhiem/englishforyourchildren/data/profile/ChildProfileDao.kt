package com.nphkhiem.englishforyourchildren.data.profile

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/** Reading and writing the children this television knows. */
@Dao
interface ChildProfileDao {
    /**
     * Every profile, oldest first.
     *
     * The id breaks ties so that two profiles written in the same millisecond still have one order,
     * and that order is the same on every read.
     */
    @Query("SELECT * FROM child_profile ORDER BY createdAt ASC, id ASC")
    fun observeAll(): Flow<List<ChildProfileEntity>>

    @Query("SELECT * FROM child_profile WHERE id = :id")
    suspend fun findById(id: String): ChildProfileEntity?

    /**
     * How many children this television knows.
     *
     * The four-profile limit asks this. Counting the latest emission of [observeAll] would answer
     * from whatever a collector last saw rather than from what is on disk.
     */
    @Query("SELECT COUNT(*) FROM child_profile")
    suspend fun count(): Int

    /**
     * Adds a child, and refuses if that id is already here.
     *
     * ABORT rather than REPLACE: creating and updating are different intentions, and a create that
     * quietly replaced an existing child would lose one without telling anybody.
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(profile: ChildProfileEntity)

    @Update
    suspend fun update(profile: ChildProfileEntity)

    @Query("DELETE FROM child_profile WHERE id = :id")
    suspend fun delete(id: String)
}
