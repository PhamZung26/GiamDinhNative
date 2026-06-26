package com.tc128.giamdinhnative.di;

import com.tc128.giamdinhnative.session.SessionManager;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;
import okhttp3.OkHttpClient;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast",
    "deprecation"
})
public final class NetworkModule_ProvideOkHttpClientFactory implements Factory<OkHttpClient> {
  private final Provider<SessionManager> sessionManagerProvider;

  public NetworkModule_ProvideOkHttpClientFactory(Provider<SessionManager> sessionManagerProvider) {
    this.sessionManagerProvider = sessionManagerProvider;
  }

  @Override
  public OkHttpClient get() {
    return provideOkHttpClient(sessionManagerProvider.get());
  }

  public static NetworkModule_ProvideOkHttpClientFactory create(
      Provider<SessionManager> sessionManagerProvider) {
    return new NetworkModule_ProvideOkHttpClientFactory(sessionManagerProvider);
  }

  public static OkHttpClient provideOkHttpClient(SessionManager sessionManager) {
    return Preconditions.checkNotNullFromProvides(NetworkModule.INSTANCE.provideOkHttpClient(sessionManager));
  }
}
