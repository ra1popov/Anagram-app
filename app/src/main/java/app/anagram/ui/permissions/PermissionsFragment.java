package app.anagram.ui.permissions;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import app.anagram.R;
import app.anagram.databinding.FragmentPermissionsBinding;
import app.anagram.ui.BaseFragment;

public class PermissionsFragment extends BaseFragment<FragmentPermissionsBinding> implements PermissionsHandler {

    private final ActivityResultLauncher<String[]> requestPermissionsLauncher = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
        if (isGrantedPermissions()) {
            navigate(R.id.action_nav_home_to_nav_video);
        } else {
            toast(R.string.permissions_camera_microphone_required);
            openAppSettings();
            requireActivity().finishAffinity();
        }
    });

    private PermissionsViewModel permissionsViewModel;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        permissionsViewModel = new ViewModelProvider(requireActivity()).get(PermissionsViewModel.class);
    }

    @Override
    protected void initView() {
    }

    @Override
    protected void initControl() {

    }

    @Override
    public void onResume() {
        super.onResume();

        checkAndRequestPermissions();
    }

    private void checkAndRequestPermissions() {
        String[] permissions = {Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO};
        if (isGrantedPermissions()) {
            navigate(R.id.action_nav_home_to_nav_video);
        } else {
            requestPermissionsLauncher.launch(permissions);
        }
    }

    private boolean isGrantedPermissions() {
        boolean cameraGranted = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
        boolean audioGranted = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
        if (cameraGranted && audioGranted) {
            return true;
        }
        return false;
    }

    private void openAppSettings() {
        Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", requireContext().getPackageName(), null));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    /// //////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    protected FragmentPermissionsBinding getViewBinding(LayoutInflater inflater, ViewGroup container) {
        FragmentPermissionsBinding binding = FragmentPermissionsBinding.inflate(inflater, container, false);
        binding.setViewModel(permissionsViewModel);
        binding.setHandler(this);
        binding.setLifecycleOwner(getViewLifecycleOwner()); // call after setAdapter
        return binding;
    }

}
