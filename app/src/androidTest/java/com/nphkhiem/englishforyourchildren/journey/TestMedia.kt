package com.nphkhiem.englishforyourchildren.journey

import android.content.Context
import com.nphkhiem.englishforyourchildren.domain.model.AssetId
import com.nphkhiem.englishforyourchildren.playback.ImageAssetLocator
import com.nphkhiem.englishforyourchildren.playback.Media3PlaybackController
import com.nphkhiem.englishforyourchildren.playback.MediaAssetLocator
import com.nphkhiem.englishforyourchildren.playback.PlaybackController

/**
 * A television with no recordings on it.
 *
 * These journeys are the "missing media, fair skip" stories: a child meets a lesson whose audio
 * will not play, is never scored for it, and is offered the fair way past. They used to get that
 * for free, because no recording existed anywhere. Unit one now ships twenty-eight of them, so the
 * premise has to be stated rather than assumed, or these tests quietly become a different story
 * and then fail as one.
 *
 * Only the finding of files is replaced. The real [Media3PlaybackController] still runs, still
 * reports, and still tells the lesson it has nothing to play, which is the path being tested.
 *
 * Plain functions rather than `@TestInstallIn`, following [TestStorage]: a module that replaces
 * production wiring for the whole test run would hide the real graph from the test that exists to
 * check it.
 */
object TestMedia {
    /** Finds nothing, whatever it is asked for. */
    fun silentAudio(): MediaAssetLocator = MediaAssetLocator { _: AssetId -> null }

    fun noPictures(): ImageAssetLocator = ImageAssetLocator { _: AssetId -> null }

    fun controller(context: Context, locator: MediaAssetLocator): PlaybackController =
        Media3PlaybackController(context, locator)
}
