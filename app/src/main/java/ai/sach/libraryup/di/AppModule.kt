package ai.sach.rentup.di

import ai.sach.rentup.BuildConfig
import ai.sach.rentup.data.firestore.repository.FirestoreRepository
import ai.sach.rentup.data.local.RentUpDatabase

import ai.sach.rentup.data.repository.PropertyRepository
import ai.sach.rentup.data.repository.TenantRepository
import ai.sach.rentup.ui.screens.authentication.AuthRepository
import ai.sach.rentup.ui.screens.authentication.BiometricAuthManager
import ai.sach.rentup.ui.screens.homescreen.TenantFormSubmissionHandler
import ai.sach.rentup.ui.screens.settingsScreen.notifications.NotificationHelper
import ai.sach.rentup.ui.screens.settingsScreen.notifications.NotificationPreferencesManager
import ai.sach.rentup.ui.screens.settingsScreen.notifications.ReminderScheduler
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import android.util.Log
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * Hilt module for providing application-wide dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

    @Provides
    @Singleton
    fun provideRentUpDatabase(@ApplicationContext context: Context): RentUpDatabase {
        return RentUpDatabase.getDatabase(context)
    }

    @Provides
    @Singleton
    fun provideFirestoreRepository(
        firestore: FirebaseFirestore,
        auth: FirebaseAuth
    ): FirestoreRepository {
        return FirestoreRepository(firestore, auth)
    }

    @Provides
    @Singleton
    fun providePropertyRepository(database: RentUpDatabase): PropertyRepository {
        return PropertyRepository(database.propertyDao())
    }

    @Provides
    @Singleton
    fun provideTenantRepository(
        database: RentUpDatabase,
        propertyRepository: PropertyRepository,
        firestoreRepository: FirestoreRepository,
        notificationHelper: NotificationHelper,
        notificationPreferences: NotificationPreferencesManager,
        reminderScheduler: ReminderScheduler,
        @ApplicationContext context: Context
    ): TenantRepository {
        return TenantRepository(
            database,
            database.tenantDao(),
            database.tenantFamilyInfoDao(),
            database.tenantDocumentDao(),
            database.paymentHistoryDao(),
            database.tenantPropertyDao(),
            propertyRepository,  // 6th parameter: PropertyRepository
            firestoreRepository, // 7th parameter: FirestoreRepository
            notificationHelper,
            notificationPreferences,
            reminderScheduler,
            context
        )
    }

    @Provides
    @Singleton
    fun provideAuthRepository(@ApplicationContext context: Context): AuthRepository {
        return AuthRepository(context)
    }

    @RequiresApi(Build.VERSION_CODES.N)
    @Provides
    @Singleton
    fun provideBiometricAuthManager(@ApplicationContext context: Context): BiometricAuthManager {
        return BiometricAuthManager(context)
    }

    @Provides
    @Singleton
    fun provideTenantFormSubmissionHandler(
        @ApplicationContext context: Context,
        tenantRepository: TenantRepository
    ): TenantFormSubmissionHandler {
        return TenantFormSubmissionHandler(context, tenantRepository)
    }

    @Provides
    @Singleton
    fun provideReminderScheduler(@ApplicationContext context: Context): ReminderScheduler {
        return ReminderScheduler(context)
    }

    @Provides
    @Singleton
    fun provideNotificationHelper(@ApplicationContext context: Context): NotificationHelper {
        return NotificationHelper(context)
    }

    @Provides
    @Singleton
    fun provideNotificationPreferencesManager(@ApplicationContext context: Context): NotificationPreferencesManager {
        return NotificationPreferencesManager(context)
    }

    @Provides
    @Singleton
    fun provideGson(): Gson {
        return GsonBuilder()
            .setLenient()
            .create()
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(createSecureLoggingInterceptor())
            .addInterceptor(createSecurityHeadersInterceptor())
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Creates a secure logging interceptor that filters sensitive data
     */
    private fun createSecureLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor { message ->
            // Filter out sensitive data from logs
            val sanitized = message
                .replace(Regex("(password|token|key|authorization)=[^&\\s]*", RegexOption.IGNORE_CASE), "$1=***")
                .replace(Regex("\"(password|token|key|authorization)\"\\s*:\\s*\"[^\"]*\"", RegexOption.IGNORE_CASE), "\"$1\":\"***\"")
            Log.d("HTTP", sanitized)
        }.apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.HEADERS  // Only log headers in debug, not body content
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
    }

    /**
     * Creates an interceptor that adds security headers
     */
    private fun createSecurityHeadersInterceptor(): okhttp3.Interceptor {
        return okhttp3.Interceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("X-Requested-With", "XMLHttpRequest")
                .addHeader("Cache-Control", "no-cache")
                .addHeader("X-Content-Type-Options", "nosniff")
                .addHeader("X-Frame-Options", "DENY")
                .build()
            chain.proceed(request)
        }
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, gson: Gson): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.BACKEND_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

}
