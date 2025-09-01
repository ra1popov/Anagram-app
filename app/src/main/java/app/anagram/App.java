package app.anagram;

import androidx.annotation.NonNull;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ProcessLifecycleOwner;
import androidx.multidex.MultiDexApplication;

import app.anagram.dependencies.ApplicationDependencies;
import app.anagram.dependencies.ApplicationDependencyProvider;

public class App extends MultiDexApplication implements DefaultLifecycleObserver {

    @Override
    public void onCreate() {
        super.onCreate();

        initializeAppDependencies();

        ProcessLifecycleOwner.get().getLifecycle().addObserver(this);
    }

    private void initializeAppDependencies() {
        ApplicationDependencies.init(this, new ApplicationDependencyProvider(this));
    }

    @Override
    public void onStart(@NonNull LifecycleOwner owner) {
    }

    @Override
    public void onStop(@NonNull LifecycleOwner owner) {
    }

}
