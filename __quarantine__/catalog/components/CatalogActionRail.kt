package com.zak.pressmark.feature.catalog.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * Single source of truth for rail state so search/add cannot conflict.
 */
enum class RailMode { Idle, Search, Add }

@Stable
data class RailDimens(
    // overlay positioning
    val outerPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
    val bottomMarginAboveNavBar: Dp = 10.dp,

    // gap between left (add) and right (search) controls when expanded
    val controlGap: Dp = 12.dp,

    // square buttons / capsules baseline
    val controlHeight: Dp = 48.dp,
    val squareCornerRadius: Dp = 12.dp,
    val iconSize: Dp = 22.dp,

    // add capsule (two-phase)
    val addCollapsedWidth: Dp = 48.dp,
    val addExpandedWidth: Dp = 176.dp,
    val addWidthAnimMs: Int = 170,
    val addStackDelayMs: Long = 150,

    // stack
    val stackSpacing: Dp = 10.dp,
    val stackItemHeight: Dp = 44.dp,
    val stackItemCornerRadius: Dp = 12.dp,
    val stackItemHorizontalPadding: Dp = 12.dp,
    val stackOffsetAboveCapsule: Dp = 10.dp,

    // search capsule
    val searchCollapsedWidth: Dp = 48.dp,
    val searchWidthAnimMs: Int = 170,
    val searchInnerHorizontalPadding: Dp = 12.dp,
    val searchIconToTextGap: Dp = 10.dp,
)

@Stable
data class RailColors(
    // add capsule + square buttons
    val capsuleContainer: Color,
    val capsuleContent: Color,
    val capsuleBorder: Color,

    // stack items
    val stackItemContainer: Color,
    val stackItemContent: Color,
    val stackItemBorder: Color,

    // search field
    val searchContainer: Color,
    val searchText: Color,
    val searchPlaceholder: Color,
    val searchBorder: Color,
    val searchCursor: Color,
    val searchClearIcon: Color,

    // scrim when expanded
    val scrim: Color,
)

@Stable
data class RailTypography(
    val capsuleTextStyle: TextStyle,
    val stackItemTextStyle: TextStyle,

    val searchTextStyle: TextStyle,
    val searchPlaceholderStyle: TextStyle,
)

@Stable
data class RailShapes(
    val capsuleShape: Shape,
    val stackItemShape: Shape,
)

@Stable
data class RailConfig(
    val dimens: RailDimens = RailDimens(),
    val shapes: RailShapes = RailShapes(
        capsuleShape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        stackItemShape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
    ),

    // Optional. If null, defaults are taken from MaterialTheme in a @Composable-safe way.
    val colors: RailColors? = null,
    val typography: RailTypography? = null,

    // Labels
    val bulkAddLabel: String = "Bulk Add",
    val scanBarcodeLabel: String = "Scan Barcode",
    val addAlbumLabel: String = "Add Album",
    val searchPlaceholder: String = "Search…",

    // Animation tuning
    val stackEnterMs: Int = 140,
    val stackExitMs: Int = 120,
)

@Composable
fun railColorsDefaults(
    scrimAlpha: Float = 0.10f,
): RailColors {
    val cs = MaterialTheme.colorScheme
    return RailColors(
        capsuleContainer = cs.surface,
        capsuleContent = cs.onSurface,
        capsuleBorder = cs.outlineVariant,

        stackItemContainer = cs.surface,
        stackItemContent = cs.onSurface,
        stackItemBorder = cs.outlineVariant,

        searchContainer = cs.surface,
        searchText = cs.onSurface,
        searchPlaceholder = cs.onSurfaceVariant,
        searchBorder = cs.outlineVariant,
        searchCursor = cs.primary,
        searchClearIcon = cs.onSurfaceVariant,

        scrim = cs.scrim.copy(alpha = scrimAlpha),
    )
}

@Composable
fun railTypographyDefaults(): RailTypography {
    val ty = MaterialTheme.typography
    return RailTypography(
        capsuleTextStyle = ty.labelLarge,
        stackItemTextStyle = ty.labelLarge,
        searchTextStyle = ty.bodyMedium,
        searchPlaceholderStyle = ty.bodyMedium,
    )
}

/**
 * Bottom overlay rail (use in a screen overlay Box).
 *
 * Left: Add capsule (expands rightward), then stack expands upward.
 * Right: Search capsule (expands leftward until it hits Add capsule).
 */
@Composable
fun CatalogActionRail(
    mode: RailMode,
    onModeChange: (RailMode) -> Unit,

    // search state
    query: String,
    onQueryChange: (String) -> Unit,

    // icons
    addIcon: ImageVector,
    searchIcon: ImageVector,
    clearIcon: ImageVector,
    bulkIcon: ImageVector,
    scanIcon: ImageVector,
    addAlbumIcon: ImageVector,

    // actions
    onBulkAdd: () -> Unit,
    onScanBarcode: () -> Unit,
    onAddAlbum: () -> Unit,

    modifier: Modifier = Modifier,
    config: RailConfig = RailConfig(),
) {
    val cfg by rememberUpdatedState(config)
    val d = cfg.dimens
    val shapes = cfg.shapes

    val colors = cfg.colors ?: railColorsDefaults()
    val typography = cfg.typography ?: railTypographyDefaults()

    // Phase-gated reveal for the add stack.
    var showAddStack by remember { mutableStateOf(false) }
    LaunchedEffect(mode) {
        if (mode == RailMode.Add) {
            showAddStack = false
            delay(d.addStackDelayMs)
            showAddStack = true
        } else {
            showAddStack = false
        }
    }

    Box(modifier = modifier) {
        // Scrim dismiss
        if (mode != RailMode.Idle) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colors.scrim)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                        role = Role.Button,
                    ) { onModeChange(RailMode.Idle) }
            )
        }

        // Rail container aligned above system nav area.
        BoxWithConstraints(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.ime)   // lifts above keyboard
                .windowInsetsPadding(WindowInsets.navigationBars) // lifts above gesture bar when no IME
                .padding(d.outerPadding)
                .padding(bottom = 4.dp),
        ) {
            // Width available inside the rail container (after outer padding).
            val railWidth = maxWidth

            // When search is expanded, Add is collapsed by definition (mode = Search).
            val leftAnchorWidth = d.addCollapsedWidth

            // Search expanded width: fill from right edge until it reaches Add, with a gap.
            val maxSearchExpandedWidth = (railWidth - leftAnchorWidth - d.controlGap)
                .coerceAtLeast(d.searchCollapsedWidth)

            AddRailLeft(
                mode = mode,
                onModeChange = onModeChange,
                addIcon = addIcon,
                bulkIcon = bulkIcon,
                scanIcon = scanIcon,
                addAlbumIcon = addAlbumIcon,
                onBulkAdd = onBulkAdd,
                onScanBarcode = onScanBarcode,
                onAddAlbum = onAddAlbum,
                showAddStack = showAddStack,
                colors = colors,
                typography = typography,
                shapes = shapes,
                config = cfg,
                modifier = Modifier.align(Alignment.BottomStart),
            )

            SearchRailRight(
                mode = mode,
                onModeChange = onModeChange,
                query = query,
                onQueryChange = onQueryChange,
                placeholder = cfg.searchPlaceholder,
                searchIcon = searchIcon,
                clearIcon = clearIcon,
                colors = colors,
                typography = typography,
                shapes = shapes,
                collapsedWidth = d.searchCollapsedWidth,
                expandedWidth = maxSearchExpandedWidth,
                height = d.controlHeight,
                widthAnimMs = d.searchWidthAnimMs,
                innerHpad = d.searchInnerHorizontalPadding,
                iconToTextGap = d.searchIconToTextGap,
                iconSize = d.iconSize,
                modifier = Modifier.align(Alignment.BottomEnd),
            )
        }
    }
}

@Composable
private fun AddRailLeft(
    mode: RailMode,
    onModeChange: (RailMode) -> Unit,
    addIcon: ImageVector,
    bulkIcon: ImageVector,
    scanIcon: ImageVector,
    addAlbumIcon: ImageVector,
    onBulkAdd: () -> Unit,
    onScanBarcode: () -> Unit,
    onAddAlbum: () -> Unit,
    showAddStack: Boolean,
    colors: RailColors,
    typography: RailTypography,
    shapes: RailShapes,
    config: RailConfig,
    modifier: Modifier = Modifier,
) {
    val d = config.dimens
    val isAdd = mode == RailMode.Add

    val targetWidth = if (isAdd) d.addExpandedWidth else d.addCollapsedWidth
    val addWidth by animateDpAsState(
        targetValue = targetWidth,
        animationSpec = tween(durationMillis = d.addWidthAnimMs),
        label = "addWidth",
    )

    Box(modifier = modifier) {
        // Upward stack
        AnimatedVisibility(
            visible = showAddStack && isAdd,
            enter = fadeIn(tween(120)) + expandVertically(
                expandFrom = Alignment.Bottom,
                animationSpec = tween(config.stackEnterMs),
            ),
            exit = fadeOut(tween(100)) + shrinkVertically(
                shrinkTowards = Alignment.Bottom,
                animationSpec = tween(config.stackExitMs),
            ),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(bottom = d.controlHeight + d.stackOffsetAboveCapsule),
        ) {
            Column(
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(d.stackSpacing),
            ) {
                StackAction(
                    icon = bulkIcon,
                    label = config.bulkAddLabel,
                    onClick = { onModeChange(RailMode.Idle); onBulkAdd() },
                    colors = colors,
                    typography = typography,
                    shapes = shapes,
                    config = config,
                )
                StackAction(
                    icon = scanIcon,
                    label = config.scanBarcodeLabel,
                    onClick = { onModeChange(RailMode.Idle); onScanBarcode() },
                    colors = colors,
                    typography = typography,
                    shapes = shapes,
                    config = config,
                )
                StackAction(
                    icon = addAlbumIcon,
                    label = config.addAlbumLabel,
                    onClick = { onModeChange(RailMode.Idle); onAddAlbum() },
                    colors = colors,
                    typography = typography,
                    shapes = shapes,
                    config = config,
                )
            }
        }

        // Two-phase capsule: expands rightward
        Surface(
            modifier = Modifier
                .width(addWidth)
                .height(d.controlHeight)
                .align(Alignment.BottomStart)
                .clip(shapes.capsuleShape)
                .clickable(
                    role = Role.Button,
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                ) {
                    // Collapse search by switching modes.
                    onModeChange(if (isAdd) RailMode.Idle else RailMode.Add)
                },
            color = colors.capsuleContainer,
            border = BorderStroke(1.dp, colors.capsuleBorder),
            shadowElevation = 0.dp,
            shape = shapes.capsuleShape,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = addIcon,
                    contentDescription = "Add",
                    tint = colors.capsuleContent,
                    modifier = Modifier.size(d.iconSize),
                )

                // Optional label appears only when expanded.
                if (isAdd) {
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Add",
                        style = typography.capsuleTextStyle,
                        color = colors.capsuleContent,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchRailRight(
    mode: RailMode,
    onModeChange: (RailMode) -> Unit,
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    searchIcon: ImageVector,
    clearIcon: ImageVector,
    colors: RailColors,
    typography: RailTypography,
    shapes: RailShapes,
    collapsedWidth: Dp,
    expandedWidth: Dp,
    height: Dp,
    widthAnimMs: Int,
    innerHpad: Dp,
    iconToTextGap: Dp,
    iconSize: Dp,
    modifier: Modifier = Modifier,
) {
    val isSearch = mode == RailMode.Search

    val targetWidth = if (isSearch) expandedWidth else collapsedWidth
    val searchWidth by animateDpAsState(
        targetValue = targetWidth,
        animationSpec = tween(durationMillis = widthAnimMs),
        label = "searchWidth",
    )

    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current

    LaunchedEffect(isSearch) {
        if (isSearch) {
            focusRequester.requestFocus()
        } else {
            focusManager.clearFocus(force = true)
        }
    }

    Surface(
        modifier = modifier
            .width(searchWidth)
            .height(height)
            .clip(shapes.capsuleShape)
            .clickable(
                role = Role.Button,
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
            ) {
                onModeChange(if (isSearch) RailMode.Idle else RailMode.Search)
            },
        color = colors.searchContainer,
        border = BorderStroke(1.dp, colors.searchBorder),
        shadowElevation = 0.dp,
        shape = shapes.capsuleShape,
    ) {
        if (!isSearch) {
            // Collapsed: icon-only, centered.
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = searchIcon,
                    contentDescription = "Search",
                    tint = colors.searchText,
                    modifier = Modifier.size(iconSize),
                )
            }
        } else {
            // Expanded: icon + text field + clear
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = innerHpad),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = searchIcon,
                    contentDescription = "Search",
                    tint = colors.searchText,
                    modifier = Modifier.size(iconSize),
                )

                Spacer(modifier = Modifier.width(iconToTextGap))

                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = typography.searchTextStyle.copy(color = colors.searchText),
                    cursorBrush = SolidColor(colors.searchCursor),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            keyboard?.hide()
                            focusManager.clearFocus()
                        }
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester),
                    decorationBox = { inner ->
                        Box(modifier = Modifier.fillMaxWidth()) {
                            if (query.isBlank()) {
                                Text(
                                    text = placeholder,
                                    style = typography.searchPlaceholderStyle,
                                    color = colors.searchPlaceholder,
                                    maxLines = 1,
                                )
                            }
                            inner()
                        }
                    }
                )

                if (query.isNotBlank()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = clearIcon,
                        contentDescription = "Clear",
                        tint = colors.searchClearIcon,
                        modifier = Modifier
                            .size(iconSize)
                            .clip(shapes.capsuleShape)
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() },
                                role = Role.Button,
                            ) { onQueryChange("") },
                    )
                }
            }
        }
    }
}

@Composable
private fun StackAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    colors: RailColors,
    typography: RailTypography,
    shapes: RailShapes,
    config: RailConfig,
) {
    val d = config.dimens

    Surface(
        modifier = Modifier
            .width(d.addExpandedWidth) // make all stack items the same width
            .height(d.stackItemHeight)
            .clip(shapes.stackItemShape)
            .clickable(role = Role.Button) { onClick() },
        color = colors.stackItemContainer,
        border = BorderStroke(1.dp, colors.stackItemBorder),
        shadowElevation = 0.dp,
        shape = shapes.stackItemShape,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = d.stackItemHorizontalPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = colors.stackItemContent,
                modifier = Modifier.size(d.iconSize),
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = label,
                style = typography.stackItemTextStyle,
                color = colors.stackItemContent,
                maxLines = 1,
            )
        }
    }
}
