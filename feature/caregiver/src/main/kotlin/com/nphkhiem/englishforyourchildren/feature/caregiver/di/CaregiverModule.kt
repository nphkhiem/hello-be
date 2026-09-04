package com.nphkhiem.englishforyourchildren.feature.caregiver.di

import com.nphkhiem.englishforyourchildren.feature.caregiver.GateChallenges
import com.nphkhiem.englishforyourchildren.feature.caregiver.RandomGateChallenges
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlin.random.Random

/**
 * Where the gate gets its numbers on a real television.
 *
 * The seed is the system's, and it is chosen here rather than inside [RandomGateChallenges] so that
 * the source itself stays a thing a test can pin down. That separation is the reason
 * `GateChallenges` exists at all: a gate reaching for a global random is a gate whose behaviour
 * cannot be asserted, and this one is the only thing between a three year old and their caregiver's
 * settings.
 */
@Module
@InstallIn(SingletonComponent::class)
object CaregiverModule {
    @Provides
    @Singleton
    fun provideGateChallenges(): GateChallenges = RandomGateChallenges(Random.Default)
}
