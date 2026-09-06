package com.wordwaverise.wordwaveriseapp.presentation.search

import android.media.MediaPlayer
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.wordwaverise.wordwaveriseapp.data.remote.dto.DefinitionDto
import com.wordwaverise.wordwaveriseapp.data.remote.dto.PronunciationDto
import com.wordwaverise.wordwaveriseapp.data.remote.dto.WordDetailResponse
import com.wordwaverise.wordwaveriseapp.data.remote.dto.WordDto
import com.wordwaverise.wordwaveriseapp.data.repository.SearchRepository
import com.wordwaverise.wordwaveriseapp.data.local.SettingsDataStore
import com.wordwaverise.wordwaveriseapp.data.repository.CategoryRepository
import com.wordwaverise.wordwaveriseapp.util.Resource
import kotlinx.coroutines.flow.first
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchRepository: SearchRepository,
    private val savedWordsRepository: com.wordwaverise.wordwaveriseapp.data.repository.SavedWordsRepository,
    private val flashcardRepository: com.wordwaverise.wordwaveriseapp.data.repository.FlashcardRepository,
    private val categoryRepository: CategoryRepository,
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    init {
        // Папки нужны раньше первого сохранения: диалог, открывшийся пустым и заполнившийся
        // через полсекунды, читается как «папок нет».
        viewModelScope.launch {
            categoryRepository.categories.collect { folders ->
                _state.value = _state.value.copy(ownFolders = folders.filter { !it.readOnly })
            }
        }
    }

    companion object {
        private const val TAG = "SearchViewModel"
    }

    private val _state = mutableStateOf(SearchState())
    val state: State<SearchState> = _state


    /** Значение статьи, к которому привязано слово: статья открывает его первым. */
    /** Значения, которые человек сохранил. Каждое — отдельное слово в его словаре. */
    private val _pinnedSenseIds = mutableStateOf<Set<String>>(emptySet())
    val pinnedSenseIds: State<Set<String>> = _pinnedSenseIds

    private var mediaPlayer: MediaPlayer? = null
    private var suggestJob: Job? = null

    /**
     * Поиск, которому принадлежит экран прямо сейчас.
     *
     * ⚠️ Отменяется в начале следующего, и это не оптимизация. Холодная статья пишется одну-три
     * минуты, а `lookup` — это поток, который всё это время опрашивает сервер и пишет в
     * состояние. Без отмены брошенный поиск продолжал жить: искали «grow up», через секунду
     * «cat», и статья про «grow up» приезжала поверх «cat» — вместе со звездой сохранённости и
     * выбранными значениями чужого слова.
     */
    private var searchJob: Job? = null
    private var analysisJob: Job? = null

    fun onSearchQueryChange(query: String) {
        _state.value = _state.value.copy(
            searchQuery = query,
            error = null,
            suggestions = emptyList()
        )
        suggestJob?.cancel()
        // Автодополнение — для английского ввода. Русский отвечает сам поиск, объяснёнными
        // вариантами, поэтому тянуть под него голые строки при вводе незачем.
        //
        // ⚠️ Запрет на пробел снят: фразы (`grow up`, `come across`) — полноправные статьи
        // корпуса, и правило «только одно слово» вычёркивало из подсказок ровно те выражения,
        // написание которых человек как раз и не помнит.
        val isEnglishInput = query.trim().length >= 2 && query.none { it in 'Ѐ'..'ӿ' }
        if (isEnglishInput) {
            suggestJob = viewModelScope.launch {
                delay(220)
                fetchSuggestions(query, prefix = true)
            }
        }
    }

    /**
     * One entry point for every kind of input.
     *
     * The server decides what the query is — a word, a typo, an inflected form, a phrase, a
     * sentence, or Russian — so the client no longer sniffs for Cyrillic or guesses at intent.
     */
    /**
     * @param exact skip the server's resolver entirely — see [searchOriginalQuery].
     */
    fun searchWord(exact: Boolean = false) {
        val query = _state.value.searchQuery.trim()
        if (query.isEmpty()) {
            _state.value = _state.value.copy(error = "Пожалуйста, введите слово для поиска")
            return
        }

        Log.d(TAG, "Looking up: $query")

        // ⚠️ Сохранённость и выбранное значение принадлежат предыдущему слову и должны уйти
        // вместе с ним. Идентификаторы значений («n1», «v1») у разных слов совпадают сплошь и
        // рядом, поэтому оставшийся пин подсвечивал «Ваше значение» в статье, к которой он не
        // имеет отношения; а если статья новому слову так и не пришла, звезда продолжала
        // утверждать, что оно сохранено.
        _pinnedSenseIds.value = emptySet()

        // Предыдущий поиск с этого момента никого не касается — вместе с его опросом статьи
        // и вместе с проверкой сохранённости, которая живёт его же корутиной.
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _state.value = _state.value.copy(
                isLoading = true,
                error = null,
                notice = null,
                wordData = null,
                entry = null,
                annotationPending = false,
                annotationDegraded = false,
                hasSearched = false,
                suggestions = emptyList(),
                isRussianSearch = false,
                russianQuery = "",
                ruEnCandidates = emptyList(),
                ruEnNote = null,
                ruEnAmbiguous = false,
                sentenceText = "",
                sentenceTokens = emptyList(),
                selectedTokenIndex = null,
                contextAnalysis = null
            )

            // Collected rather than awaited: a cold word streams an immediate raw response and
            // then the finished article, so each emission replaces the last on screen.
            searchRepository.lookup(query, exact = exact).collect { result ->
                when (result) {
                    is Resource.Success -> {
                        val data = result.data ?: return@collect
                        _state.value = _state.value.copy(
                            isLoading = false,
                            hasSearched = true,
                            error = null,
                            notice = data.notice,
                            entry = data.entry,
                            annotationPending = data.annotationStatus == "PENDING",
                            annotationDegraded = data.annotationStatus == "DEGRADED",
                            wordData = data.raw?.toWordDto(),
                            // A sentence has no headword — its words become tappable instead.
                            sentenceText = data.tokenized?.text.orEmpty(),
                            sentenceTokens = data.tokenized?.tokens.orEmpty(),
                            isRussianSearch = data.ruEn != null,
                            russianQuery = if (data.ruEn != null) query else "",
                            ruEnCandidates = data.ruEn?.candidates.orEmpty(),
                            ruEnNote = data.ruEn?.note,
                            ruEnAmbiguous = data.ruEn?.isAmbiguous ?: false
                        )

                        // Дочерней корутиной, а не из viewModelScope: иначе она переживает
                        // отмену поиска и подсвечивает значения предыдущего слова.
                        data.entry?.lemma?.takeIf { it.isNotBlank() }?.let { lemma ->
                            launch { checkIfWordIsSaved(lemma) }
                        }

                        // Nothing to show at all — offer alternatives rather than a bare error.
                        val empty = data.entry == null && data.raw == null &&
                            data.ruEn == null && data.tokenized == null
                        if (empty) {
                            _state.value = _state.value.copy(error = "Слово не найдено")
                            fetchSuggestions(query, prefix = false)
                        }
                    }
                    is Resource.Error -> {
                        Log.e(TAG, "Lookup error: ${result.message}")
                        _state.value = _state.value.copy(
                            isLoading = false,
                            error = result.message,
                            hasSearched = true
                        )
                        fetchSuggestions(query, prefix = false)
                    }
                    is Resource.Loading -> _state.value = _state.value.copy(isLoading = true)
                }
            }

            // Stop the spinner if the article never landed; the sources view stays usable.
            if (_state.value.annotationPending) {
                _state.value = _state.value.copy(annotationPending = false)
            }
        }
    }

    /**
     * Asks what one word means in the sentence the user pasted.
     * The index refers to the server's own tokenisation, which shipped with the lookup.
     */
    fun analyzeToken(index: Int) {
        val text = _state.value.sentenceText
        if (text.isBlank()) return
        // Тапнуть по второму слову, не дождавшись первого, — обычное дело: без отмены разбор
        // первого приезжает поверх второго и подписывается выбранным словом.
        analysisJob?.cancel()
        analysisJob = viewModelScope.launch {
            _state.value = _state.value.copy(
                selectedTokenIndex = index,
                isAnalyzingContext = true,
                contextAnalysis = null
            )
            when (val result = searchRepository.analyzeInContext(text, index)) {
                is Resource.Success -> _state.value = _state.value.copy(
                    contextAnalysis = result.data, isAnalyzingContext = false
                )
                is Resource.Error -> _state.value = _state.value.copy(
                    isAnalyzingContext = false, error = result.message
                )
                is Resource.Loading -> Unit
            }
        }
    }

    fun dismissContextAnalysis() {
        _state.value = _state.value.copy(selectedTokenIndex = null, contextAnalysis = null)
    }

    fun selectSuggestion(suggestion: String) {
        _state.value = _state.value.copy(
            searchQuery = suggestion,
            suggestions = emptyList(),
            isRussianSearch = false,
            russianQuery = "",
            ruEnCandidates = emptyList()
        )
        searchWord()
    }

    /**
     * Re-runs the search for exactly what was typed, overriding whatever the resolver decided.
     *
     * Repeating the query was not enough: the same input took the same rungs and produced the
     * same answer, so «Искать точно» could never override anything. The override has to reach
     * the server, where the decision is actually made.
     */
    fun searchOriginalQuery(original: String) {
        _state.value = _state.value.copy(searchQuery = original, notice = null)
        searchWord(exact = true)
    }

    /**
     * Подсказки печатаются быстрее, чем отвечает сеть, поэтому у них своя единственная корутина.
     *
     * Без отмены ответ на «cat» мог приехать после ответа на «catal» и подставить список от трёх
     * букв назад — под курсором, стоящим уже на другом слове.
     */
    private fun fetchSuggestions(query: String, prefix: Boolean = false) {
        suggestJob?.cancel()
        suggestJob = viewModelScope.launch {
            _state.value = _state.value.copy(isFetchingSuggestions = true)
            val suggestions = searchRepository.getSuggestions(query, prefix = prefix)
            _state.value = _state.value.copy(
                suggestions = suggestions,
                isFetchingSuggestions = false
            )
        }
    }

    fun clearSearch() {
        stopAudio()
        // Крестик обязан отменять поиск, а не только стирать экран: иначе статья, которую уже
        // ждали, приезжает на очищенный экран через минуту после нажатия.
        searchJob?.cancel()
        suggestJob?.cancel()
        analysisJob?.cancel()
        _state.value = SearchState()
        _pinnedSenseIds.value = emptySet()
    }

    fun playAudio(url: String) {
        if (_state.value.playingAudioUrl == url && _state.value.isPlayingAudio) {
            stopAudio()
            return
        }
        viewModelScope.launch(Dispatchers.Main) {
            try {
                _state.value = _state.value.copy(isPlayingAudio = true, playingAudioUrl = url)
                mediaPlayer?.release()
                mediaPlayer = null
                val mp = MediaPlayer()
                mediaPlayer = mp
                mp.setDataSource(url)
                mp.setOnPreparedListener { it.start() }
                mp.setOnCompletionListener {
                    _state.value = _state.value.copy(isPlayingAudio = false, playingAudioUrl = null)
                }
                mp.setOnErrorListener { _, _, _ ->
                    _state.value = _state.value.copy(isPlayingAudio = false, playingAudioUrl = null)
                    true
                }
                mp.prepareAsync()
            } catch (e: Exception) {
                Log.e(TAG, "Audio playback error: ${e.message}")
                _state.value = _state.value.copy(isPlayingAudio = false, playingAudioUrl = null)
            }
        }
    }

    fun stopAudio() {
        try {
            mediaPlayer?.let { if (it.isPlaying) it.stop() }
        } catch (_: Exception) { }
        mediaPlayer?.release()
        mediaPlayer = null
        _state.value = _state.value.copy(isPlayingAudio = false, playingAudioUrl = null)
    }

    override fun onCleared() {
        super.onCleared()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    /**
     * Saves the word, preferring the annotated article.
     *
     * Its Russian is written per sense, so the saved word and the flashcard made from it carry a
     * translation that actually matches the definition sitting next to it.
     */
    /**
     * The bookmark on one sense of the article.
     *
     * Каждое значение сохраняется само по себе: отметили два определения — в словаре два
     * слова, у каждого свой перевод, свой пример и своя карточка. Раньше вторая закладка
     * переставляла привязку первой — то есть выглядела как добавление, а была отменой
     * предыдущего выбора. Снятие закладки убирает только эту запись.
     *
     * The definition travelling with the request is a fallback for a word whose article the
     * server has not written yet; when the corpus knows the sense, the server's own text wins.
     */
    fun toggleSense(senseId: String) {
        val entry = _state.value.entry ?: return
        val word = entry.lemma.takeIf { it.isNotBlank() } ?: _state.value.wordData?.word ?: return

        // Снятие закладки ничего не спрашивает: убрать — это уже ответ.
        if (senseId in _pinnedSenseIds.value) {
            unsaveSense(word, senseId)
            return
        }

        val sense = entry.posGroups.flatMap { it.senses }.firstOrNull { it.id == senseId } ?: return

        // Сохранение спрашивает, куда. Папки прошлого раза предлагаются отмеченными: слова
        // собирают подряд в один урок, и повторять один и тот же выбор двадцать раз — это не
        // выбор, а работа. ⚠️ Сверяется с текущим списком: папку могли удалить на другом
        // устройстве, и «сохранить в несуществующую» — это молчаливая потеря слова.
        viewModelScope.launch {
            val known = _state.value.ownFolders.map { it.id }.toSet()
            val remembered = settingsDataStore.lastSaveFolders.first().filter { it in known }
            _state.value = _state.value.copy(
                pendingSenseId = senseId,
                pendingSenseSummary = sense.translationsRu.joinToString(", ")
                    .takeIf { it.isNotBlank() }
                    ?: sense.definitionEn.takeIf { it.isNotBlank() }
                    ?: sense.definitionRu.takeIf { it.isNotBlank() },
                chosenFolders = remembered
            )
        }
    }

    /** Отметить папку в диалоге сохранения или снять отметку. Пусто — «без папки». */
    fun toggleSaveFolder(id: Long) {
        val chosen = _state.value.chosenFolders
        _state.value = _state.value.copy(
            chosenFolders = if (id in chosen) chosen - id else chosen + id
        )
    }

    fun cancelSaveSense() {
        _state.value = _state.value.copy(pendingSenseId = null, pendingSenseSummary = null)
    }

    /**
     * Заводит папку прямо из диалога и сразу её отмечает.
     *
     * Без этого человек без единой папки упирается в диалог, в котором нечего выбрать, — то
     * есть в тупик ровно там, где его позвали выбирать.
     */
    fun createFolderForSave(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            when (val result = categoryRepository.createCategory(trimmed)) {
                is Resource.Success -> {
                    val created = result.data ?: return@launch
                    _state.value = _state.value.copy(
                        chosenFolders = _state.value.chosenFolders + created.id
                    )
                }
                is Resource.Error -> Log.w(TAG, "Failed to create folder: " + result.message)
                else -> {}
            }
        }
    }

    /**
     * Сохраняет значение в выбранные папки.
     *
     * Папки едут вместе с сохранением, а не вторым запросом: слово, которое сначала легло
     * «никуда», а потом переехало, при обрыве между этими двумя шагами остаётся не там, куда
     * его клали, — и человек об этом не узнаёт.
     */
    fun confirmSaveSense() {
        val senseId = _state.value.pendingSenseId ?: return
        val entry = _state.value.entry ?: return
        val word = entry.lemma.takeIf { it.isNotBlank() } ?: _state.value.wordData?.word ?: return
        val sense = entry.posGroups.flatMap { it.senses }.firstOrNull { it.id == senseId } ?: return
        val group = entry.posGroups.firstOrNull { g -> g.senses.any { it.id == senseId } }
        val translation = sense.translationsRu.firstOrNull()
        val definition = sense.definitionEn.takeIf { it.isNotBlank() }
            ?: sense.definitionRu.takeIf { it.isNotBlank() }
        val example = sense.examples.firstOrNull()?.en
        val chosen = _state.value.chosenFolders

        viewModelScope.launch {
            _state.value = _state.value.copy(isSavingSense = true)
            val folders = _state.value.ownFolders.filter { it.id in chosen }
            val result = savedWordsRepository.saveWord(
                word = word,
                translation = translation,
                definition = definition,
                senseId = senseId,
                categoryLocalIds = folders.map { it.id },
                // ⚠️ Папка, заведённая офлайн, серверного id ещё не имеет: на сервер она
                // доедет со следующей синхронизацией, а слово ляжет в неё уже сейчас.
                categoryServerIds = folders.mapNotNull { it.serverId }
            )
            when (result) {
                is Resource.Success -> {
                    _pinnedSenseIds.value = _pinnedSenseIds.value + senseId
                    settingsDataStore.setLastSaveFolders(chosen)
                    if (definition != null) {
                        flashcardRepository.createFlashcard(
                            word = word,
                            definition = definition,
                            example = example,
                            translation = translation,
                            phonetic = entry.phonetic ?: _state.value.wordData?.phonetic,
                            partOfSpeech = group?.pos,
                            senseId = senseId
                        )
                    }
                    _state.value = _state.value.copy(
                        pendingSenseId = null,
                        pendingSenseSummary = null,
                        isSavingSense = false
                    )
                }
                is Resource.Error -> {
                    Log.e(TAG, "Failed to pin sense")
                    _state.value = _state.value.copy(isSavingSense = false)
                }
                else -> _state.value = _state.value.copy(isSavingSense = false)
            }
        }
    }

    /** Убирает одно значение; остальные значения того же слова остаются сохранёнными. */
    private fun unsaveSense(word: String, senseId: String) {
        viewModelScope.launch {
            val entry = savedWordsRepository.entryFor(word, senseId)
            val result =
                if (entry != null) savedWordsRepository.deleteEntry(entry)
                // Записи под этим значением на телефоне нет — снять можно только слово целиком,
                // что и было единственным вариантом до появления записей.
                else savedWordsRepository.deleteWord(word)
            when (result) {
                is Resource.Success -> {
                    _pinnedSenseIds.value = _pinnedSenseIds.value - senseId
                }
                is Resource.Error -> Log.e(TAG, "Failed to remove sense")
                else -> {}
            }
        }
    }

    /**
     * Какие значения этого слова уже в словаре.
     *
     * ⚠️ «Сохранено ли слово целиком» больше не спрашивается: сохраняются значения, и ответ на
     * старый вопрос ничего не значил бы ни для одной закладки в статье.
     */
    private suspend fun checkIfWordIsSaved(word: String) {
        _pinnedSenseIds.value = savedWordsRepository.pinnedSenseIds(word).toSet()
    }
}

/**
 * Adapts the v2 raw aggregate to the shape the existing sources view renders.
 *
 * The sources tabs were built against the legacy search response; converting here keeps that
 * whole rendering path untouched while the article becomes the primary view.
 */
private fun WordDetailResponse.toWordDto(): WordDto = WordDto(
    word = word,
    phonetic = phonetic,
    audioUrl = audioUrl,
    // Same three fields, two declarations — the DTO split predates this screen.
    pronunciations = pronunciations.map { PronunciationDto(it.region, it.ipa, it.audioMp3Url) },
    translation = translation,
    definitions = definitions.map { def ->
        DefinitionDto(
            partOfSpeech = def.partOfSpeech,
            definition = def.definition,
            example = def.example,
            synonyms = synonyms.take(5),
            antonyms = antonyms.take(5),
            source = def.source
        )
    }
)
