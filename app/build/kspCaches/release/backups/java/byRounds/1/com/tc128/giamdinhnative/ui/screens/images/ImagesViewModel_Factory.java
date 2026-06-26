package com.tc128.giamdinhnative.ui.screens.images;

import com.tc128.giamdinhnative.data.repository.PhotoRepository;
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
public final class ImagesViewModel_Factory implements Factory<ImagesViewModel> {
  private final Provider<PhotoRepository> photoRepositoryProvider;

  public ImagesViewModel_Factory(Provider<PhotoRepository> photoRepositoryProvider) {
    this.photoRepositoryProvider = photoRepositoryProvider;
  }

  @Override
  public ImagesViewModel get() {
    return newInstance(photoRepositoryProvider.get());
  }

  public static ImagesViewModel_Factory create(Provider<PhotoRepository> photoRepositoryProvider) {
    return new ImagesViewModel_Factory(photoRepositoryProvider);
  }

  public static ImagesViewModel newInstance(PhotoRepository photoRepository) {
    return new ImagesViewModel(photoRepository);
  }
}
