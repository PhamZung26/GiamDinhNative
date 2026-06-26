package com.tc128.giamdinhnative.ui.screens.items;

import com.tc128.giamdinhnative.data.repository.ContainerRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
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
public final class ItemsViewModel_Factory implements Factory<ItemsViewModel> {
  private final Provider<ContainerRepository> containerRepositoryProvider;

  public ItemsViewModel_Factory(Provider<ContainerRepository> containerRepositoryProvider) {
    this.containerRepositoryProvider = containerRepositoryProvider;
  }

  @Override
  public ItemsViewModel get() {
    return newInstance(containerRepositoryProvider.get());
  }

  public static ItemsViewModel_Factory create(
      Provider<ContainerRepository> containerRepositoryProvider) {
    return new ItemsViewModel_Factory(containerRepositoryProvider);
  }

  public static ItemsViewModel newInstance(ContainerRepository containerRepository) {
    return new ItemsViewModel(containerRepository);
  }
}
