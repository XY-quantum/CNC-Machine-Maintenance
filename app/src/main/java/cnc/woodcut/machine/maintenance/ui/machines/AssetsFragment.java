package cnc.woodcut.machine.maintenance.ui.machines;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewFragment;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import org.opencv.android.OpenCVLoader;

import cnc.woodcut.machine.maintenance.R;
import cnc.woodcut.machine.maintenance.databinding.FragmentAssetBinding;
import cnc.woodcut.machine.maintenance.databinding.ItemAssetBinding;

import java.util.Arrays;
import java.util.List;
/**
 *  The list of the CNC machines
 */

/**
 * Fragment that demonstrates a responsive layout pattern where the format of the content
 * transforms depending on the size of the screen. Specifically this Fragment shows items in
 * the [RecyclerView] using LinearLayoutManager in a small screen
 * and shows items using GridLayoutManager in a large screen.
 */
public class AssetsFragment extends Fragment {
    // opencv
    static {
        if (OpenCVLoader.initDebug()) {
            Log.d("Check", "OpenCV configured successfully");
        } else {
            Log.d("Check", "OpenCV not configured successfully");
        }
    }
    private FragmentAssetBinding binding;
    private ValueCallback<Uri[]> filePathCallback;
    private static final int FILE_CHOOSER_REQUEST_CODE = 1001;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        AssetsViewModel assetsViewModel =
                new ViewModelProvider(this).get(AssetsViewModel.class);

        binding = FragmentAssetBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        /*---------For the "choose file"------------------*/
        View view = inflater.inflate(R.layout.fragment_asset, container, false);
        WebView webView = view.findViewById(R.id.webview);

        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setAllowFileAccess(true);
        webView.getSettings().setAllowContentAccess(true);

        webView.setWebChromeClient( new WebChromeClient(){
            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback,
                                             FileChooserParams fileChooserParams) {
                AssetsFragment.this.filePathCallback = filePathCallback;
                Intent intent = fileChooserParams.createIntent();
                try {
                    startActivityForResult(intent, FILE_CHOOSER_REQUEST_CODE);
                } catch (ActivityNotFoundException e){
                    AssetsFragment.this.filePathCallback = null;
                    return false;
                }
                return true;
            }

        });

        webView.loadUrl("file:///android_asset/cnc_maintenance_camera_app.html");

        return view;

    }
    /*---------For the "choose file"------------------*/
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILE_CHOOSER_REQUEST_CODE) {
            if (filePathCallback != null) {
                Uri[] results = null;
                if (resultCode == Activity.RESULT_OK && data != null) {
                    results = new Uri[]{(data.getData())};
                }
                filePathCallback.onReceiveValue(results);
                filePathCallback = null;
            }
        }
    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }



}