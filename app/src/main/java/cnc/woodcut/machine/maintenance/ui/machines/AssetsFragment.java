package cnc.woodcut.machine.maintenance.ui.machines;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebSettings;
import android.webkit.WebView;

import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import org.opencv.android.OpenCVLoader;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

import cnc.woodcut.machine.maintenance.R;


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
    private File generatedPdfFile;
    private WebView webView;
    private ValueCallback<Uri[]> filePathCallback;
    private static final int FILE_CHOOSER_REQUEST_CODE = 1001;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        //AssetsViewModel assetsViewModel =
          //      new ViewModelProvider(this).get(AssetsViewModel.class);

        /*---------For the "choose file"------------------*/
        View view = inflater.inflate(R.layout.fragment_asset, container, false);
        webView = view.findViewById(R.id.webview);
        setupWebView();

        webView.loadUrl("file:///android_asset/cnc_maintenance_camera_app.html");
        return view;
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView(){
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowContentAccess(true);

        webView.addJavascriptInterface(new WebAppInterface(getContext()), "AndroidInterface");
    }

    // JavaScript Interface for the .html
    public class WebAppInterface {
        public final Context mContext;

        public WebAppInterface(Context pcontext) {
            this.mContext = pcontext;
        }

        @JavascriptInterface
        public void savePDF() {
            Log.d("WebView", "savePDF called from HTML");
            // Already being successfully called

            Date currentDate = new Date();
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss",Locale.getDefault());
            String timestamp = dateFormat.format(currentDate);
            String fileName  = "MachineX_Report_" + timestamp+".pdf";
            File file = new File(mContext.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName);
            // Print PDF
            PrintManager printManager = (PrintManager) mContext.getSystemService(Objects.requireNonNull(Context.PRINT_SERVICE));
            if (printManager != null) {
                String jobName = mContext.getString(R.string.app_name) + "MachineXMaintenanceReport_" + System.currentTimeMillis();

                // Create PDF file that captures the current DOM state
                // including all user input (text, timestamps, and the base64 image data).
                PrintDocumentAdapter printAdapter = webView.createPrintDocumentAdapter(jobName);

                //Start the print job, which opens the system's "Save to PDF" dialog.
                printManager.print(jobName, printAdapter, new PrintAttributes.Builder().build());
                Log.d("FeedbackfromAndroidInterface","PDF saved path is "+file.getAbsolutePath());
                Toast.makeText(mContext,"PDF saved: "+file.getAbsolutePath(), Toast.LENGTH_LONG).show();
                // IMPORTANT: Re-load the original HTML after printing to reset the form and interactive elements
                webView.loadUrl("file:///android_asset/cnc_maintenance_camera_app.html");
            } else {
                Toast.makeText(mContext,"Printing Service UnAvailiable", Toast.LENGTH_LONG).show();
            }
        }
    }
}