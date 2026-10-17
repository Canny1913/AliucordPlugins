package com.github.canny1913

import com.aliucord.Utils
import com.aliucord.utils.RNSuperProperties
import com.discord.BuildConfig
import com.discord.api.emoji.GuildEmoji
import com.discord.models.domain.ModelMemberVerificationForm
import com.discord.models.domain.ModelMemberVerificationFormResponse
import com.discord.restapi.RequiredHeadersInterceptor
import com.discord.restapi.RestAPIBuilder
import com.discord.restapi.RestAPIParams
import com.discord.utilities.rest.RestAPI
import com.franmontiel.persistentcookiejar.PersistentCookieJar
import com.franmontiel.persistentcookiejar.cache.SetCookieCache
import com.franmontiel.persistentcookiejar.persistence.SharedPrefsCookiePersistor
import i0.f0.a
import i0.f0.b
import i0.f0.c
import i0.f0.d
import i0.f0.f
import i0.f0.g
import i0.f0.j
import i0.f0.k
import i0.f0.m
import i0.f0.n
import i0.f0.o
import i0.f0.p
import i0.f0.s
import i0.f0.t
import i0.f0.u
import i0.f0.y
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import rx.Observable
import java.util.TimeZone

fun createRESTApiExtension(token: String, rnSuperProps: Boolean = false): PluginRestAPI {
    val headerProvider = object : RequiredHeadersInterceptor.HeadersProvider {
        override fun getAcceptLanguages(): String? = RestAPI.AppHeadersProvider.acceptLanguageProvider.invoke()

        override fun getAuthToken(): String = token

        override fun getFingerprint(): String? = RestAPI.AppHeadersProvider.fingerprintProvider.invoke()

        override fun getLocale(): String? = RestAPI.AppHeadersProvider.localeProvider.invoke()

        override fun getSpotifyToken(): String? = RestAPI.AppHeadersProvider.spotifyTokenProvider.invoke()

        override fun getUserAgent(): String = if (rnSuperProps) RNSuperProperties.userAgent else BuildConfig.USER_AGENT
    }
    val requiredHeadersInterceptor = RequiredHeadersInterceptor(headerProvider)
    val loggingInterceptor = RestAPI.Companion!!.buildLoggingInterceptor()
    val superPropsInterceptor = if (rnSuperProps) {
        RNSuperPropsInterceptor()
    } else {
        RestAPI.Companion!!.buildAnalyticsInterceptor()
    }
    val interceptors = mutableListOf(
        requiredHeadersInterceptor,
        loggingInterceptor,
        superPropsInterceptor
    )

    val persistentCookieJar = PersistentCookieJar(SetCookieCache(), SharedPrefsCookiePersistor(Utils.appContext))
    val restAPIBuilder = RestAPIBuilder(BuildConfig.HOST_API, persistentCookieJar)
    val restAPIInterface = restAPIBuilder.build(
        PluginRestAPIInterface::class.java,
        true,
        0,
        interceptors,
        "client_base",
        true,
        RestAPIBuilder.CONTENT_TYPE_JSON
    )
    return PluginRestAPI(restAPIInterface)
}

class PluginRestAPI(
    val api: PluginRestAPIInterface
) : PluginRestAPIInterface {
    @GET("guilds/{guildId}/onboarding")
    override fun getOnboarding(@Path("guildId") guildId: Long): Observable<OnboardingModel> =
        api.getOnboarding(guildId)
    @POST("guilds/{guildId}/onboarding-responses")
    override fun createOnboardingResponses(@Path("guildId") guildId: Long, @Body response: OnboardingResponseRequest): Observable<Void> = api.createOnboardingResponses(guildId, response)
    @GET("guilds/{guildId}/member-verification")
    override fun getGuildMemberVerification(@Path("guildId") guildId: Long, @Query("with_guild") withGuild: Boolean): Observable<ModelMemberVerificationForm> = api.getGuildMemberVerification(guildId, withGuild)
    @PUT("guilds/{guildId}/requests/@me")
    override fun createGuildJoinRequest(@Path("guildId") guildId: Long, @Body form: RestAPIParams.MemberVerificationForm): Observable<ModelMemberVerificationFormResponse> = api.createGuildJoinRequest(guildId, form)
}

interface PluginRestAPIInterface {
    @GET("guilds/{guildId}/onboarding")
    fun getOnboarding(@Path("guildId") guildId: Long): Observable<OnboardingModel>
    @POST("guilds/{guildId}/onboarding-responses")
    fun createOnboardingResponses(@Path("guildId") guildId: Long, @Body response: OnboardingResponseRequest): Observable<Void>
    @GET("guilds/{guildId}/member-verification")
    fun getGuildMemberVerification(@Path("guildId") guildId: Long, @Query("with_guild") withGuild: Boolean): Observable<ModelMemberVerificationForm>

    @PUT("guilds/{guildId}/requests/@me")
    fun createGuildJoinRequest(@Path("guildId") guildId: Long, @Body form: RestAPIParams.MemberVerificationForm): Observable<ModelMemberVerificationFormResponse>
}

class OnboardingResponseRequest(
    val onboardingResponses: List<Long>?,
    val onboardingPromptsSeen: Map<Long, Long>?,
    val onboardingResponsesSeen: Map<Long, Long>?,
)
class OnboardingModel(
    /**
     * ID of the guild this onboarding is part of.
     */
    @Query("guild_id") val guildId: Long,
    /**
     * The prompts shown during onboarding.
     */
    @Query("prompts") val prompts: Array<OnboardingPrompt>,
    /**
     * The channel IDs that members get opted into automatically.
     */
    @Query("default_channel_ids") val _defaultChannels: Array<Long>,
    @Query("enabled") val _enabled: Boolean,
    @Query("below_requirements") _belowRequirements: Boolean,
    @Query("mode") val _mode: OnboardingMode?,
    /**
     * Connection recommendations shown during onboarding
     */
    @Query("connections") val connections: Array<OnboardingConnection>?,
    @Query("responses") val responses: Array<Long>?,
    @Query("onboarding_prompts_seen") val _onboardingPromptsSeen: Map<Long, Int>?,
    @Query("onboarding_responses_seen") val _onboardingResponsesSeen: Map<Long, Int>?,
)

class OnboardingPrompt(
    @Query("id") val id: Long,
    @Query("type") val type: OnboardingPromptType,
    @Query("options") val options: Array<OnboardingPromptOption>,
    @Query("title") val title: String,
    @Query("single_select") val singleSelect: Boolean,
    @Query("required") val required: Boolean,
    @Query("inOnboarding") val inOnboarding: Boolean,
)

class OnboardingPromptOption(
    @Query("id") val id: Long,
    @Query("channel_ids") val channelIds: Array<Long>,
    @Query("role_ids") val roleIds: Array<Long>,
    @Query("emoji") val emoji: GuildEmoji,
    @Query("title") val title: String,
    @Query("description") val description: String?,
)

enum class OnboardingPromptType(type: Int) {
    MULTIPLE_CHOICE(0), DROPDOWN(1)
}

enum class OnboardingMode(mode: Int) {
    DEFAULT(0), ADVANCED(1)
}

class OnboardingConnection(
    @Query("connection_type") type: OnboardingConnectionType,
    @Query("application_id") applicationId: Long?,
    @Query("provider_id") providerId: String?,
    @Query("description") description: String,
)

enum class OnboardingConnectionType(type: Int) {
    APPLICATION(0), PROVIDER_CONNECTED_ACCOUNT(1)
}

// todo: not sure if this is correct
class RNSuperPropsInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.c()
        val headersBuilder = request.d.e()
        headersBuilder.a("X-Super-Properties", RNSuperProperties.superPropertiesBase64)
        headersBuilder.a("X-Discord-Timezone", TimeZone.getDefault().id)
        headersBuilder.a("Accept", "*/*")
        val newHeaders = headersBuilder.c()
        val newRequest = Request(request.b, request.c, newHeaders, request.e, f0.e0.c.A(request.f))
        return chain.a(newRequest)
    }
}

typealias Body = a
typealias Headers = k
typealias HeaderMap = j
typealias Field = c
typealias FieldMap = d
typealias Path = s
typealias Query = t
typealias QueryMap = u
typealias Part = p
typealias Url = y
typealias DELETE = b
typealias GET = f
typealias HEAD = g
typealias OPTIONS = m
typealias PATCH = n
typealias POST = o
typealias PUT = p
