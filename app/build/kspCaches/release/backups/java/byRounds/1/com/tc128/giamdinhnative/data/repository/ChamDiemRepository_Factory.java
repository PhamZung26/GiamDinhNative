package com.tc128.giamdinhnative.data.repository;

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
public final class ChamDiemRepository_Factory implements Factory<ChamDiemRepository> {
  private final Provider<LookupRepository> lookupRepositoryProvider;

  public ChamDiemRepository_Factory(Provider<LookupRepository> lookupRepositoryProvider) {
    this.lookupRepositoryProvider = lookupRepositoryProvider;
  }

  @Override
  public ChamDiemRepository get() {
    return newInstance(lookupRepositoryProvider.get());
  }

  public static ChamDiemRepository_Factory create(
      Provider<LookupRepository> lookupRepositoryProvider) {
    return new ChamDiemRepository_Factory(lookupRepositoryProvider);
  }

  public static ChamDiemRepository newInstance(LookupRepository lookupRepository) {
    return new ChamDiemRepository(lookupRepository);
  }
}
