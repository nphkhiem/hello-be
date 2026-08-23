package com.nphkhiem.englishforyourchildren

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HelloBeApp()
        }
    }
}

@Composable
private fun HelloBeApp() {
    MaterialTheme {
        val focusRequester = remember { FocusRequester() }
        val homeContentDescription = stringResource(R.string.home_content_description)
        var isFocused by remember { mutableStateOf(false) }

        LaunchedEffect(focusRequester) {
            focusRequester.requestFocus()
        }

        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(colorResource(R.color.storybook_cream))
                    .padding(horizontal = 64.dp, vertical = 36.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier =
                    Modifier
                        .focusRequester(focusRequester)
                        .onFocusChanged { isFocused = it.isFocused }
                        .focusable()
                        .semantics {
                            contentDescription = homeContentDescription
                        }.background(
                            color = Color(0xFF082A4A),
                            shape = RoundedCornerShape(28.dp)
                        ).border(
                            width = if (isFocused) 6.dp else 2.dp,
                            color = if (isFocused) Color(0xFFFFC857) else Color(0xFF8FB8D8),
                            shape = RoundedCornerShape(28.dp)
                        ).padding(horizontal = 72.dp, vertical = 48.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.app_name),
                    color = Color.White,
                    fontSize = 36.sp
                )
            }
        }
    }
}
