package cnc.woodcut.machine.maintenance.ui.machines;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.print.PageRange;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintDocumentInfo;
import android.print.PrintManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import org.opencv.android.OpenCVLoader;

import java.io.File;
import java.io.IOException;

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
        AssetsViewModel assetsViewModel =
                new ViewModelProvider(this).get(AssetsViewModel.class);



        /*---------For the "choose file"------------------*/
        View view = inflater.inflate(R.layout.fragment_asset, container, false);
        // Save file button
        Button btnShare = view.findViewById(R.id.btnSharePdf);
        btnShare.setOnClickListener(v -> {
            if (generatedPdfFile != null && generatedPdfFile.exists()) {
                sharePdfFile(generatedPdfFile);
            } else {
                Toast.makeText(requireContext(), "PDF not generated yet", Toast.LENGTH_SHORT).show();
            }
        });

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
                createPdfFromWebView(webView);
                return true;
            }
        });

        webView.loadUrl("file:///android_asset/cnc_maintenance_camera_app.html");

        return view;
    }

    private void createPdfFromWebView(WebView webView) {
        String fileName = "output_" + System.currentTimeMillis() + ".pdf";
        File file = new File(requireContext().getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName);
        generatedPdfFile = file;

        webView.post(() -> {
            PrintManager printManager = (PrintManager) requireContext().getSystemService(Context.PRINT_SERVICE);
            printManager.print("Document", new CustomPrintDocumentAdapter(requireContext(), webView, file), null);
            Toast.makeText(requireContext(), "PDF saved: " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
        });
    }

    private void sharePdfFile(File file) {
        Uri uri = FileProvider.getUriForFile(
                requireContext(),
                requireContext().getPackageName() + ".fileprovider",
                file
        );

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("application/pdf");
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        startActivity(Intent.createChooser(shareIntent, "Share PDF using"));
    }

}