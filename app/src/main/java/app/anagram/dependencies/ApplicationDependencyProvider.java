package app.anagram.dependencies;

import android.app.Application;

import androidx.annotation.NonNull;

import app.anagram.Config;
import app.anagram.api.ApiClient;

public class ApplicationDependencyProvider implements ApplicationDependencies.Provider {

    private final Application context;

    public ApplicationDependencyProvider(@NonNull Application context) {
        this.context = context;
    }

    @NonNull
    @Override
    public ApiClient provideApiClient() {
        return new ApiClient(context, Config.SIGNAL_URI);
    }

}
