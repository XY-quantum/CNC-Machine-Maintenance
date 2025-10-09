package cnc.woodcut.machine.maintenance.ui.arassistant;

import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.Manifest;
import android.content.pm.PackageManager;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentManager;

//ARCore
import com.google.ar.core.Anchor;
import com.google.ar.core.ArCoreApk;
import com.google.ar.core.Camera;
import com.google.ar.core.Frame;
import com.google.ar.core.Plane;
import com.google.ar.core.Session;
import com.google.ar.core.exceptions.UnavailableException;
import cnc.woodcut.machine.maintenance.R;



public class ARAssistantFragment extends Fragment{ //implements SampleRender.Renderer {

    private static final String TAG = "ARAssistantFragment";

    private GLSurfaceView surfaceView;;
    private Session session;

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        surfaceView = view.findViewById(R.id.glsurfaceview);

        // Initialize ARCore session
        try {
            session = new Session(requireContext());
        } catch (UnavailableException e) {
            Log.e(TAG, "ARCore unavailable", e);
            return;
        }

        // Configure GLSurfaceView
        surfaceView.setEGLContextClientVersion(2);
        //surfaceView.setRenderer(new ARAssistantRenderer(requireContext(),session));
        surfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
    }

}