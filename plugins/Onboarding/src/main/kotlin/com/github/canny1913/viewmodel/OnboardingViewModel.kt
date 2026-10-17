package com.github.canny1913.viewmodel

import android.view.View
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.aliucord.Logger
import com.aliucord.utils.RxUtils
import com.aliucord.utils.RxUtils.subscribe
import com.aliucord.wrappers.GuildRoleWrapper.Companion.name
import com.discord.api.role.GuildRole
import com.discord.app.AppComponent
import com.discord.models.domain.ModelMemberVerificationForm
import com.discord.models.domain.emoji.ModelEmojiCustom
import com.discord.restapi.RestAPIParams
import com.discord.stores.StoreStream
import com.discord.utilities.mg_recycler.MGRecyclerAdapterSimple
import com.discord.utilities.rest.RestAPI
import com.discord.utilities.rx.ObservableExtensionsKt
import com.discord.utilities.textprocessing.DiscordParser
import com.discord.utilities.textprocessing.MessageParseState
import com.discord.views.LoadingButton
import com.github.canny1913.OnboardingModel
import com.github.canny1913.OnboardingPrompt
import com.github.canny1913.OnboardingResponseRequest
import com.github.canny1913.PluginRestAPI
import com.github.canny1913.adapter.items.OnboardingItem
import com.github.canny1913.adapter.items.OnboardingItemHeader
import com.github.canny1913.adapter.items.OnboardingItemPromptOption
import com.github.canny1913.adapter.items.OnboardingItemTerm
import com.github.canny1913.adapter.items.OnboardingItemTermHeader
import com.github.canny1913.createRESTApiExtension
import com.github.canny1913.isAtBottom
import rx.Observable
import com.discord.models.domain.ModelMemberVerificationForm.MemberVerificationFieldType as VerificationFieldType


class OnboardingViewModel(
    val guildId: Long
) : DiscordViewModel<OnboardingViewModel.ViewState>(ViewState.Loading()) {
    private var termsParser = DiscordParser.createParser(false, true, false, false, false)!!

    /**
     * Collection that holds identifiers of onboarding prompt options user has chosen.
     * Key is the index of [OnboardingPrompt].
     * Value is a map of option ID's with values being whether user selected them.
     */
    private val promptSelectedMap: HashMap<Int, HashMap<Long, Boolean>> = hashMapOf()
    private val pluginRestAPI: PluginRestAPI =
        createRESTApiExtension(RestAPI.AppHeadersProvider.authTokenProvider.invoke(), rnSuperProps = true)

    init {
        val storeStream = StoreStream.Companion!!
        val currentUserId = storeStream.users.me.id
        storeStream.guilds.observeGuildMember(guildId, currentUserId)
            .withComputation()
            .A { member ->  // Observable.flatMap
                if (member == null) { // exit if guild is inaccessible
                    updateViewState(ViewState.Completed())
                    return@A null
                }

                if (member.pending) {
                    observeStoresWithVerification(guildId)
                } else {
                    observeStoresWithoutVerification(guildId)
                }

            }
            .loggerSubscribe { it?.let { state -> handleStoreState(state) } }
    }

    fun onNextClick(nextView: View, scrollView: NestedScrollView) {
        val viewState = getViewState() as? ViewState.Loaded ?: return
        val nextButton = nextView as LoadingButton
        // to ensure user reads all rules, scroll to bottom if not already scrolled
        if (!scrollView.isAtBottom()) {
            scrollView.smoothScrollTo(0, scrollView.getChildAt(0).bottom)
            return
        }

        nextButton.isClickable = false
        if (viewState.inTermsPage()) {
            nextButton.setIsLoading(true)
            completeOnboardingWithVerification()
            return
        }
        val nextPrompt = viewState.currentPrompt + 1
        if (nextPrompt == viewState.prompts.size) setupTerms() else advancePrompt()
    }

    fun onOptionClick(index: Int, id: Long) {
        val viewState = getViewState() as? ViewState.Loaded ?: return
        val prompt = viewState.prompts[index]
        val selectedOptions = promptSelectedMap[index]!!
        val alreadySelected = selectedOptions[id] ?: false
        if (prompt.singleSelect && selectedOptions.isNotEmpty()) {
            selectedOptions.transformAll { false }
        }
        selectedOptions[id] = !alreadySelected
        val newState = viewState.apply {
            disabled = !selectedOptions.values.any { it } && prompt.required
            items = getPromptItems(index, viewState.prompts)
        }
        updateViewState(newState)
    }

    fun onBackPress(): Boolean {
        val viewState = getViewState() as? ViewState.Loaded
        if (viewState == null || viewState.currentPrompt == 0) {
            updateViewState(ViewState.Completed())
        } else {
            deAdvancePrompt()
        }
        return true
    }

    fun fetchSelectedRoles(): List<String>? {
        val viewState = getViewState() as? ViewState.Loaded ?: return null
        if (viewState.inTermsPage()) return null

        val currentPrompt = viewState.prompts[viewState.currentPrompt]
        val selectedOptionIds = promptSelectedMap[viewState.currentPrompt]
            ?.filter { (_, selected) -> selected }
            ?.keys ?: return null
        val selectedRoles = currentPrompt.options
            .asSequence()
            .filter { it.id in selectedOptionIds }
            .flatMap { it.roleIds.asSequence() }
            .toHashSet()

        val roleList = mutableListOf<String>()
        for (roleId in selectedRoles) {
            val role = viewState.roles?.get(roleId) ?: continue
            // manually add prefix here to bold-ify later
            roleList.add("@${role.name}")
        }
        return roleList.takeUnless { it.isEmpty() }
    }
    fun advancePrompt() {
        updatePromptList(true)
    }

    fun deAdvancePrompt() {
        updatePromptList(false)
    }

    private fun updatePromptList(advance: Boolean) {
        val viewState = getViewState() as? ViewState.Loaded ?: return
        if (advance) viewState.currentPrompt++ else viewState.currentPrompt--
        val i = viewState.currentPrompt
        if (i in 0..viewState.prompts.size) {
            val newState = viewState.apply {
                disabled = promptSelectedMap[i].isNullOrEmpty() && viewState.prompts[i].required
                items = getPromptItems(i, viewState.prompts)
            }
            updateViewState(newState)
        }
    }

    fun setupTerms() {
        val viewState = getViewState() as? ViewState.Loaded ?: return
        if (viewState.verifyForm == null) return completeOnboarding()
        viewState.currentPrompt++
        val newItems = mutableListOf<OnboardingItem>()
        val termsField = viewState.verifyForm!!.formFields.find {
            it.memberVerificationFieldType == VerificationFieldType.TERMS
        } ?: return

        newItems += OnboardingItemTermHeader()
        termsField.values?.forEachIndexed { i, value ->
            newItems += OnboardingItemTerm(
                index = i,
                ast = termsParser.parse(value, MessageParseState.Companion!!.initialState),
                channelNames = viewState.channels!!,
                roles = viewState.roles!!,
                isFirstItem = i == 0,
                isLastItem = i == termsField.values.size - 1,
                allowAnimatedEmojis = false
            )
            val newState = viewState.apply {
                disabled = false
                items = newItems
            }
            updateViewState(newState)
        }
    }

    private fun getPromptItems(index: Int, onboardingPrompts: List<OnboardingPrompt>): List<OnboardingItem> {
        val newItems = mutableListOf<OnboardingItem>()
        val totalPrompts = onboardingPrompts.size
        val prompt = onboardingPrompts[index]
        newItems += OnboardingItemHeader(
            currentQuestion = index,
            questionCount = totalPrompts,
            question = prompt.title,
            required = prompt.required
        )

        for (option in prompt.options) {
            val selectedMap = promptSelectedMap.orDefault(index, hashMapOf())
            val selected = selectedMap.orDefault(option.id, false)
            newItems += OnboardingItemPromptOption(
                id = option.id,
                promptIndex = index,
                emoji = ModelEmojiCustom(option.emoji, guildId),
                title = option.title,
                description = option.description,
                roleIds = option.roleIds,
                selected = selected
            )
        }
        return newItems
    }


    private fun handleStoreState(state: StoreState) {
        if (state is StoreState.Loaded) {
            if (getViewState() is ViewState.Loaded) return

            val onboardingPrompts = state.onboardingResponse.prompts.filter(OnboardingPrompt::inOnboarding)
            if (onboardingPrompts.isEmpty()) return
            updateViewState(
                ViewState.Loaded(
                    disabled = onboardingPrompts.first().required,
                    items = getPromptItems(0, onboardingPrompts),
                    verifyForm = state.verificationForm,
                    prompts = onboardingPrompts,
                    channels = state.channels,
                    roles = state.roles,
                ))
        } else {
            updateViewState(ViewState.Invalid())
        }
    }

    private fun completeOnboardingWithVerification() {
        val viewState = getViewState() as? ViewState.Loaded ?: return
        val verifyForm = viewState.verifyForm ?: return
        verifyForm
            .formFields
            .find { it.memberVerificationFieldType == VerificationFieldType.TERMS }
            .apply { this?.response = true }
        val form = RestAPIParams.MemberVerificationForm(verifyForm.formFields, verifyForm.version)
        pluginRestAPI.createGuildJoinRequest(guildId, form).withComputation()
            .loggerSubscribe { completeOnboarding() }
    }

    private fun completeOnboarding() {
        val viewState = getViewState() as? ViewState.Loaded ?: return
        val time = System.currentTimeMillis()
        val onboardingResponse =
            OnboardingResponseRequest(
                onboardingResponses = promptSelectedMap.values.flatMap { map -> map.filter { it.value }.keys.toList() }
                .sorted(),
                onboardingPromptsSeen = viewState.prompts.associate { it.id to time }.toSortedMap(),
                onboardingResponsesSeen = viewState.prompts.flatMap { it.options.toList() }.associate { it.id to time }
                    .toSortedMap()
            )
        pluginRestAPI.createOnboardingResponses(guildId, onboardingResponse).withComputation()
            .loggerSubscribe {
                updateViewState(ViewState.Completed())
            }
    }

    sealed interface ViewState {
        class Invalid : ViewState
        class Loading : ViewState
        class Completed : ViewState
        class Loaded(
            var disabled: Boolean,
            var items: List<OnboardingItem>,
            var verifyForm: ModelMemberVerificationForm?,
            var prompts: List<OnboardingPrompt>,
            var currentPrompt: Int = 0,
            val channels: Map<Long, String>?,
            val roles: Map<Long, GuildRole>?
        ) : ViewState {
            fun inTermsPage() = currentPrompt == prompts.size
            fun isLast() = (verifyForm == null && prompts.size - 1 == currentPrompt) || inTermsPage()
        }
    }

    sealed interface StoreState {
        class Loaded(
            val onboardingResponse: OnboardingModel,
            val verificationForm: ModelMemberVerificationForm?,
            val channels: Map<Long, String>?,
            val roles: Map<Long, GuildRole>?,
        ) : StoreState
    }

    private fun observeStoresWithVerification(guildId: Long): Observable<StoreState> {
        val storeStream = StoreStream.Companion
        return Observable<StoreState>.h( // Observable.merge
            pluginRestAPI.getOnboarding(guildId),
            pluginRestAPI.getGuildMemberVerification(guildId, false),
            storeStream.channels.observeNames(),
            storeStream.guilds.observeRoles(guildId)
        ) { onboardingData, verificationForm, channels, roles ->
            StoreState.Loaded(
                onboardingData, verificationForm, channels, roles
            )
        }
    }

    private fun observeStoresWithoutVerification(guildId: Long): Observable<StoreState> {
        val storeStream = StoreStream.Companion
        return Observable<StoreState>.j( // Observable.merge
            pluginRestAPI.getOnboarding(guildId), storeStream.guilds.observeRoles(guildId)
        ) { onboardingData, roles ->
            StoreState.Loaded(
                onboardingData, null, null, roles
            )
        }
    }

    companion object {
        private val logger = Logger("OnboardingVM")

        fun provideFactory(
            guildId: Long,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(
                modelClass: Class<T>,
            ): T {
                return OnboardingViewModel(guildId) as T
            }
        }
        fun <T> Observable<T>.uiWithLifecycle(component: AppComponent? = null, adapter: MGRecyclerAdapterSimple<*>? = null): Observable<T> = ObservableExtensionsKt.ui<T>(this, component, adapter)

        fun <T> Observable<T>.withComputation(): Observable<T> = ObservableExtensionsKt.computationLatest<T>(this)

        fun <T> Observable<T>.loggerSubscribe(
            onError: (Throwable) -> Unit = {}, onCompleted: () -> Unit = {}, onNext: (T?) -> Unit
        ) = RxUtils.createActionSubscriber(
            onNext, onError = {
                logger.error(it)
                onError(it)
            }, onCompleted
        ).apply { subscribe(this) }

        fun <K, V> MutableMap<K, V>.orDefault(key: K, defaultValue: V): V {
            if (this[key] == null) {
                this[key] = defaultValue
            }
            return this[key]!!
        }

        inline fun <K, V> MutableMap<K, V>.transformAll(transform: (V) -> V) {
            for (key in keys) {
                this[key] = transform(this[key]!!)
            }
        }
    }
}

