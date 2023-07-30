package com.johnathanmitri.alarmsystem.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.johnathanmitri.alarmsystem.R;
import com.johnathanmitri.alarmsystem.databinding.FragmentHomeBinding;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        /*GalleryViewModel galleryViewModel =
                new ViewModelProvider(this).get(GalleryViewModel.class);*/

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        //final TextView textView = binding.textGallery;
        //galleryViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);

        binding.armButton.setOnCheckedChangeListener(
                (buttonView, isChecked) -> {
                    if (isChecked)
                    {
                        binding.textView4.setText("Armed");
                        binding.textView4.setTextColor(getResources().getColor(R.color.armedColor, getActivity().getTheme()));
                    }
                    else
                    {
                        binding.textView4.setText("Disarmed");
                        binding.textView4.setTextColor(getResources().getColor(R.color.disarmedColor, getActivity().getTheme()));
                    }
                }
        );

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}