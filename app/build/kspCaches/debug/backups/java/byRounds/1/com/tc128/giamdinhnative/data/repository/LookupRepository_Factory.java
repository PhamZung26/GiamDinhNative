package com.tc128.giamdinhnative.data.repository;

import com.tc128.giamdinhnative.data.local.LookupDao;
import com.tc128.giamdinhnative.data.remote.ApiService;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class LookupRepository_Factory implements Factory<LookupRepository> {
  private final Provider<ApiService> apiServiceProvider;

  private final Provider<LookupDao> lookupDaoProvider;

  public LookupRepository_Factory(Provider<ApiService> apiServiceProvider,
      Provider<LookupDao> lookupDaoProvider) {
    this.apiServiceProvider = apiServiceProvider;
    this.lookupDaoProvider = lookupDaoProvider;
  }

  @Override
  public LookupRepository get() {
    return newInstance(apiServiceProvider.get(), lookupDaoProvider.get());
  }

  public static LookupRepository_Factory create(Provider<ApiService> apiServiceProvider,
      Provider<LookupDao> lookupDaoProvider) {
    return new LookupRepository_Factory(apiServiceProvider, lookupDaoProvider);
  }

  public static LookupRepository newInstance(ApiService apiService, LookupDao lookupDao) {
    return new LookupRepository(apiService, lookupDao);
  }
}
