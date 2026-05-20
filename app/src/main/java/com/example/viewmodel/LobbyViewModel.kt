package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class LobbyViewModel : ViewModel() {

    // Initial map list with Overpass!
    private val defaultMaps = listOf(
        MapItem("mirage", "Mirage"),
        MapItem("inferno", "Inferno"),
        MapItem("nuke", "Nuke"),
        MapItem("overpass", "Overpass"), // Overpass added as requested
        MapItem("vertigo", "Vertigo"),
        MapItem("ancient", "Ancient"),
        MapItem("anubis", "Anubis"),
        MapItem("dust2", "Dust 2")
    )

    // Matchmaking & Bot States
    private val _botState = MutableStateFlow(BotState.IDLE)
    val botState: StateFlow<BotState> = _botState.asStateFlow()

    private val _matchFormat = MutableStateFlow(MatchFormat.MD1)
    val matchFormat: StateFlow<MatchFormat> = _matchFormat.asStateFlow()

    private val _isAutomated = MutableStateFlow(false)
    val isAutomated: StateFlow<Boolean> = _isAutomated.asStateFlow()

    // Player lists
    private val _players = MutableStateFlow<List<Player>>(emptyList())
    val players: StateFlow<List<Player>> = _players.asStateFlow()

    private val _captainA = MutableStateFlow<Player?>(null)
    val captainA: StateFlow<Player?> = _captainA.asStateFlow()

    private val _captainB = MutableStateFlow<Player?>(null)
    val captainB: StateFlow<Player?> = _captainB.asStateFlow()

    private val _teamA = MutableStateFlow<List<Player>>(emptyList())
    val teamA: StateFlow<List<Player>> = _teamA.asStateFlow()

    private val _teamB = MutableStateFlow<List<Player>>(emptyList())
    val teamB: StateFlow<List<Player>> = _teamB.asStateFlow()

    // Map Pool State
    private val _maps = MutableStateFlow<List<MapItem>>(defaultMaps)
    val maps: StateFlow<List<MapItem>> = _maps.asStateFlow()

    // Picked maps for the match
    private val _pickedMaps = MutableStateFlow<List<MapItem>>(emptyList())
    val pickedMaps: StateFlow<List<MapItem>> = _pickedMaps.asStateFlow()

    // Live Message console input
    private val _messages = MutableStateFlow<List<BotMessage>>(emptyList())
    val messages: StateFlow<List<BotMessage>> = _messages.asStateFlow()

    // Active Captain turn in map veto ("A" or "B")
    private val _vetoTurn = MutableStateFlow("A")
    val vetoTurn: StateFlow<String> = _vetoTurn.asStateFlow()

    // Active automation Job
    private var automationJob: Job? = null

    // Predefined roster of gamer names to simulate realistic fills
    private val sampleNames = listOf(
        "Fallen", "Coldzera", "Fer", "Taco", "Fnx", 
        "Kscerato", "Yuurih", "Art", "Hen1", "Saffee",
        "ZywOo", "s1mple", "donk", "m0NESY", "NiKo",
        "ropz", "broky", "Twistzz", "Snax", "dev1ce"
    )

    init {
        logBotMessage("Bot online. Digite um comando ou utilize os atalhos abaixo.", MessageType.SYSTEM)
        logBotMessage("Formatos disponíveis: /md1, /md3, /md5. Comandos de automação: /md1 auto, /md3 auto, /md5 auto.", MessageType.INFO)
    }

    private fun getCurrentTimestamp(): String {
        return SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
    }

    fun logBotMessage(text: String, type: MessageType = MessageType.INFO) {
        val message = BotMessage(
            text = text,
            type = type,
            timestamp = getCurrentTimestamp()
        )
        // Keep last 100 messages
        _messages.value = (_messages.value + message).takeLast(100)
    }

    // Command Parser
    fun runCommand(commandText: String) {
        val cleanCommand = commandText.trim()
        if (!cleanCommand.startsWith("/")) {
            logBotMessage("Mensagem: $cleanCommand", MessageType.INFO)
            return
        }

        logBotMessage(cleanCommand, MessageType.COMMAND)

        val parts = cleanCommand.split(" ")
        val cmd = parts[0].lowercase()
        val arg = parts.getOrNull(1)?.lowercase()

        when (cmd) {
            "/md1" -> {
                if (arg == "auto") {
                    startAutomatedFlow(MatchFormat.MD1)
                } else {
                    startManualLobby(MatchFormat.MD1)
                }
            }
            "/md3" -> {
                if (arg == "auto") {
                    startAutomatedFlow(MatchFormat.MD3)
                } else {
                    startManualLobby(MatchFormat.MD3)
                }
            }
            "/md5" -> {
                if (arg == "auto") {
                    startAutomatedFlow(MatchFormat.MD5)
                } else {
                    startManualLobby(MatchFormat.MD5)
                }
            }
            "/capitao", "/capitão" -> {
                if (arg == "auto") {
                    executeAutoCaptains()
                } else {
                    logBotMessage("Use '/capitão auto' para selecionar os capitães automaticamente.", MessageType.INFO)
                }
            }
            "/times" -> {
                if (arg == "auto") {
                    executeAutoTeams()
                } else {
                    logBotMessage("Use '/times auto' para dividir os times automaticamente.", MessageType.INFO)
                }
            }
            "/entrar" -> {
                val name = parts.getOrNull(1) ?: "Jogador_${(100..999).random()}"
                addPlayer(name)
            }
            "/sair" -> {
                val name = parts.getOrNull(1)
                if (name != null) {
                    removePlayerByName(name)
                } else {
                    logBotMessage("Use '/sair [Nome]' para remover um jogador específico.", MessageType.INFO)
                }
            }
            "/limpar" -> {
                resetLobby()
                logBotMessage("Lobby limpo e reiniciado.", MessageType.SYSTEM)
            }
            "/mapas" -> {
                val text = _maps.value.joinToString { "${it.name} (${it.status})" }
                logBotMessage("Mapas disponíveis: $text", MessageType.INFO)
            }
            "/status" -> {
                logBotMessage("Estado atual do Bot: ${_botState.value}. Jogadores na fila: ${_players.value.size}/10.", MessageType.INFO)
            }
            else -> {
                logBotMessage("Comando desconhecido. Comandos: /md1, /md3, /md5, /md1 auto, /md3 auto, /md5 auto, /capitão auto, /times auto, /entrar, /sair, /limpar, /status", MessageType.INFO)
            }
        }
    }

    // Set a manual lobby
    fun startManualLobby(format: MatchFormat) {
        cancelActiveAutomation()
        _matchFormat.value = format
        _isAutomated.value = false
        _maps.value = defaultMaps.map { it.copy(status = MapStatus.AVAILABLE, actionedBy = null) }
        _pickedMaps.value = emptyList()
        _captainA.value = null
        _captainB.value = null
        _teamA.value = emptyList()
        _teamB.value = emptyList()
        _vetoTurn.value = "A"
        _botState.value = BotState.QUEUE_WAITING

        logBotMessage("Fila iniciada para Match [$format] (Modo Manual). Adicione 10 jogadores para prosseguir.", MessageType.SYSTEM)
    }

    // Completely Automated Flow: /mdX auto
    fun startAutomatedFlow(format: MatchFormat) {
        cancelActiveAutomation()
        _matchFormat.value = format
        _isAutomated.value = true
        _maps.value = defaultMaps.map { it.copy(status = MapStatus.AVAILABLE, actionedBy = null) }
        _pickedMaps.value = emptyList()
        _captainA.value = null
        _captainB.value = null
        _teamA.value = emptyList()
        _teamB.value = emptyList()
        _vetoTurn.value = "A"
        _botState.value = BotState.QUEUE_WAITING

        logBotMessage("Fila iniciada para Match [$format] (Modo Automático - /${format.name.lowercase()} auto).", MessageType.SYSTEM)
        logBotMessage("Bot irá preencher a fila e automatizar Captains, Veto e Times com pequenos intervalos visuais.", MessageType.INFO)

        automationJob = viewModelScope.launch {
            delay(1000)
            
            // Step 1: Fill queue if under 10 players
            if (_players.value.size < 10) {
                logBotMessage("Preenchendo fila automaticamente com jogadores...", MessageType.SYSTEM)
                val currentSize = _players.value.size
                val needed = 10 - currentSize
                val pool = sampleNames.shuffled()
                var addedCount = 0
                for (name in pool) {
                    if (addedCount >= needed) break
                    if (!_players.value.any { it.name.lowercase() == name.lowercase() }) {
                        addPlayerInternal(name)
                        addedCount++
                        delay(250) // visual fill delay
                    }
                }
            }

            logBotMessage("👥 Fila completa (10/10 jogadores). Pronto para iniciar automação!", MessageType.SUCCESS)
            delay(1200)

            // Step 2: Auto Captain selection
            runCommand("/capitão auto")
            delay(2000)

            // Step 3: Auto Veto
            runAutoMapVetoFlow()
        }
    }

    // Selected Captains automatically
    fun executeAutoCaptains() {
        val currentPlayers = _players.value
        if (currentPlayers.size < 2) {
            logBotMessage("Erro /capitão auto: É necessário pelo menos 2 jogadores na fila.", MessageType.INFO)
            return
        }

        _botState.value = BotState.CAPTAINS_SELECTING
        logBotMessage("⚡ Executando comando /capitão auto...", MessageType.COMMAND)

        // Select the top two highest-rated players, or 2 random if ratings are equal
        val sorted = currentPlayers.sortedByDescending { it.rating }
        val capA = sorted[0].copy(isCaptain = true, team = "A")
        val capB = sorted[1].copy(isCaptain = true, team = "B")

        _captainA.value = capA
        _captainB.value = capB

        // Update in players list
        _players.value = currentPlayers.map {
            when (it.id) {
                capA.id -> capA
                capB.id -> capB
                else -> it.copy(isCaptain = false, team = null)
            }
        }

        logBotMessage("👑 Capitães selecionados automaticamente:", MessageType.SUCCESS)
        logBotMessage("👑 Capitão A: @${capA.name} (Rating: ${capA.rating})", MessageType.SUCCESS)
        logBotMessage("👑 Capitão B: @${capB.name} (Rating: ${capB.rating})", MessageType.SUCCESS)

        _botState.value = BotState.MAP_VETO
        _vetoTurn.value = "A" // Captain A starts banning
        logBotMessage("🗺️ Iniciando Veto de Mapas. Formato: ${_matchFormat.value}.", MessageType.SYSTEM)
        logBotMessage("Capitão A (@${capA.name}) começa o banimento.", MessageType.INFO)
    }

    // Automated map veto flow
    private suspend fun runAutoMapVetoFlow() {
        val format = _matchFormat.value
        logBotMessage("🤖 Iniciando banimentos automáticos dos mapas...", MessageType.SYSTEM)

        while (hasNextVetoAction(format)) {
            val currentTurn = _vetoTurn.value
            val currentCaptain = if (currentTurn == "A") _captainA.value else _captainB.value
            val captainLabel = if (currentTurn == "A") "Capitão A (@${currentCaptain?.name})" else "Capitão B (@${currentCaptain?.name})"

            delay(1200) // Simulates typing / choosing delay

            val action = getNextVetoActionType(format)
            val availableMaps = _maps.value.filter { it.status == MapStatus.AVAILABLE }

            if (availableMaps.isEmpty()) break

            // Randomly pick a map to ban/pick from available pool
            val selectedMap = availableMaps.random()

            if (action == "BAN") {
                banMap(selectedMap.id, currentTurn)
            } else {
                pickMap(selectedMap.id, currentTurn)
            }
        }

        // Veto finished, do decider if needed
        val available = _maps.value.filter { it.status == MapStatus.AVAILABLE }
        if (available.size == 1) {
            val decider = available.first()
            _pickedMaps.value = _pickedMaps.value + decider
            _maps.value = _maps.value.map {
                if (it.id == decider.id) it.copy(status = MapStatus.PICKED, actionedBy = "Decoverery Map")
                else it
            }
            logBotMessage("🗺️ Último mapa restante (Decider): ${decider.name}!", MessageType.SUCCESS)
            delay(1200)
        }

        logBotMessage("🎉 Veto de mapas concluído com sucesso!", MessageType.SUCCESS)
        val selectedNames = _pickedMaps.value.joinToString { it.name }
        logBotMessage("🎮 Mapas que serão jogados: $selectedNames", MessageType.SUCCESS)
        
        delay(1500)

        // Step 4: Auto Team Division
        runCommand("/times auto")
    }

    private fun hasNextVetoAction(format: MatchFormat): Boolean {
        val totalAvailable = _maps.value.count { it.status == MapStatus.AVAILABLE }
        val totalPicked = _pickedMaps.value.size

        return when (format) {
            MatchFormat.MD1 -> totalAvailable > 1 // Ban until 1 remains
            MatchFormat.MD3 -> {
                // MD3 steps: Need to ban 4 maps, pick 2. (7 available -> 6 available -> 1 picked -> 1 picked -> 3 available -> 2 available -> 1 remaining as Decider)
                // Let's count banned, picked:
                // We pick 2 maps, ban others till 1 decider.
                // Veto action continues if available maps > 1 and we haven't picked 2 yet, or if we have picked 2 but still have options to ban.
                // Practically: run until 1 remaining map.
                totalAvailable > 1
            }
            MatchFormat.MD5 -> {
                // MD5 steps: pick 4 maps, ban 3, 1 remains decider.
                // Run until 1 remaining map.
                totalAvailable > 1
            }
        }
    }

    private fun getNextVetoActionType(format: MatchFormat): String {
        val totalAvailable = _maps.value.count { it.status == MapStatus.AVAILABLE }
        val totalPicked = _pickedMaps.value.size

        return when (format) {
            MatchFormat.MD1 -> "BAN" // MD1 is ONLY BANS until 1 remains
            MatchFormat.MD3 -> {
                // Map sequence:
                // 1. BAN (8 remain)
                // 2. BAN (7 remain)
                // 3. PICK (6 remain)
                // 4. PICK (5 remain)
                // 5. BAN (4 remain)
                // 6. BAN (3 remain)
                // 7. BAN (2 remain)
                // -> 1 decider
                when (totalAvailable) {
                    8, 7 -> "BAN"
                    6, 5 -> "PICK"
                    else -> "BAN"
                }
            }
            MatchFormat.MD5 -> {
                // Map sequence:
                // 1. BAN (8 remain)
                // 2. BAN (7 remain)
                // 3. PICK (6 remain)
                // 4. PICK (5 remain)
                // 5. PICK (4 remain)
                // 6. PICK (3 remain)
                // 7. BAN (2 remain) -> 1 remaining decider
                when (totalAvailable) {
                    8, 7 -> "BAN"
                    6, 5, 4, 3 -> "PICK"
                    else -> "BAN"
                }
            }
        }
    }

    // Manual Veto Actions
    fun currentVetoActionLabel(): String {
        if (_botState.value != BotState.MAP_VETO) return ""
        val type = getNextVetoActionType(_matchFormat.value)
        val turn = _vetoTurn.value
        val captainName = if (turn == "A") _captainA.value?.name else _captainB.value?.name
        return if (type == "BAN") "Banir mapa (Turno de @$captainName)" else "Escolher mapa (Turno de @$captainName)"
    }

    fun onMapClickInVeto(mapId: String) {
        if (_botState.value != BotState.MAP_VETO || _isAutomated.value) return

        val action = getNextVetoActionType(_matchFormat.value)
        val turn = _vetoTurn.value

        if (action == "BAN") {
            banMap(mapId, turn)
        } else {
            pickMap(mapId, turn)
        }

        // Check if finished or decider step should trigger
        val available = _maps.value.filter { it.status == MapStatus.AVAILABLE }
        if (available.size == 1) {
            val decider = available.first()
            _pickedMaps.value = _pickedMaps.value + decider
            _maps.value = _maps.value.map {
                if (it.id == decider.id) it.copy(status = MapStatus.PICKED, actionedBy = "Decider")
                else it
            }
            logBotMessage("🗺️ Último mapa restante (Decider): ${decider.name}!", MessageType.SUCCESS)
        }

        if (available.size <= 1) {
            logBotMessage("🎉 Veto de mapas concluído com sucesso!", MessageType.SUCCESS)
            val selectedNames = _pickedMaps.value.joinToString { it.name }
            logBotMessage("🎮 Mapas que serão jogados: $selectedNames", MessageType.SUCCESS)
            _botState.value = BotState.TEAM_DIVIDING
        }
    }

    fun banMap(mapId: String, turn: String) {
        val map = _maps.value.find { it.id == mapId } ?: return
        if (map.status != MapStatus.AVAILABLE) return

        val turnLabel = if (turn == "A") "Capitão A" else "Capitão B"
        val captainName = if (turn == "A") _captainA.value?.name else _captainB.value?.name

        _maps.value = _maps.value.map {
            if (it.id == mapId) it.copy(status = MapStatus.BANNED, actionedBy = turnLabel)
            else it
        }

        logBotMessage("❌ $turnLabel (@$captainName) baniu: ${map.name}", MessageType.INFO)

        // Toggle turn
        _vetoTurn.value = if (turn == "A") "B" else "A"
    }

    fun pickMap(mapId: String, turn: String) {
        val map = _maps.value.find { it.id == mapId } ?: return
        if (map.status != MapStatus.AVAILABLE) return

        val turnLabel = if (turn == "A") "Capitão A" else "Capitão B"
        val captainName = if (turn == "A") _captainA.value?.name else _captainB.value?.name

        _maps.value = _maps.value.map {
            if (it.id == mapId) it.copy(status = MapStatus.PICKED, actionedBy = turnLabel)
            else it
        }

        _pickedMaps.value = _pickedMaps.value + map.copy(status = MapStatus.PICKED, actionedBy = turnLabel)
        logBotMessage("🗺️ $turnLabel (@$captainName) escolheu (PICK): ${map.name}", MessageType.SUCCESS)

        // Toggle turn
        _vetoTurn.value = if (turn == "A") "B" else "A"
    }

    // Divided teams automatically
    fun executeAutoTeams() {
        _botState.value = BotState.TEAM_DIVIDING
        logBotMessage("⚡ Executando comando /times auto...", MessageType.COMMAND)

        val capA = _captainA.value
        val capB = _captainB.value

        if (capA == null || capB == null) {
            logBotMessage("Erro /times auto: Ambos os capitães precisam estar previamente definidos.", MessageType.INFO)
            return
        }

        // Remaining 8 players to distribute
        val remainingPlayers = _players.value.filter { it.id != capA.id && it.id != capB.id }

        // Snake draft or rating balanced distribution
        // Sort remaining players by rating descending
        val sortedRemaining = remainingPlayers.sortedByDescending { it.rating }

        val listA = mutableListOf<Player>()
        val listB = mutableListOf<Player>()

        // Snake distribution to balance skills:
        // A, B, B, A, A, B, B, A
        sortedRemaining.forEachIndexed { index, player ->
            when (index) {
                0, 3, 4, 7 -> {
                    listA.add(player.copy(team = "A"))
                }
                1, 2, 5, 6 -> {
                    listB.add(player.copy(team = "B"))
                }
            }
        }

        val fullTeamA = listOf(capA) + listA
        val fullTeamB = listOf(capB) + listB

        _teamA.value = fullTeamA
        _teamB.value = fullTeamB

        // Print rosters
        logBotMessage("👥 Times equilibrados gerados via /times auto!", MessageType.SUCCESS)
        logBotMessage("🔵 TIME CAPITÃO A (@${capA.name}): " + fullTeamA.joinToString { it.name }, MessageType.INFO)
        logBotMessage("🍊 TIME CAPITÃO B (@${capB.name}): " + fullTeamB.joinToString { it.name }, MessageType.INFO)

        val avgA = fullTeamA.map { it.rating }.average()
        val avgB = fullTeamB.map { it.rating }.average()
        logBotMessage("📊 Média de Habilidade: Time A (Rating: ${String.format("%.1f", avgA)}) VS Time B (Rating: ${String.format("%.1f", avgB)})", MessageType.INFO)

        _botState.value = BotState.FINISHED
        logBotMessage("🚀 Partida pronta para jogar! Bom jogo a todos do lobby!", MessageType.SUCCESS)
    }

    // Add Player manually
    fun addPlayer(name: String) {
        val cleanName = name.trim()
        if (cleanName.isEmpty()) return

        if (_players.value.size >= 10) {
            logBotMessage("Fila está lotada (10/10). Digite /limpar ou inicie o veto.", MessageType.INFO)
            return
        }

        if (_players.value.any { it.name.lowercase() == cleanName.lowercase() }) {
            logBotMessage("O jogador '$cleanName' já está na fila.", MessageType.INFO)
            return
        }

        addPlayerInternal(cleanName)
    }

    private fun addPlayerInternal(name: String) {
        val newPlayer = Player(name = name)
        _players.value = _players.value + newPlayer
        logBotMessage("📥 @$name entrou na fila! (${_players.value.size}/10)", MessageType.INFO)

        if (_players.value.size == 10 && _botState.value == BotState.IDLE) {
            _botState.value = BotState.QUEUE_WAITING
            logBotMessage("👥 Fila completa! Digite /capitão auto ou clique em Iniciar para continuar.", MessageType.SYSTEM)
        }
    }

    fun removePlayer(playerId: String) {
        val player = _players.value.find { it.id == playerId } ?: return
        _players.value = _players.value.filter { it.id != playerId }
        logBotMessage("📤 @${player.name} saiu da fila. (${_players.value.size}/10)", MessageType.INFO)

        if (_players.value.size < 10 && _botState.value != BotState.IDLE) {
            _botState.value = BotState.IDLE
            cancelActiveAutomation()
        }
    }

    private fun removePlayerByName(name: String) {
        val player = _players.value.find { it.name.lowercase() == name.lowercase() }
        if (player != null) {
            removePlayer(player.id)
        } else {
            logBotMessage("Jogador '$name' não pôde ser encontrado na fila.", MessageType.INFO)
        }
    }

    fun resetLobby() {
        cancelActiveAutomation()
        _players.value = emptyList()
        _captainA.value = null
        _captainB.value = null
        _teamA.value = emptyList()
        _teamB.value = emptyList()
        _maps.value = defaultMaps.map { it.copy(status = MapStatus.AVAILABLE, actionedBy = null) }
        _pickedMaps.value = emptyList()
        _botState.value = BotState.IDLE
        _isAutomated.value = false
        logBotMessage("Lobby redefinido. Digite /md1 auto para simular o bot de forma totalmente automatizada.", MessageType.SYSTEM)
    }

    private fun cancelActiveAutomation() {
        automationJob?.cancel()
        automationJob = null
        _isAutomated.value = false
    }

    override fun onCleared() {
        super.onCleared()
        cancelActiveAutomation()
    }
}
