/*
 * This file is part of FairSpeed.
 *
 * FairSpeed is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FairSpeed is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with FairSpeed.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.fairspeed.test.di

import com.fairspeed.test.data.remote.api.SpeedTestApi
import com.fairspeed.test.data.remote.api.LibreSpeedApi
import com.fairspeed.test.data.remote.service.SpeedTestService
import com.fairspeed.test.data.repository.SpeedTestRepository
import com.fairspeed.test.domain.repository.ISpeedTestRepository
import com.fairspeed.test.domain.usecase.ClearHistoryUseCase
import com.fairspeed.test.domain.usecase.GetServersUseCase
import com.fairspeed.test.domain.usecase.GetTestHistoryUseCase
import com.fairspeed.test.domain.usecase.SaveTestHistoryUseCase
import com.fairspeed.test.domain.usecase.SelectServerUseCase
import com.fairspeed.test.domain.usecase.StartSpeedTestUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.Dns
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.Inet4Address
import java.net.InetAddress
import javax.inject.Singleton
import com.fairspeed.test.config.SpeedTestConfig

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.HEADERS
        }

        val ipv4Dns = object : Dns {
            override fun lookup(hostname: String): List<InetAddress> {
                val addresses = Dns.SYSTEM.lookup(hostname)
                val ipv4 = addresses.filterIsInstance<Inet4Address>()
                return ipv4.ifEmpty { addresses }
            }
        }

        return OkHttpClient.Builder()
            .dns(ipv4Dns)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .connectionPool(okhttp3.ConnectionPool(8, 5, java.util.concurrent.TimeUnit.MINUTES))
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(SpeedTestConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideLibreSpeedApi(retrofit: Retrofit): LibreSpeedApi {
        return retrofit.create(LibreSpeedApi::class.java)
    }

    @Provides
    @Singleton
    fun provideSharedPreferences(@dagger.hilt.android.qualifiers.ApplicationContext context: android.content.Context): android.content.SharedPreferences {
        return context.getSharedPreferences("SpeedTestPrefs", android.content.Context.MODE_PRIVATE)
    }

    @Provides
    @Singleton
    fun provideSpeedTestRepository(
        api: LibreSpeedApi,
        speedTestService: SpeedTestService,
        sharedPreferences: android.content.SharedPreferences
    ): ISpeedTestRepository {
        return SpeedTestRepository(api, speedTestService, sharedPreferences)
    }
}

@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {

    @Provides
    fun provideStartSpeedTestUseCase(repository: ISpeedTestRepository): StartSpeedTestUseCase {
        return StartSpeedTestUseCase(repository)
    }

    @Provides
    fun provideGetTestHistoryUseCase(repository: ISpeedTestRepository): GetTestHistoryUseCase {
        return GetTestHistoryUseCase(repository)
    }

    @Provides
    fun provideGetServersUseCase(repository: ISpeedTestRepository): GetServersUseCase {
        return GetServersUseCase(repository)
    }

    @Provides
    fun provideSelectServerUseCase(repository: ISpeedTestRepository): SelectServerUseCase {
        return SelectServerUseCase(repository)
    }

    @Provides
    fun provideGetUserIpUseCase(repository: ISpeedTestRepository): com.fairspeed.test.domain.usecase.GetUserIpUseCase {
        return com.fairspeed.test.domain.usecase.GetUserIpUseCase(repository)
    }

    @Provides
    fun provideClearHistoryUseCase(repository: ISpeedTestRepository): ClearHistoryUseCase {
        return ClearHistoryUseCase(repository)
    }

    @Provides
    fun provideSaveTestHistoryUseCase(repository: ISpeedTestRepository): SaveTestHistoryUseCase {
        return SaveTestHistoryUseCase(repository)
    }
}
