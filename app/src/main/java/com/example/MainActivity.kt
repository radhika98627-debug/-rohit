package com.example

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Calculation
import com.example.data.CalculatorDatabase
import com.example.data.CalculatorRepository
import com.example.ui.CalculatorViewModel
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    // Lazily instantiate database and repositories per standard modern practices
    private val database by lazy { CalculatorDatabase.getDatabase(applicationContext) }
    private val repository by lazy { CalculatorRepository(database.calculationDao()) }

    // Use ViewModel Factory to wire up the VM easily
    private val viewModel: CalculatorViewModel by viewModels {
        CalculatorViewModel.Factory(repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    CalculatorScreen(
                        viewModel = viewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorScreen(
    viewModel: CalculatorViewModel,
    modifier: Modifier = Modifier
) {
    val displayText by viewModel.displayText.collectAsStateWithLifecycle()
    val resultText by viewModel.resultText.collectAsStateWithLifecycle()
    val isRadians by viewModel.isRadians.collectAsStateWithLifecycle()
    val historyList by viewModel.history.collectAsStateWithLifecycle()

    val scope = rememberCoroutineScope()
    var showHistorySheet by remember { mutableStateOf(false) }

    // Screen dimensions detection for responsiveness
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    // Clean Minimalism background styling
    val backgroundColor = Color(0xFF1C1B1F)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        if (isLandscape) {
            // Landscape layout: Split Screen
            Row(modifier = Modifier.fillMaxSize()) {
                // Interactive real-time history pane on the left with Clean Minimalism panel coloring
                Box(
                    modifier = Modifier
                        .weight(1.2f)
                        .fillMaxHeight()
                        .padding(start = 16.dp, top = 16.dp, bottom = 16.dp, end = 8.dp)
                        .background(Color(0xFF2B2930), RoundedCornerShape(24.dp))
                        .clip(RoundedCornerShape(24.dp))
                ) {
                    HistoryContent(
                        history = historyList,
                        onItemClick = { viewModel.onHistoryItemClick(it) },
                        onDeleteClick = { viewModel.onDeleteCalculation(it) },
                        onClearAll = { viewModel.onClearHistory() }
                    )
                }

                // Calculator Display and Keypad on the right
                Column(
                    modifier = Modifier
                        .weight(2f)
                        .fillMaxHeight()
                        .padding(start = 8.dp, top = 16.dp, bottom = 16.dp, end = 16.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    CalculatorDisplay(
                        displayText = displayText,
                        resultText = resultText,
                        isRadians = isRadians,
                        onToggleRadDeg = { viewModel.onButtonClick("DEG/RAD") },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    ScientificAndStandardGrids(
                        onButtonClick = { viewModel.onButtonClick(it) },
                        isRadians = isRadians,
                        modifier = Modifier
                            .weight(2.5f)
                            .fillMaxWidth()
                    )
                }
            }
        } else {
            // Portrait layout: Stacked structure perfectly aligned with Clean Minimalism design HTML
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Main Header Title with minimalist brand logo and history button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, end = 24.dp, top = 32.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Soft minimalist brand icon container
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(Color(0xFFD0BCFF), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "+",
                                color = Color(0xFF381E72),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "Calculator",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFE6E1E5),
                            letterSpacing = (-0.5).sp
                        )
                    }

                    // Minimalist pill-shaped history toggle
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color(0xFF2B2930))
                            .clickable { showHistorySheet = true }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                            .testTag("history_toggle_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "HISTORY",
                            color = Color(0xFFD0BCFF),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // High Contrast Output Display Window
                CalculatorDisplay(
                    displayText = displayText,
                    resultText = resultText,
                    isRadians = isRadians,
                    onToggleRadDeg = { viewModel.onButtonClick("DEG/RAD") },
                    modifier = Modifier
                        .weight(1.2f)
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                )

                // Premium Rounded Keyboard container background (#2B2930) with custom tabs and grids
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Color(0xFF2B2930),
                            RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp)
                        )
                        .padding(top = 20.dp, start = 20.dp, end = 20.dp, bottom = 12.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Subtitle indicator tabs as styled in the Clean Minimalism design HTML
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFF4A4458), RoundedCornerShape(20.dp))
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "Basic",
                                    color = Color(0xFFD0BCFF),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFF332D41), RoundedCornerShape(20.dp))
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "Scientific",
                                    color = Color(0xFFE6E1E5),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        ScientificAndStandardGrids(
                            onButtonClick = { viewModel.onButtonClick(it) },
                            isRadians = isRadians,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Touch interaction bar indicator at the keyboard bottom
                        Box(
                            modifier = Modifier
                                .width(48.dp)
                                .height(5.dp)
                                .background(Color(0xFF4A4458), CircleShape)
                        )
                    }
                }
            }
        }

        // Slide Up Modal Sheet styled with Clean Minimalism container color
        if (showHistorySheet && !isLandscape) {
            ModalBottomSheet(
                onDismissRequest = { showHistorySheet = false },
                containerColor = Color(0xFF2B2930),
                contentColor = Color(0xFFE6E1E5),
                tonalElevation = 12.dp,
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                modifier = Modifier.fillMaxHeight(0.75f)
            ) {
                HistoryContent(
                    history = historyList,
                    onItemClick = {
                        viewModel.onHistoryItemClick(it)
                        showHistorySheet = false
                    },
                    onDeleteClick = { viewModel.onDeleteCalculation(it) },
                    onClearAll = { viewModel.onClearHistory() }
                )
            }
        }
    }
}

@Composable
fun CalculatorDisplay(
    displayText: String,
    resultText: String,
    isRadians: Boolean,
    onToggleRadDeg: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    // Whenever typed text changes, auto scroll display input area to the right
    LaunchedEffect(displayText) {
        if (displayText.isNotEmpty()) {
            listState.animateScrollToItem(displayText.length - 1)
        }
    }

    Box(
        modifier = modifier
            .padding(vertical = 12.dp)
    ) {
        // RAD / DEG Badges in Display Corner
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF4A4458))
                .clickable { onToggleRadDeg() }
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(Color(0xFFD0BCFF), CircleShape)
                )
                Text(
                    text = if (isRadians) "RAD" else "DEG",
                    color = Color(0xFFD0BCFF),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = FontFamily.SansSerif
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 36.dp),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.End
        ) {
            // Live/Query Equation Scroll Strip
            LazyRow(
                state = listState,
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                item {
                    Text(
                        text = if (displayText.isBlank()) "0" else displayText,
                        fontSize = if (displayText.length > 14) 22.sp else 28.sp,
                        fontWeight = FontWeight.Light,
                        color = Color(0xFF938F99),
                        textAlign = TextAlign.End,
                        maxLines = 1,
                        modifier = Modifier.testTag("calculator_input_display")
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Result strip shown in glowing accent color
            AnimatedVisibility(
                visible = resultText.isNotEmpty(),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Text(
                    text = resultText,
                    fontSize = if (resultText.length > 8) 38.sp else 56.sp,
                    fontWeight = FontWeight.Light,
                    color = Color.White,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("calculator_result_display")
                )
            }
        }
    }
}

@Composable
fun ScientificAndStandardGrids(
    onButtonClick: (String) -> Unit,
    isRadians: Boolean,
    modifier: Modifier = Modifier
) {
    // Buttons grids
    val scientificLayout = listOf(
        listOf("sin", "cos", "tan", "x^y"),
        listOf("ln", "log", "sqrt", "π"),
        listOf("e", "(", ")", if (isRadians) "RAD" else "DEG")
    )

    val standardLayout = listOf(
        listOf("C", "⌫", "%", "÷"),
        listOf("7", "8", "9", "×"),
        listOf("4", "5", "6", "-"),
        listOf("1", "2", "3", "+"),
        listOf("0", ".", "=")
    )

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Scientific Keyboard Block (Steel metal styling)
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1.3f)
        ) {
            scientificLayout.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    row.forEach { char ->
                        CalculatorButton(
                            symbol = char,
                            onClick = { onButtonClick(char) },
                            isScientific = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Standard Numerical & Operation Block (Gloss Obsidian Styling)
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(2.7f)
        ) {
            standardLayout.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    row.forEach { char ->
                        val isDoubleWeight = char == "0" && row.size == 3 // Zero scales nicely
                        val weight = if (isDoubleWeight) 2f else 1f
                        CalculatorButton(
                            symbol = char,
                            onClick = { onButtonClick(char) },
                            isScientific = false,
                            modifier = Modifier.weight(weight)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CalculatorButton(
    symbol: String,
    onClick: () -> Unit,
    isScientific: Boolean,
    modifier: Modifier = Modifier
) {
    // Clean Minimalism visual color mapping from Design HTML and Guidelines
    val backgroundColor = when {
        symbol == "=" -> Color(0xFF381E72) // Dark violet equal action button
        symbol in listOf("÷", "×", "-", "+") -> Color(0xFFD0BCFF) // Highlight lavender symbol modifiers
        symbol in listOf("C", "⌫", "%", "(", ")") || isScientific -> Color(0xFF4A4458) // Mid-tone clean purple utilities
        else -> Color(0xFF1C1B1F) // Minimalist slate numbers, values, and dots
    }

    val textColor = when {
        symbol == "=" -> Color(0xFFD0BCFF)
        symbol in listOf("÷", "×", "-", "+") -> Color(0xFF381E72)
        symbol in listOf("C", "⌫", "%", "(", ")") || isScientific -> Color(0xFFD0BCFF)
        else -> Color(0xFFE6E1E5)
    }

    val displaySymbol = when (symbol) {
        "x^y" -> "xʸ"
        "sqrt" -> "√"
        else -> symbol
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = spring(
            dampingRatio = 0.55f,
            stiffness = 600f
        ),
        label = "button_scale"
    )

    Box(
        modifier = modifier
            .heightIn(min = 48.dp, max = 64.dp)
            .aspectRatio(if (symbol == "0") 2f else 1f)
            .scale(scale)
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(),
                onClick = onClick
            )
            .testTag("calculator_button_$symbol"),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = displaySymbol,
            color = textColor,
            fontSize = if (isScientific) 15.sp else 21.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun HistoryContent(
    history: List<Calculation>,
    onItemClick: (Calculation) -> Unit,
    onDeleteClick: (Calculation) -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Title Bar inside History Screen
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "History Log",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            if (history.isNotEmpty()) {
                TextButton(
                    onClick = onClearAll,
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFFF5252)),
                    modifier = Modifier.testTag("history_clear_button")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Clear All Action",
                            modifier = Modifier.size(18.dp)
                        )
                        Text(text = "Clear All", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Divider(
            color = Color.White.copy(alpha = 0.08f),
            thickness = 1.dp,
            modifier = Modifier.padding(vertical = 12.dp)
        )

        if (history.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "History is empty",
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("history_list_view"),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(history, key = { it.id }) { item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onItemClick(item) }
                            .testTag("history_item_${item.id}"),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF1C1B1F),
                            contentColor = Color(0xFFE6E1E5)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = item.expression,
                                    color = Color(0xFF938F99),
                                    fontSize = 14.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "= ${item.result}",
                                    color = Color(0xFFD0BCFF),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            // Specific entry deletion icon
                            IconButton(
                                onClick = { onDeleteClick(item) },
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("delete_item_button_${item.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete entry",
                                    tint = Color.White.copy(alpha = 0.4f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
