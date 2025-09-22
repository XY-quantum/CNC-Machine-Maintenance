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

import android.Manifest;
import android.content.pm.PackageManager;
import android.opengl.GLES20;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import cnc.woodcut.machine.maintenance.R;


// ARCore library
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.core.Pose;
import com.google.ar.core.TrackingState;
import com.google.ar.core.exceptions.*;
import com.google.ar.core.Anchor;
import com.google.ar.core.Camera;
import com.google.ar.core.Frame;
import com.google.ar.core.InstantPlacementPoint;
import com.google.ar.core.Session;
import com.google.ar.core.Trackable;
//
import android.opengl.GLSurfaceView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class ARAssistantFragment extends Fragment  {
    /*-* Vars Definitions  ---------------------------------------------------------------------*/
    private boolean installRequested;
    private GLSurfaceView glSurfaceView;
    private Session arSession;
    private ARRenderer renderer;

    private static final String SEARCHING_PLANE_MESSAGE = "Searching for surfaces...";
    private static final String WAITING_FOR_TAP_MESSAGE = "Tap on a surface to place an object.";

    private static final int CAMERA_PERMISSION_CODE = 1001;
    private static final String CAMERA_PERMISSION = Manifest.permission.CAMERA;


    // See the definition of updateSphericalHarmonicsCoefficients for an explanation of these
    // constants.
    private static final float[] sphericalHarmonicFactors = {
            0.282095f,
            -0.325735f,
            0.325735f,
            -0.325735f,
            0.273137f,
            -0.273137f,
            0.078848f,
            -0.273137f,
            0.136569f,
    };

    private static final float Z_NEAR = 0.1f;
    private static final float Z_FAR = 100f;

    private static final int CUBEMAP_RESOLUTION = 16;
    private static final int CUBEMAP_NUMBER_OF_IMPORTANCE_SAMPLES = 32;

    // Rendering. The Renderers are created here, and initialized when the GL surface is created.
    private GLSurfaceView surfaceView;

    private Session session;


    // Temporary matrix allocated here to reduce number of allocations for each frame.
    private final float[] modelMatrix = new float[16];
    private final float[] viewMatrix = new float[16];
    private final float[] projectionMatrix = new float[16];
    private final float[] modelViewMatrix = new float[16]; // view x model
    private final float[] modelViewProjectionMatrix = new float[16]; // projection x view x model
    private final float[] sphericalHarmonicsCoefficients = new float[9 * 3];
    private final float[] viewInverseMatrix = new float[16];
    private final float[] worldLightDirection = {0.0f, 0.0f, 0.0f, 0.0f};
    private final float[] viewLightDirection = new float[4]; // view x world light direction

    /*-* End Vars Definitions  -----------------------------------------------------------------*/
    @Override
    public  View onCreateView (LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
         View view = inflater.inflate(R.layout.fragment_arassistant,container,false);
         glSurfaceView = view.findViewById(R.id.glsurfaceview);

         glSurfaceView.setEGLContextClientVersion(2);
         glSurfaceView.setPreserveEGLContextOnPause(true);

         Log.e("AR Assistant","AR Assistant being CALLED");
         //SetARView();

         return view;
    }

    private void SetARView(){
        if (arSession == null) {
            try {
                arSession.resume();
            } catch (CameraNotAvailableException e) {
                 Log.e("ARFragment", "Camera not available",e);
                 arSession = null;
            }
        }
        glSurfaceView.onResume();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (arSession == null) {
            try {
                arSession = new Session(requireContext());
            } catch (UnavailableException e) {
                Toast.makeText(getContext(), "ARCore not available", Toast.LENGTH_LONG).show();
                return;
            }
            renderer = new ARRenderer(arSession);
            glSurfaceView.setRenderer(renderer);
            glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
        }
        try {
            // Check camera permission
            if (!hasCameraPermission()) {
                requestCameraPermission();
                return;
            }
            arSession.resume();
        } catch (CameraNotAvailableException e) {
            Toast.makeText(getContext(), "Camera not available", Toast.LENGTH_LONG).show();
        }
        glSurfaceView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        glSurfaceView.onPause();
        if (arSession != null) {
            arSession.pause();
        }
    }

    private boolean hasCameraPermission() {
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestCameraPermission() {
        requestPermissions(new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (hasCameraPermission()) {
            onResume(); // Try again now that we have permission
        } else {
            Toast.makeText(getContext(), "Camera permission is needed for AR", Toast.LENGTH_LONG).show();
        }
    }
    public class ARRenderer implements GLSurfaceView.Renderer {

        private final Session session;
        private final BackgroundRenderer backgroundRenderer = new BackgroundRenderer();
        private final PlaneRenderer planeRenderer = new PlaneRenderer();
        //private final ObjectRenderer objectRenderer = new ObjectRenderer();

        private final List<Anchor> anchors = new ArrayList<>();
        private MotionEvent queuedTap;

        public ARRenderer(Session session) {
            this.session = session;
        }

        public void onTap(MotionEvent tap) {
            queuedTap = tap;
        }

        @Override
        public void onSurfaceCreated(GL10 gl, EGLConfig config) {
            backgroundRenderer.createOnGlThread();
            planeRenderer.init();
            //objectRenderer.init();
        }

        @Override
        public void onSurfaceChanged(GL10 gl, int width, int height) {
            GLES20.glViewport(0, 0, width, height);
        }

        @Override
        public void onDrawFrame(GL10 gl) {
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

            try {
                session.setCameraTextureName(backgroundRenderer.getTextureId());
                Frame frame = session.update();
                Camera camera = frame.getCamera();

                backgroundRenderer.draw(frame);

                // Handle tap
                if (queuedTap != null && camera.getTrackingState() == TrackingState.TRACKING) {
                    List<HitResult> hits = frame.hitTest(queuedTap);
                    for (HitResult hit : hits) {
                        Trackable trackable = hit.getTrackable();
                        if (trackable instanceof Plane && ((Plane) trackable).isPoseInPolygon(hit.getHitPose())) {
                            anchors.add(hit.createAnchor());
                            break;
                        }
                    }
                    queuedTap = null;
                }

                // Draw planes
                planeRenderer.drawPlanes(session.getAllTrackables(Plane.class), camera.getDisplayOrientedPose());

                // Draw placed objects
                /*for (Anchor anchor : anchors) {
                    if (anchor.getTrackingState() == TrackingState.TRACKING) {
                        objectRenderer.draw(anchor.getPose());
                    }
                }*/

            } catch (Exception e) {
                Log.e("ARRenderer", "Exception in onDrawFrame", e);
            }
        }
    }

    public class BackgroundRenderer {
        private int textureId;

        public void createOnGlThread() {
            int[] textures = new int[1];
            GLES20.glGenTextures(1, textures, 0);
            textureId = textures[0];
            // Configure texture parameters here
        }

        public int getTextureId() {
            return textureId;
        }

        public void draw(Frame frame) {
            // Use frame.getCamera() and frame.getTimestamp() to draw camera feed
            // This requires shader setup and texture binding
        }
    }

    public class PlaneRenderer {
        public void init() {
            // Load shaders and setup buffers
        }

        public void drawPlanes(Collection<Plane> planes, Pose cameraPose) {
            for (Plane plane : planes) {
                if (plane.getTrackingState() == TrackingState.TRACKING) {
                    // Draw plane geometry here
                }
            }
        }
    }


}/*__end Fragment */



/**
 * Associates an Anchor with the trackable it was attached to. This is used to be able to check
 * whether or not an Anchor originally was attached to an {@link InstantPlacementPoint}.
 */
class WrappedAnchor {
    private Anchor anchor;
    private Trackable trackable;

    public WrappedAnchor(Anchor anchor, Trackable trackable) {
        this.anchor = anchor;
        this.trackable = trackable;
    }

    public Anchor getAnchor() {
        return anchor;
    }

    public Trackable getTrackable() {
        return trackable;
    }
}


