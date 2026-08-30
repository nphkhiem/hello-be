package com.nphkhiem.englishforyourchildren

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.nphkhiem.englishforyourchildren.navigation.HelloBeContent
import com.nphkhiem.englishforyourchildren.navigation.HelloBeNavHost
import com.nphkhiem.englishforyourchildren.navigation.ProfileGateway
import com.nphkhiem.englishforyourchildren.navigation.RepositoryProfileGateway
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * The app root.
 *
 * It does two things: put the tokenized theme around everything, and hand the navigation host a
 * way to read profiles and a way to leave. Every other decision belongs to the host or to a screen.
 *
 * The gateway now reads real storage. Until P2-T2 there was no data layer at all, and the app
 * honestly reported that storage could not be read; that is what [UnavailableProfileGateway] was
 * for, and it stays in the source as the thing a build without a database should do.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var gateway: RepositoryProfileGateway

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HelloBeRoot(
                gateway = gateway,
                content = null,
                onExitApp = { finish() }
            )
        }
    }
}

@Composable
internal fun HelloBeRoot(gateway: ProfileGateway, content: HelloBeContent?, onExitApp: () -> Unit) {
    HelloBeTheme {
        // The app root paints the canvas. Every screen sits on the stage rather than on whatever
        // the window happens to be, and the recovery panel and the dialogs, which are cards on a
        // background rather than full-bleed surfaces, would otherwise float on system grey.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(HelloBeTheme.colors.canvas)
        ) {
            HelloBeNavHost(
                gateway = gateway,
                content = content,
                onExitApp = onExitApp
            )
        }
    }
}
