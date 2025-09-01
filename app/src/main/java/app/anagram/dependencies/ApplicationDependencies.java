package app.anagram.dependencies;

import android.app.Application;

import androidx.annotation.NonNull;

import java.lang.ref.WeakReference;

import app.anagram.api.ApiClient;

public class ApplicationDependencies {

    private static Application application;
    private static Provider provider;

    private static WeakReference<ApiClient> apiClientRef;

    public static synchronized void init(@NonNull Application application, @NonNull Provider provider) {
        if (ApplicationDependencies.application != null || ApplicationDependencies.provider != null) {
            throw new IllegalStateException("Already initialized!");
        }

        ApplicationDependencies.application = application;
        ApplicationDependencies.provider = provider;
    }

    @NonNull
    public static Application getApplication() {
        assertInitialization();
        return application;
    }

    @NonNull
    public static synchronized ApiClient getApiClient() {
        assertInitialization();

        ApiClient apiClient = apiClientRef != null ? apiClientRef.get() : null;
        if (apiClient == null) {
            apiClient = provider.provideApiClient();
            apiClientRef = new WeakReference<>(apiClient);
        }

        return apiClient;
    }

    private static void assertInitialization() {
        if (application == null || provider == null) {
            throw new UninitializedException();
        }
    }

    public interface Provider {
        @NonNull
        ApiClient provideApiClient();
    }

    private static class UninitializedException extends IllegalStateException {
        private UninitializedException() {
            super("You must call init() first!");
        }
    }


    public static void dispose() {
        if (apiClientRef != null) {
            apiClientRef.clear();
            apiClientRef = null;
        }
    }

}
