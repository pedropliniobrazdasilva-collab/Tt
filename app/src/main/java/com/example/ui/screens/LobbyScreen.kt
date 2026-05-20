package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.theme.*
import com.example.viewmodel.LobbyViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun LobbyScreen(
    viewModel: LobbyViewModel,
    modifier: Modifier = Modifier
) {
    val botState by viewModel.botState.collectAsState()
    val matchFormat by viewModel.matchFormat.collectAsState()
    val isAutomated by viewModel.isAutomated.collectAsState()
    val players by viewModel.players.collectAsState()
    val maps by viewModel.maps.collectAsState()
    val pickedMaps by viewModel.pickedMaps.collectAsState()
    val captainA by viewModel.captainA.collectAsState()
    val captainB by viewModel.captainB.collectAsState()
    val teamA by viewModel.teamA.collectAsState()
    val teamB by viewModel.teamB.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val vetoTurn by viewModel.vetoTurn.collectAsState()

    var customNameInput by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(CS2_Background),
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SportsEsports,
                            contentDescription = "CS2 Icon",
                            tint = CS2_Orange,
                            modifier = Modifier.size(28.dp)
                        )
                        Column {
                            Text(
                                text = "CS2 LOBBY BOT",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.5.sp
                                ),
                                color = CS2_TextPrimary
                            )
                            Text(
                                text = "Lobby Manager & Automated Matchmaker",
                                style = MaterialTheme.typography.bodySmall,
                                color = CS2_Orange
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CS2_Surface,
                    titleContentColor = CS2_TextPrimary
                ),
                actions = {
                    Box(
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .background(
                                color = if (isAutomated) CS2_Green.copy(alpha = 0.15f) else CS2_Orange.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(4.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = if (isAutomated) CS2_Green else CS2_Orange,
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (isAutomated) "AUTOMÁTICO" else "MANUAL",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            ),
                            color = if (isAutomated) CS2_Green else CS2_Orange
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(CS2_Background)
        ) {
            // Screen split: Main content (Scrollable Dashboard) + Console (Fixed size at bottom)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Progress Flow Indicator
                BotProgressFlow(currentState = botState)

                // Master Quick Automation Center Panel
                QuickControlPanel(
                    botState = botState,
                    isAutomated = isAutomated,
                    playerCount = players.size,
                    onMd1Auto = { viewModel.runCommand("/md1 auto") },
                    onMd3Auto = { viewModel.runCommand("/md3 auto") },
                    onMd5Auto = { viewModel.runCommand("/md5 auto") },
                    onCaptainsAuto = { viewModel.runCommand("/capitão auto") },
                    onTimesAuto = { viewModel.runCommand("/times auto") },
                    onReset = { viewModel.runCommand("/limpar") },
                    customName = customNameInput,
                    onCustomNameChange = { customNameInput = it },
                    onSubmitPlayer = {
                        if (customNameInput.isNotBlank()) {
                            viewModel.runCommand("/entrar $customNameInput")
                            customNameInput = ""
                            focusManager.clearFocus()
                        }
                    }
                )

                // Interactive Content based on state
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Queue Block (Always there, adjusts size)
                    QueuePanel(
                        modifier = Modifier
                            .weight(1.2f)
                            .heightIn(min = 400.dp),
                        players = players,
                        captainA = captainA,
                        captainB = captainB,
                        onRemovePlayer = { viewModel.removePlayer(it) },
                        onAddMockPlayers = {
                            viewModel.runCommand("/md1") // initiates empty queue MD1
                            viewModel.startAutomatedFlow(viewModel.matchFormat.value) // triggers auto fills
                        }
                    )

                    // Secondary Panel: Either Maps Veto or Teams Division
                    Column(
                        modifier = Modifier
                            .weight(1.8f)
                            .heightIn(min = 400.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (botState == BotState.MAP_VETO || botState == BotState.TEAM_DIVIDING || botState == BotState.FINISHED) {
                            // Map Veto Visualization card (Overpass included)
                            VetoPanel(
                                maps = maps,
                                pickedMaps = pickedMaps,
                                vetoTurn = vetoTurn,
                                format = matchFormat,
                                state = botState,
                                isAutomated = isAutomated,
                                currentVetoLabel = viewModel.currentVetoActionLabel(),
                                onMapClick = { viewModel.onMapClickInVeto(it) }
                            )

                            if (botState == BotState.TEAM_DIVIDING || botState == BotState.FINISHED) {
                                // Balance teams outputs
                                TeamsPanel(
                                    teamA = teamA,
                                    teamB = teamB,
                                    captainA = captainA,
                                    captainB = captainB
                                )
                            }
                        } else {
                            // Idle/Queue Instructions Info Card
                            InfoWaitingCard(playerCount = players.size, format = matchFormat)
                        }
                    }
                }
            }

            // Fixed Bottom discord interactive Console
            ConsolePanel(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
                    .background(CS2_TerminalBg),
                messages = messages,
                onSendCommand = { cmd ->
                    viewModel.runCommand(cmd)
                }
            )
        }
    }
}

@Composable
fun BotProgressFlow(currentState: BotState) {
    val steps = listOf(
        BotState.IDLE to "IDLE",
        BotState.QUEUE_WAITING to "FILA",
        BotState.CAPTAINS_SELECTING to "CAPITÃES",
        BotState.MAP_VETO to "VETO MAPAS",
        BotState.TEAM_DIVIDING to "TIMES",
        BotState.FINISHED to "CONCLUÍDO"
    )

    Card(
        colors = CardDefaults.cardColors(containerColor = CS2_Surface),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, CS2_SurfaceAlt),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            steps.forEachIndexed { index, (state, label) ->
                val isActive = when (state) {
                    BotState.IDLE -> currentState == BotState.IDLE
                    BotState.QUEUE_WAITING -> currentState == BotState.QUEUE_WAITING
                    BotState.CAPTAINS_SELECTING -> currentState == BotState.CAPTAINS_SELECTING
                    BotState.MAP_VETO -> currentState == BotState.MAP_VETO
                    BotState.TEAM_DIVIDING -> currentState == BotState.TEAM_DIVIDING
                    BotState.FINISHED -> currentState == BotState.FINISHED
                }
                
                // Done steps are steps before active
                val activeIndex = steps.indexOfFirst { it.first == currentState }
                val isDone = index < activeIndex && activeIndex != -1

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(34.dp)
                            .background(
                                color = when {
                                    isActive -> CS2_Orange.copy(alpha = 0.2f)
                                    isDone -> CS2_Green.copy(alpha = 0.15f)
                                    else -> CS2_SurfaceAlt
                                },
                                shape = RoundedCornerShape(17.dp)
                            )
                            .border(
                                width = 1.5.dp,
                                color = when {
                                    isActive -> CS2_Orange
                                    isDone -> CS2_Green
                                    else -> CS2_TextSecondary.copy(alpha = 0.4f)
                                },
                                shape = RoundedCornerShape(17.dp)
                            )
                    ) {
                        if (isDone) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Done",
                                tint = CS2_Green,
                                modifier = Modifier.size(16.dp)
                            )
                        } else {
                            Text(
                                text = (index + 1).toString(),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = if (isActive) CS2_Orange else CS2_TextSecondary
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 9.sp,
                            letterSpacing = 0.5.sp
                        ),
                        color = when {
                            isActive -> CS2_Orange
                            isDone -> CS2_Green
                            else -> CS2_TextSecondary
                        },
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                }

                // Small connector line between stages
                if (index < steps.size - 1) {
                    val isLineDone = index < activeIndex
                    Box(
                        modifier = Modifier
                            .weight(0.4f)
                            .height(1.5.dp)
                            .background(
                                color = if (isLineDone) CS2_Green else CS2_TextSecondary.copy(alpha = 0.2f)
                            )
                            .align(Alignment.CenterVertically)
                    )
                }
            }
        }
    }
}

@Composable
fun QuickControlPanel(
    botState: BotState,
    isAutomated: Boolean,
    playerCount: Int,
    onMd1Auto: () -> Unit,
    onMd3Auto: () -> Unit,
    onMd5Auto: () -> Unit,
    onCaptainsAuto: () -> Unit,
    onTimesAuto: () -> Unit,
    onReset: () -> Unit,
    customName: String,
    onCustomNameChange: (String) -> Unit,
    onSubmitPlayer: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CS2_Surface),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, CS2_SurfaceAlt),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "⚡ ADICIONAR COMANDOS DE AUTOMATIZAÇÃO (MANDATÓRIO)",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                ),
                color = CS2_Orange
            )

            // Section 1: Fully Automated Shortcuts
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Command /md1 auto
                Button(
                    onClick = onMd1Auto,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CS2_Orange
                    ),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("md1_auto_button"),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "MD1", modifier = Modifier.size(16.dp))
                        Text(
                            "/md1 auto",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }

                // Command /md3 auto
                Button(
                    onClick = onMd3Auto,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CS2_DarkOrange
                    ),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("md3_auto_button"),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "MD3", modifier = Modifier.size(16.dp))
                        Text(
                            "/md3 auto",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }

                // Command /md5 auto
                Button(
                    onClick = onMd5Auto,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CS2_CardBg
                    ),
                    border = BorderStroke(1.dp, CS2_Orange),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("md5_auto_button"),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "MD5", modifier = Modifier.size(16.dp), tint = CS2_Orange)
                        Text(
                            "/md5 auto",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = CS2_Orange
                        )
                    }
                }
            }

            // Section 2: Step-by-Step Command Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Captains Auto Button
                Button(
                    onClick = onCaptainsAuto,
                    enabled = playerCount >= 2 && botState == BotState.QUEUE_WAITING && !isAutomated,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CS2_SurfaceAlt,
                        contentColor = CS2_TextPrimary,
                        disabledContainerColor = CS2_SurfaceAlt.copy(alpha = 0.4f),
                        disabledContentColor = CS2_TextSecondary.copy(alpha = 0.4f)
                    ),
                    border = BorderStroke(1.dp, if (playerCount >= 2 && botState == BotState.QUEUE_WAITING && !isAutomated) CS2_Blue else CS2_SurfaceAlt),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Star, contentDescription = "Crown", modifier = Modifier.size(16.dp), tint = CS2_Blue)
                        Text("/capitão auto", style = MaterialTheme.typography.bodySmall)
                    }
                }

                // Teams Auto Button
                Button(
                    onClick = onTimesAuto,
                    enabled = (botState == BotState.TEAM_DIVIDING || botState == BotState.MAP_VETO) && !isAutomated,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CS2_SurfaceAlt,
                        contentColor = CS2_TextPrimary,
                        disabledContainerColor = CS2_SurfaceAlt.copy(alpha = 0.4f),
                        disabledContentColor = CS2_TextSecondary.copy(alpha = 0.4f)
                    ),
                    border = BorderStroke(1.dp, if ((botState == BotState.TEAM_DIVIDING || botState == BotState.MAP_VETO) && !isAutomated) CS2_Green else CS2_SurfaceAlt),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Group, contentDescription = "Group", modifier = Modifier.size(16.dp), tint = CS2_Green)
                        Text("/times auto", style = MaterialTheme.typography.bodySmall)
                    }
                }

                // Reset Button
                Button(
                    onClick = onReset,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CS2_Red.copy(alpha = 0.15f),
                        contentColor = CS2_Red
                    ),
                    border = BorderStroke(1.dp, CS2_Red),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.weight(0.6f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Trash", modifier = Modifier.size(16.dp))
                        Text("Reset", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            Divider(color = CS2_SurfaceAlt, thickness = 1.dp)

            // Section 3: Custom player additions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = customName,
                    onValueChange = onCustomNameChange,
                    placeholder = { Text("Nome do jogador para entrar...", color = CS2_TextSecondary) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CS2_Orange,
                        unfocusedBorderColor = CS2_SurfaceAlt,
                        focusedContainerColor = CS2_CardBg,
                        unfocusedContainerColor = CS2_CardBg,
                        focusedTextColor = CS2_TextPrimary,
                        unfocusedTextColor = CS2_TextPrimary
                    ),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.weight(1f)
                )

                Button(
                    onClick = onSubmitPlayer,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CS2_SurfaceAlt,
                        contentColor = CS2_Orange
                    ),
                    border = BorderStroke(1.dp, CS2_Orange),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text("+ /entrar", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                }
            }
        }
    }
}

@Composable
fun QueuePanel(
    modifier: Modifier = Modifier,
    players: List<Player>,
    captainA: Player?,
    captainB: Player?,
    onRemovePlayer: (String) -> Unit,
    onAddMockPlayers: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CS2_Surface),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, CS2_SurfaceAlt),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.Group, contentDescription = "Queue", tint = CS2_Orange, modifier = Modifier.size(18.dp))
                    Text(
                        text = "FILA LOBBY",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = CS2_TextPrimary
                    )
                }

                Box(
                    modifier = Modifier
                        .background(CS2_SurfaceAlt, shape = RoundedCornerShape(12.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "${players.size}/10 J",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (players.size == 10) CS2_Green else CS2_Orange
                    )
                }
            }

            if (players.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                        .background(CS2_CardBg, shape = RoundedCornerShape(4.dp))
                        .border(1.dp, CS2_SurfaceAlt, shape = RoundedCornerShape(4.dp))
                        .clickable { onAddMockPlayers() },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Group,
                            contentDescription = "Queue empty",
                            tint = CS2_TextSecondary.copy(alpha = 0.5f),
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            text = "A fila está vazia",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = CS2_TextPrimary
                        )
                        Text(
                            text = "Toque aqui para completar a fila automaticamente e simular o bot!",
                            style = MaterialTheme.typography.bodySmall,
                            color = CS2_Orange,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    players.forEachIndexed { index, player ->
                        val isCaptain = player.id == captainA?.id || player.id == captainB?.id
                        val capTeam = if (player.id == captainA?.id) "A" else if (player.id == captainB?.id) "B" else null

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(CS2_CardBg, shape = RoundedCornerShape(4.dp))
                                .border(
                                    width = 1.dp,
                                    color = when {
                                        capTeam == "A" -> CS2_Blue.copy(alpha = 0.6f)
                                        capTeam == "B" -> CS2_Orange.copy(alpha = 0.6f)
                                        else -> CS2_SurfaceAlt
                                    },
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                // Avatar circle with rank level
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(24.dp)
                                        .background(
                                            color = when {
                                                player.rating >= 18 -> CS2_Red.copy(alpha = 0.15f)
                                                player.rating >= 14 -> CS2_Orange.copy(alpha = 0.15f)
                                                else -> CS2_Blue.copy(alpha = 0.15f)
                                            },
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = when {
                                                player.rating >= 18 -> CS2_Red
                                                player.rating >= 14 -> CS2_Orange
                                                else -> CS2_Blue
                                            },
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                ) {
                                    Text(
                                        text = player.rating.toString(),
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp),
                                        color = when {
                                            player.rating >= 18 -> CS2_Red
                                            player.rating >= 14 -> CS2_Orange
                                            else -> CS2_Blue
                                        }
                                    )
                                }

                                Text(
                                    text = player.name,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = CS2_TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                if (isCaptain) {
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                color = if (capTeam == "A") CS2_Blue.copy(alpha = 0.15f) else CS2_Orange.copy(alpha = 0.15f),
                                                shape = RoundedCornerShape(3.dp)
                                            )
                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                    ) {
                                        Text(
                                            text = "CAP $capTeam",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 7.sp),
                                            color = if (capTeam == "A") CS2_Blue else CS2_Orange
                                        )
                                    }
                                }
                            }

                            IconButton(
                                onClick = { onRemovePlayer(player.id) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove player",
                                    tint = CS2_TextSecondary.copy(alpha = 0.6f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    // Remaining empty slots
                    if (players.size < 10) {
                        repeat(10 - players.size) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(CS2_CardBg.copy(alpha = 0.4f), shape = RoundedCornerShape(4.dp))
                                    .border(1.dp, CS2_SurfaceAlt.copy(alpha = 0.5f), shape = RoundedCornerShape(4.dp))
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Slot empty",
                                    tint = CS2_TextSecondary.copy(alpha = 0.2f),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Vaga disponível...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = CS2_TextSecondary.copy(alpha = 0.4f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VetoPanel(
    modifier: Modifier = Modifier,
    maps: List<MapItem>,
    pickedMaps: List<MapItem>,
    vetoTurn: String,
    format: MatchFormat,
    state: BotState,
    isAutomated: Boolean,
    currentVetoLabel: String,
    onMapClick: (String) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CS2_Surface),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, CS2_SurfaceAlt),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.Layers, contentDescription = "Map pools", tint = CS2_Orange, modifier = Modifier.size(18.dp))
                    Text(
                        text = "VETO DE MAPAS ($format)",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = CS2_TextPrimary
                    )
                }

                if (state == BotState.MAP_VETO) {
                    Box(
                        modifier = Modifier
                            .background(
                                color = if (vetoTurn == "A") CS2_Blue.copy(alpha = 0.15f) else CS2_Orange.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(4.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = if (vetoTurn == "A") CS2_Blue else CS2_Orange,
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "TURNO: CAPITÃO $vetoTurn",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (vetoTurn == "A") CS2_Blue else CS2_Orange
                        )
                    }
                }
            }

            if (state == BotState.MAP_VETO) {
                Text(
                    text = if (isAutomated) "🤖 Automático: Bot controlando os vetos..." else "👆 Clique em um mapa ativo abaixo para realizar a ação: $currentVetoLabel",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = if (isAutomated) CS2_Green else CS2_Orange,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Maps Visual Grid (CS2 Stylized)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                maxItemsInEachRow = 4,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                maps.forEach { mapItem ->
                    val isPickedInMatch = pickedMaps.any { it.id == mapItem.id }
                    val pickOrder = pickedMaps.indexOfFirst { it.id == mapItem.id } + 1

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(75.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                when (mapItem.status) {
                                    MapStatus.BANNED -> CS2_TerminalBg
                                    MapStatus.PICKED -> {
                                        if (mapItem.id == "overpass") CS2_DarkOrange.copy(alpha = 0.3f)
                                        else CS2_Green.copy(alpha = 0.2f)
                                    }
                                    else -> CS2_CardBg
                                }
                            )
                            .border(
                                width = if (mapItem.status == MapStatus.PICKED) 1.5.dp else 1.dp,
                                color = when (mapItem.status) {
                                    MapStatus.BANNED -> CS2_Red.copy(alpha = 0.5f)
                                    MapStatus.PICKED -> {
                                        if (mapItem.id == "overpass") CS2_Orange else CS2_Green
                                    }
                                    else -> {
                                        if (mapItem.id == "overpass") CS2_Orange.copy(alpha = 0.4f) // Highlight Overpass visually
                                        else CS2_SurfaceAlt
                                    }
                                },
                                shape = RoundedCornerShape(4.dp)
                            )
                            .clickable(
                                enabled = state == BotState.MAP_VETO && !isAutomated && mapItem.status == MapStatus.AVAILABLE
                            ) {
                                onMapClick(mapItem.id)
                            }
                            .drawBehind {
                                // Design element: Tactical lines
                                drawLine(
                                    color = if (mapItem.status == MapStatus.BANNED) CS2_Red.copy(alpha = 0.1f) else CS2_Orange.copy(alpha = 0.1f),
                                    start = Offset(0f, size.height * 0.8f),
                                    end = Offset(size.width, size.height * 0.4f),
                                    strokeWidth = 2f
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(6.dp)
                        ) {
                            Text(
                                text = mapItem.name.uppercase(),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 1.sp
                                ),
                                color = when (mapItem.status) {
                                    MapStatus.BANNED -> CS2_TextSecondary.copy(alpha = 0.4f)
                                    MapStatus.PICKED -> {
                                        if (mapItem.id == "overpass") CS2_Orange else CS2_Green
                                    }
                                    else -> CS2_TextPrimary
                                }
                            )

                            Spacer(modifier = Modifier.height(2.dp))

                            // Highlight requested map specialty
                            if (mapItem.id == "overpass" && mapItem.status == MapStatus.AVAILABLE) {
                                Box(
                                    modifier = Modifier
                                        .background(CS2_Orange.copy(alpha = 0.1f), RoundedCornerShape(2.dp))
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        "NOVO MAPA VETO",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 6.sp, fontWeight = FontWeight.Bold),
                                        color = CS2_Orange
                                    )
                                }
                            }

                            when (mapItem.status) {
                                MapStatus.BANNED -> {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Banned", tint = CS2_Red, modifier = Modifier.size(10.dp))
                                        Text(
                                            text = "BAN: ${mapItem.actionedBy ?: ""}",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontWeight = FontWeight.Bold),
                                            color = CS2_Red
                                        )
                                    }
                                }
                                MapStatus.PICKED -> {
                                    val isDecider = mapItem.actionedBy?.contains("Decider") == true || mapItem.actionedBy?.contains("Decoverery") == true
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Picked",
                                            tint = if (mapItem.id == "overpass") CS2_Orange else CS2_Green,
                                            modifier = Modifier.size(10.dp)
                                        )
                                        Text(
                                            text = if (isDecider) "DECIDER (M3)" else "PICK (M$pickOrder) - ${mapItem.actionedBy}",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = if (mapItem.id == "overpass") CS2_Orange else CS2_Green
                                        )
                                    }
                                }
                                MapStatus.AVAILABLE -> {
                                    Text(
                                        text = "DISPONÍVEL",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, color = CS2_TextSecondary.copy(alpha = 0.6f))
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TeamsPanel(
    modifier: Modifier = Modifier,
    teamA: List<Player>,
    teamB: List<Player>,
    captainA: Player?,
    captainB: Player?
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CS2_Surface),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, CS2_SurfaceAlt),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Default.Group, contentDescription = "Balanced teams", tint = CS2_Green, modifier = Modifier.size(18.dp))
                Text(
                    text = "TIMES FORMADOS /times auto",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = CS2_TextPrimary
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Team A (Blue)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .background(CS2_CardBg, shape = RoundedCornerShape(4.dp))
                        .border(1.dp, CS2_Blue.copy(alpha = 0.4f), shape = RoundedCornerShape(4.dp))
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val avgRatingA = if (teamA.isNotEmpty()) teamA.map { it.rating }.average() else 0.0

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "TIME AZUL",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = CS2_Blue
                        )
                        Text(
                            text = "Rating: ${String.format("%.1f", avgRatingA)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = CS2_TextSecondary
                        )
                    }

                    Divider(color = CS2_SurfaceAlt)

                    teamA.forEach { player ->
                        val isCap = player.id == captainA?.id
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .background(CS2_Blue.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                ) {
                                    Text(
                                        text = player.rating.toString(),
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.sp, fontWeight = FontWeight.Bold),
                                        color = CS2_Blue
                                    )
                                }
                                Text(
                                    text = player.name,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = if (isCap) FontWeight.Bold else FontWeight.Normal
                                    ),
                                    color = CS2_TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (isCap) {
                                Text(
                                    text = "👑 CAP",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 7.sp),
                                    color = CS2_Blue
                                )
                            }
                        }
                    }
                }

                // Team B (Orange)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .background(CS2_CardBg, shape = RoundedCornerShape(4.dp))
                        .border(1.dp, CS2_Orange.copy(alpha = 0.4f), shape = RoundedCornerShape(4.dp))
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val avgRatingB = if (teamB.isNotEmpty()) teamB.map { it.rating }.average() else 0.0

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "TIME LARANJA",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = CS2_Orange
                        )
                        Text(
                            text = "Rating: ${String.format("%.1f", avgRatingB)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = CS2_TextSecondary
                        )
                    }

                    Divider(color = CS2_SurfaceAlt)

                    teamB.forEach { player ->
                        val isCap = player.id == captainB?.id
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .background(CS2_Orange.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                ) {
                                    Text(
                                        text = player.rating.toString(),
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.sp, fontWeight = FontWeight.Bold),
                                        color = CS2_Orange
                                    )
                                }
                                Text(
                                    text = player.name,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = if (isCap) FontWeight.Bold else FontWeight.Normal
                                    ),
                                    color = CS2_TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (isCap) {
                                Text(
                                    text = "👑 CAP",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 7.sp),
                                    color = CS2_Orange
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InfoWaitingCard(playerCount: Int, format: MatchFormat) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CS2_Surface),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, CS2_SurfaceAlt),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Layers,
                contentDescription = "Waiting veto",
                tint = CS2_Orange.copy(alpha = 0.4f),
                modifier = Modifier.size(54.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Veto de mapas pendente",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = CS2_TextPrimary
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Para iniciar o veto de mapas automátivo ou manual, a fila necessita ter 10 jogadores. Você pode digitar /md1 auto para realizar todo o ciclo de automação.",
                style = MaterialTheme.typography.bodySmall,
                color = CS2_TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .background(CS2_SurfaceAlt, RoundedCornerShape(4.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "Aguardando: ${10 - playerCount} jogador(es)",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = CS2_Orange
                )
            }
        }
    }
}

@Composable
fun ConsolePanel(
    modifier: Modifier = Modifier,
    messages: List<BotMessage>,
    onSendCommand: (String) -> Unit
) {
    var textInput by remember { mutableStateOf("") }
    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Auto-scroll terminal to bottom
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            lazyListState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(modifier = modifier) {
        // Console Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CS2_Surface)
                .border(width = 1.dp, color = CS2_SurfaceAlt)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(CS2_Green, RoundedCornerShape(4.dp))
                )
                Text(
                    text = "CS2 MATCHMAKER BOT CONSOLE (Simulação Discord)",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.5.sp
                    ),
                    color = CS2_TextPrimary
                )
            }

            Text(
                text = "TOTAL: ${messages.size} logs",
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                color = CS2_TextSecondary
            )
        }

        // Live Feed Terminals Logs
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            LazyColumn(
                state = lazyListState,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(messages) { msg ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "[${msg.timestamp}]",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                color = CS2_TextSecondary.copy(alpha = 0.5f),
                                fontSize = 11.sp
                            )
                        )

                        val prefix = when (msg.type) {
                            MessageType.COMMAND -> "User >"
                            MessageType.SUCCESS -> "Bot ✔"
                            MessageType.SYSTEM -> "System"
                            MessageType.INFO -> "Bot"
                        }

                        val color = when (msg.type) {
                            MessageType.COMMAND -> CS2_Blue
                            MessageType.SUCCESS -> CS2_Green
                            MessageType.SYSTEM -> CS2_Orange
                            MessageType.INFO -> CS2_TextPrimary
                        }

                        Text(
                            text = "$prefix:",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = color,
                                fontSize = 11.sp
                            )
                        )

                        Text(
                            text = msg.text,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp
                            ),
                            color = if (msg.type == MessageType.COMMAND) CS2_Blue else CS2_TextPrimary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        Divider(color = CS2_SurfaceAlt)

        // Command input bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = textInput,
                onValueChange = { textInput = it },
                textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                placeholder = {
                    Text(
                        "Digite um comando (ex: /md1 auto ou /times auto)",
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        color = CS2_TextSecondary.copy(alpha = 0.6f)
                    )
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CS2_Orange,
                    unfocusedBorderColor = CS2_SurfaceAlt,
                    focusedContainerColor = CS2_CardBg,
                    unfocusedContainerColor = CS2_CardBg,
                    focusedTextColor = CS2_TextPrimary,
                    unfocusedTextColor = CS2_TextPrimary
                ),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp)
            )

            Button(
                onClick = {
                    if (textInput.isNotBlank()) {
                        onSendCommand(textInput)
                        textInput = ""
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = CS2_Orange),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.height(42.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Enviar",
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
