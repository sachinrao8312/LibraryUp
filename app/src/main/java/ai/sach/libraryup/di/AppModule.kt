package ai.sach.libraryup.di

import ai.sach.libraryup.ui.screens.authentication.BiometricAuthManager
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt is only used to inject [BiometricAuthManager] into MainActivity. Everything else
 * lives on [ai.sach.libraryup.LibraryUpApplication], which screens reach directly.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @RequiresApi(Build.VERSION_CODES.N)
    @Provides
    @Singleton
    fun provideBiometricAuthManager(@ApplicationContext context: Context): BiometricAuthManager =
        BiometricAuthManager(context)
}
