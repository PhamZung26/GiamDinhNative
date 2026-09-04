package com.tc128.giamdinhnative.ui.screens.images;

import android.content.Context;
import com.tc128.giamdinhnative.data.repository.PhotoRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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

  private final Provider<Context> contextProvider;

  public ImagesViewModel_Factory(Provider<PhotoRepository> photoRepositoryProvider,
      Provider<Context> contextProvider) {
    this.photoRepositoryProvider = photoRepositoryProvider;
    this.contextProvider = contextProvider;
  }

  @Override
  public ImagesViewModel get() {
    return newInstance(photoRepositoryProvider.get(), contextProvider.get());
  }

  public static ImagesViewModel_Factory create(Provider<PhotoRepository> photoRepositoryProvider,
      Provider<Context> contextProvider) {
    return new ImagesViewModel_Factory(photoRepositoryProvider, contextProvider);
  }

  public static ImagesViewModel newInstance(PhotoRepository photoRepository, Context context) {
    return new ImagesViewModel(photoRepository, context);
  }
}
