package cnc.woodcut.machine.maintenance.ui.machines;

import android.content.Context;
import android.graphics.pdf.PdfDocument;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.print.PageRange;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintDocumentInfo;
import android.print.pdf.PrintedPdfDocument;
import android.webkit.WebView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class CustomPrintDocumentAdapter extends PrintDocumentAdapter {

    private final Context context;
    private final WebView webView;
    private final File file;

    public CustomPrintDocumentAdapter(Context context, WebView webView, File file) {
        this.context = context;
        this.webView = webView;
        this.file = file;
    }

    @Override
    public void onLayout(PrintAttributes oldAttributes, PrintAttributes newAttributes,
                         CancellationSignal cancellationSignal,
                         LayoutResultCallback callback, Bundle extras) {

        PrintDocumentInfo info = new PrintDocumentInfo
                .Builder("output.pdf")
                .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                .build();
        callback.onLayoutFinished(info, true);
    }

    @Override
    public void onWrite(PageRange[] pages, ParcelFileDescriptor destination,
                        CancellationSignal cancellationSignal,
                        WriteResultCallback callback) {

        PrintedPdfDocument document = new PrintedPdfDocument(context, newAttributes());

        PdfDocument.Page page = document.startPage(0);
        webView.draw(page.getCanvas());
        document.finishPage(page);

        try (FileOutputStream out = new FileOutputStream(file)) {
            document.writeTo(out);
            callback.onWriteFinished(new PageRange[]{PageRange.ALL_PAGES});
        } catch (IOException e) {
            callback.onWriteFailed(e.toString());
        } finally {
            document.close();
        }
    }

    private PrintAttributes newAttributes() {
        return new PrintAttributes.Builder()
                .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                .setResolution(new PrintAttributes.Resolution("pdf", "pdf", 600, 600))
                .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                .build();
    }
}

