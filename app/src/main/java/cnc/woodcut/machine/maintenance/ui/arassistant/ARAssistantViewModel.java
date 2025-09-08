package cnc.woodcut.machine.maintenance.ui.arassistant;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class ARAssistantViewModel extends ViewModel {

    private final MutableLiveData<String> mText;

    public ARAssistantViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("This is slideshow fragment");
    }

    public LiveData<String> getText() {
        return mText;
    }
}