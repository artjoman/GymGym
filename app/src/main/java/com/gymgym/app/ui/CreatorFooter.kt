package com.gymgym.app.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp

private const val CREATOR_URL = "https://projectorum.com/"

// Exact wording is fixed (not localized) so it reads identically on every locale.
private const val FOOTER_PREFIX = "Built by people who train and work hard. By "
private const val FOOTER_LINK = "projectorum.com"
private const val FOOTER_SUFFIX = " with ❤️"

/**
 * Creator credit. Only "projectorum.com" is a link — tapping it opens the site in
 * the device's default browser; the rest of the sentence is plain, centered text.
 */
@Composable
fun CreatorFooter(modifier: Modifier = Modifier) {
    val linkColor = MaterialTheme.colorScheme.primary
    val text = buildAnnotatedString {
        append(FOOTER_PREFIX)
        withLink(
            LinkAnnotation.Url(
                url = CREATOR_URL,
                styles = TextLinkStyles(
                    style = SpanStyle(
                        color = linkColor,
                        fontWeight = FontWeight.SemiBold,
                        textDecoration = TextDecoration.Underline,
                    ),
                ),
            ),
        ) {
            append(FOOTER_LINK)
        }
        append(FOOTER_SUFFIX)
    }
    Text(
        text = text,
        modifier = modifier.fillMaxWidth().padding(vertical = 8.dp),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
}
