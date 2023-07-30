package com.johnathanmitri.alarmsystem.ui.zones;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class ZonesViewModel extends ViewModel {

    private final MutableLiveData<String> mText;

    public ZonesViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("This is home fragment");
    }

    public LiveData<String> getText() {
        return mText;
    }
}