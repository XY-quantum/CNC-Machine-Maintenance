/************************************************************ AR Assistant
 * AR Assistant are based on the Google AR Core example
 *
 ----------------- original AR Core example
 * Copyright 2020 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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