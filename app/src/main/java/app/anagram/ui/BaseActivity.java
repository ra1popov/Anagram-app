package app.anagram.ui;

import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.Navigation;
import androidx.viewbinding.ViewBinding;

import app.anagram.R;

public abstract class BaseActivity<B extends ViewBinding> extends AppCompatActivity {

    protected B binding;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        overridePendingTransition(0, 0);

        binding = getViewBinding();
        setContentView(binding.getRoot());

        initView();
        initControl();
    }

    protected abstract void initView();

    protected abstract B getViewBinding();

    protected abstract void initControl();

    public void toast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }

    public void toast(@StringRes int textResId) {
        Toast.makeText(this, getString(textResId), Toast.LENGTH_SHORT).show();
    }

    public void navigate(int id) {
        navigate(id, null);
    }

    public void navigate(int id, Bundle bundle) {
        try {
            Navigation.findNavController(this, R.id.nav_host_fragment).navigate(
                    id,
                    bundle,
                    null,
                    null);
        } catch (Exception ignored) {
            // try-catch for fix Fatal Exception: java.lang.IllegalArgumentException
            // Navigation action/destination app.gefon:id/action_nav_themes_to_nav_settings cannot be found from the current destination a(app.gefon:id/nav_settings)
        }
    }

}
