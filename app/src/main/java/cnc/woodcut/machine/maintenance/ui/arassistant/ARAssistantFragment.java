package cnc.woodcut.machine.maintenance.ui.arassistant;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import cnc.woodcut.machine.maintenance.databinding.FragmentArassistantBinding;

public class ARAssistantFragment extends Fragment {

    private FragmentArassistantBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        ARAssistantViewModel arassistantViewModel =
                new ViewModelProvider(this).get(ARAssistantViewModel.class);

        binding = FragmentArassistantBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        final TextView textView = binding.textSlideshow;
        arassistantViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);
        return root;

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        //binding = null;
    }
}