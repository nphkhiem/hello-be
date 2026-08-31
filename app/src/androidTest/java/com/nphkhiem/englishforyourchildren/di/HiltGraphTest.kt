package com.nphkhiem.englishforyourchildren.di

import com.google.common.truth.Truth.assertThat
import com.nphkhiem.englishforyourchildren.domain.repository.CurriculumRepository
import com.nphkhiem.englishforyourchildren.domain.repository.ProfileRepository
import com.nphkhiem.englishforyourchildren.domain.repository.ProgressRepository
import com.nphkhiem.englishforyourchildren.domain.repository.SettingsRepository
import com.nphkhiem.englishforyourchildren.domain.time.TimeProvider
import com.nphkhiem.englishforyourchildren.playback.PlaybackController
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * The production graph, built for real.
 *
 * Every module here assembles itself and `:app` only installs them, so nothing checks that the
 * pieces actually meet until something asks for them. A compile error catches a missing binding;
 * this catches the ones that only fail when Dagger runs, such as a duplicate provided into the
 * same component from two modules.
 */
@HiltAndroidTest
class HiltGraphTest {
    @get:Rule
    val hilt = HiltAndroidRule(this)

    @Inject lateinit var profiles: ProfileRepository

    @Inject lateinit var progress: ProgressRepository

    @Inject lateinit var curriculum: CurriculumRepository

    @Inject lateinit var settings: SettingsRepository

    @Inject lateinit var playback: PlaybackController

    @Inject lateinit var time: TimeProvider

    @Before
    fun injectGraph() {
        hilt.inject()
    }

    @Test
    fun givenTheAppGraph_whenItIsBuilt_thenEveryContractHasSomethingBehindIt() {
        assertThat(profiles).isNotNull()
        assertThat(progress).isNotNull()
        assertThat(curriculum).isNotNull()
        assertThat(settings).isNotNull()
        assertThat(playback).isNotNull()
        assertThat(time).isNotNull()
    }
}
