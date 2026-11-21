package com.vcode.ide.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.vcode.ide.domain.model.EditorThemeId

@Immutable
data class VCodeColors(
    val isDark: Boolean,
    val activityBarBg: Color,
    val activityBarIconActive: Color,
    val activityBarIconInactive: Color,
    val sideBarBg: Color,
    val sideBarHeaderBg: Color,
    val editorBg: Color,
    val tabBarBg: Color,
    val tabActiveBg: Color,
    val tabInactiveBg: Color,
    val panelBg: Color,
    val statusBarBg: Color,
    val statusBarFg: Color,
    val border: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val accent: Color,
    val selectionBg: Color,
    val error: Color,
    val warning: Color,
    val success: Color
)

val LocalVCodeColors = staticCompositionLocalOf {
    buildVCodeColors(EditorThemeId.DARK_PLUS, Color(0xFF007ACC))
}

fun buildVCodeColors(themeId: EditorThemeId, accent: Color): VCodeColors {
    return when (themeId) {
        EditorThemeId.DARK_PLUS -> VCodeColors(
            isDark = true,
            activityBarBg = Color(0xFF333333),
            activityBarIconActive = Color(0xFFFFFFFF),
            activityBarIconInactive = Color(0xFF858585),
            sideBarBg = Color(0xFF252526),
            sideBarHeaderBg = Color(0xFF2D2D2E),
            editorBg = Color(0xFF1E1E1E),
            tabBarBg = Color(0xFF252526),
            tabActiveBg = Color(0xFF1E1E1E),
            tabInactiveBg = Color(0xFF2D2D2D),
            panelBg = Color(0xFF181818),
            statusBarBg = accent,
            statusBarFg = Color.White,
            border = Color(0xFF3C3C3C),
            textPrimary = Color(0xFFD4D4D4),
            textSecondary = Color(0xFFCCCCCC),
            textMuted = Color(0xFF858585),
            accent = accent,
            selectionBg = Color(0xFF094771),
            error = Color(0xFFF48771),
            warning = Color(0xFFCCA700),
            success = Color(0xFF89D185)
        )
        EditorThemeId.ONE_DARK_PRO -> VCodeColors(
            isDark = true,
            activityBarBg = Color(0xFF21252B),
            activityBarIconActive = Color(0xFFD7DAE0),
            activityBarIconInactive = Color(0xFF6B717D),
            sideBarBg = Color(0xFF21252B),
            sideBarHeaderBg = Color(0xFF282C34),
            editorBg = Color(0xFF282C34),
            tabBarBg = Color(0xFF21252B),
            tabActiveBg = Color(0xFF282C34),
            tabInactiveBg = Color(0xFF21252B),
            panelBg = Color(0xFF1E2227),
            statusBarBg = Color(0xFF21252B),
            statusBarFg = Color(0xFF9DA5B4),
            border = Color(0xFF181A1F),
            textPrimary = Color(0xFFABB2BF),
            textSecondary = Color(0xFFD7DAE0),
            textMuted = Color(0xFF636D83),
            accent = accent,
            selectionBg = Color(0xFF323842),
            error = Color(0xFFE06C75),
            warning = Color(0xFFE5C07B),
            success = Color(0xFF98C379)
        )
        EditorThemeId.DRACULA -> VCodeColors(
            isDark = true,
            activityBarBg = Color(0xFF21222C),
            activityBarIconActive = Color(0xFFF8F8F2),
            activityBarIconInactive = Color(0xFF6272A4),
            sideBarBg = Color(0xFF21222C),
            sideBarHeaderBg = Color(0xFF282A36),
            editorBg = Color(0xFF282A36),
            tabBarBg = Color(0xFF191A21),
            tabActiveBg = Color(0xFF282A36),
            tabInactiveBg = Color(0xFF21222C),
            panelBg = Color(0xFF1E1F29),
            statusBarBg = Color(0xFF191A21),
            statusBarFg = Color(0xFFF8F8F2),
            border = Color(0xFF44475A),
            textPrimary = Color(0xFFF8F8F2),
            textSecondary = Color(0xFFE2E2DC),
            textMuted = Color(0xFF6272A4),
            accent = Color(0xFFBD93F9),
            selectionBg = Color(0xFF44475A),
            error = Color(0xFFFF5555),
            warning = Color(0xFFFFB86C),
            success = Color(0xFF50FA7B)
        )
        EditorThemeId.MONOKAI -> VCodeColors(
            isDark = true,
            activityBarBg = Color(0xFF1E1F1C),
            activityBarIconActive = Color(0xFFF8F8F2),
            activityBarIconInactive = Color(0xFF75715E),
            sideBarBg = Color(0xFF1E1F1C),
            sideBarHeaderBg = Color(0xFF272822),
            editorBg = Color(0xFF272822),
            tabBarBg = Color(0xFF1E1F1C),
            tabActiveBg = Color(0xFF272822),
            tabInactiveBg = Color(0xFF34352F),
            panelBg = Color(0xFF1B1C18),
            statusBarBg = Color(0xFF414339),
            statusBarFg = Color(0xFFF8F8F2),
            border = Color(0xFF3E3D32),
            textPrimary = Color(0xFFF8F8F2),
            textSecondary = Color(0xFFE6E6DF),
            textMuted = Color(0xFF75715E),
            accent = Color(0xFFA6E22E),
            selectionBg = Color(0xFF49483E),
            error = Color(0xFFF92672),
            warning = Color(0xFFE6DB74),
            success = Color(0xFFA6E22E)
        )
        EditorThemeId.LIGHT_PLUS, EditorThemeId.GITHUB_LIGHT -> VCodeColors(
            isDark = false,
            activityBarBg = Color(0xFF2C2C2C),
            activityBarIconActive = Color(0xFFFFFFFF),
            activityBarIconInactive = Color(0xFF9E9E9E),
            sideBarBg = Color(0xFFF3F3F3),
            sideBarHeaderBg = Color(0xFFE8E8E8),
            editorBg = Color(0xFFFFFFFF),
            tabBarBg = Color(0xFFF3F3F3),
            tabActiveBg = Color(0xFFFFFFFF),
            tabInactiveBg = Color(0xFFECECEC),
            panelBg = Color(0xFFF8F8F8),
            statusBarBg = accent,
            statusBarFg = Color.White,
            border = Color(0xFFDADADA),
            textPrimary = Color(0xFF1F2328),
            textSecondary = Color(0xFF424A53),
            textMuted = Color(0xFF6E7781),
            accent = accent,
            selectionBg = Color(0xFFDDF4FF),
            error = Color(0xFFCF222E),
            warning = Color(0xFF9A6700),
            success = Color(0xFF1A7F37)
        )
    }
}

@Composable
fun VCodeTheme(
    themeId: EditorThemeId,
    accentColorHex: Long,
    content: @Composable () -> Unit
) {
    val accent = Color(accentColorHex)
    val vcodeColors = buildVCodeColors(themeId, accent)
    val materialScheme = if (vcodeColors.isDark) {
        darkColorScheme(
            primary = vcodeColors.accent,
            onPrimary = Color.White,
            background = vcodeColors.editorBg,
            onBackground = vcodeColors.textPrimary,
            surface = vcodeColors.sideBarBg,
            onSurface = vcodeColors.textPrimary,
            surfaceVariant = vcodeColors.panelBg,
            onSurfaceVariant = vcodeColors.textSecondary,
            outline = vcodeColors.border,
            error = vcodeColors.error
        )
    } else {
        lightColorScheme(
            primary = vcodeColors.accent,
            onPrimary = Color.White,
            background = vcodeColors.editorBg,
            onBackground = vcodeColors.textPrimary,
            surface = vcodeColors.sideBarBg,
            onSurface = vcodeColors.textPrimary,
            surfaceVariant = vcodeColors.panelBg,
            onSurfaceVariant = vcodeColors.textSecondary,
            outline = vcodeColors.border,
            error = vcodeColors.error
        )
    }

    CompositionLocalProvider(LocalVCodeColors provides vcodeColors) {
        MaterialTheme(colorScheme = materialScheme, content = content)
    }
}
