package app.anagram.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.databinding.ViewDataBinding;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import app.anagram.util.Toolbox;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

public abstract class BaseFragment<B extends ViewDataBinding> extends Fragment {

    protected CompositeDisposable compositeDisposable = new CompositeDisposable();
    protected B binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = getViewBinding(inflater, container);
        Toolbox.hideSoftKeyboard(requireActivity());
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initView();
        initControl();
    }

    protected abstract void initView();

    protected abstract void initControl();

    protected abstract B getViewBinding(LayoutInflater inflater, ViewGroup container);

    private BaseActivity<?> getBaseActivity() {
        return (BaseActivity<?>) getActivity();
    }

    @Override
    public void onDestroy() {
        compositeDisposable.dispose();
        Toolbox.hideSoftKeyboard(getActivity());
        super.onDestroy();
    }

    public void toast(String text) {
        BaseActivity<?> activity = getBaseActivity();
        if (activity != null) {
            activity.toast(text);
        }
    }

    public void toast(@StringRes int textResId) {
        BaseActivity<?> activity = getBaseActivity();
        if (activity != null) {
            activity.toast(textResId);
        }
    }

    public void navigate(int id) {
        navigate(id, null);
    }

    public void navigate(int id, Bundle bundle) {
        try {
            NavHostFragment.findNavController(BaseFragment.this).navigate(
                    id,
                    bundle,
                    null,
                    null);
        } catch (Exception ignored) {
            // try-catch for fix Fatal Exception: java.lang.IllegalArgumentException
        }
    }

}
