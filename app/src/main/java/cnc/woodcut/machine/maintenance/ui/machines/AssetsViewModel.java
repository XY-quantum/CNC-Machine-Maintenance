package cnc.woodcut.machine.maintenance.ui.machines;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.List;

public class AssetsViewModel extends ViewModel {

    private final MutableLiveData<List<String>> mTexts;

    public AssetsViewModel() {
        mTexts = new MutableLiveData<>();
        List<String> texts = new ArrayList<>();
        for (int i = 1; i <= 2; i++) {
            texts.add("This is machine  " + i);
        }
        mTexts.setValue(texts);
    }

    public LiveData<List<String>> getTexts() {
        return mTexts;
    }
}