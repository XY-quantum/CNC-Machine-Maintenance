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
import android.content.res.AssetManager;
import android.opengl.GLES30;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import cnc.woodcut.machine.maintenance.R;
import cnc.woodcut.machine.maintenance.ui.arassistant.common.helpers.FullScreenHelper;
import cnc.woodcut.machine.maintenance.ui.arassistant.common.samplerender.Framebuffer;
import cnc.woodcut.machine.maintenance.ui.arassistant.common.samplerender.GLError;
import cnc.woodcut.machine.maintenance.ui.arassistant.common.samplerender.Mesh;
import cnc.woodcut.machine.maintenance.ui.arassistant.common.samplerender.SampleRender;
import cnc.woodcut.machine.maintenance.ui.arassistant.common.samplerender.Shader;
import cnc.woodcut.machine.maintenance.ui.arassistant.common.samplerender.Texture;
import cnc.woodcut.machine.maintenance.ui.arassistant.common.samplerender.VertexBuffer;
import cnc.woodcut.machine.maintenance.ui.arassistant.common.samplerender.arcore.BackgroundRenderer;
import cnc.woodcut.machine.maintenance.ui.arassistant.common.samplerender.arcore.PlaneRenderer;
import cnc.woodcut.machine.maintenance.ui.arassistant.common.samplerender.arcore.SpecularCubemapFilter;
import cnc.woodcut.machine.maintenance.ui.arassistant.common.helpers.CameraPermissionHelper;
import cnc.woodcut.machine.maintenance.ui.arassistant.common.helpers.DepthSettings;
import cnc.woodcut.machine.maintenance.ui.arassistant.common.helpers.DisplayRotationHelper;
import cnc.woodcut.machine.maintenance.ui.arassistant.common.helpers.InstantPlacementSettings;
import cnc.woodcut.machine.maintenance.ui.arassistant.common.helpers.SnackbarHelper;
import cnc.woodcut.machine.maintenance.ui.arassistant.common.helpers.TapHelper;
import cnc.woodcut.machine.maintenance.ui.arassistant.common.helpers.TrackingStateHelper;


// ARCore library
import com.google.ar.core.Anchor;
import com.google.ar.core.ArCoreApk;
import com.google.ar.core.Camera;
import com.google.ar.core.Config;
import com.google.ar.core.DepthPoint;
import com.google.ar.core.Frame;
import com.google.ar.core.HitResult;
import com.google.ar.core.InstantPlacementPoint;
import com.google.ar.core.Plane;
import com.google.ar.core.Point;
import com.google.ar.core.Session;
import com.google.ar.core.Trackable;
import com.google.ar.core.TrackingState;
import com.google.ar.core.exceptions.UnavailableApkTooOldException;
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException;
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException;
import com.google.ar.core.exceptions.UnavailableSdkTooOldException;
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException;

//
import android.opengl.GLSurfaceView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public abstract class ARAssistant extends Fragment implements SampleRender.Renderer {
    /*-* Vars Definitions  ---------------------------------------------------------------------*/
    public boolean installRequested;
    public GLSurfaceView glSurfaceView;
    public Session session;
    private ActivityResultLauncher<String> requestPermissionLauncher;

    //
    public static final String SEARCHING_PLANE_MESSAGE = "Searching for surfaces...";
    public static final String WAITING_FOR_TAP_MESSAGE = "Tap on a surface to place an object.";

    public static final int CAMERA_PERMISSION_CODE = 1001;
    public static final String CAMERA_PERMISSION = Manifest.permission.CAMERA;


    // See the definition of updateSphericalHarmonicsCoefficients for an explanation of these
    // constants.
    public static final float[] sphericalHarmonicFactors = {
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

    public static final float Z_NEAR = 0.1f;
    public static final float Z_FAR = 100f;

    public static final int CUBEMAP_RESOLUTION = 16;
    public static final int CUBEMAP_NUMBER_OF_IMPORTANCE_SAMPLES = 32;

    // Rendering. The Renderers are created here, and initialized when the GL surface is created.
    public GLSurfaceView surfaceView;

    public final SnackbarHelper messageSnackbarHelper = new SnackbarHelper();
    public DisplayRotationHelper displayRotationHelper;
    public final TrackingStateHelper trackingStateHelper = new TrackingStateHelper(getActivity());
    public TapHelper tapHelper;
    public SampleRender render;

    public PlaneRenderer planeRenderer;
    public BackgroundRenderer backgroundRenderer;
    public Framebuffer virtualSceneFramebuffer;
    public boolean hasSetTextureNames = false;

    public final DepthSettings depthSettings = new DepthSettings();
    public boolean[] depthSettingsMenuDialogCheckboxes = new boolean[2];

    public final InstantPlacementSettings instantPlacementSettings = new InstantPlacementSettings();
    public boolean[] instantPlacementSettingsMenuDialogCheckboxes = new boolean[1];
    // Assumed distance from the device camera to the surface on which user will try to place objects.
    // This value affects the apparent scale of objects while the tracking method of the
    // Instant Placement point is SCREENSPACE_WITH_APPROXIMATE_DISTANCE.
    // Values in the [0.2, 2.0] meter range are a good choice for most AR experiences. Use lower
    // values for AR experiences where users are expected to place objects on surfaces close to the
    // camera. Use larger values for experiences where the user will likely be standing and trying to
    // place an object on the ground or floor in front of them.
    public static final float APPROXIMATE_DISTANCE_METERS = 2.0f;

    // Point Cloud
    public VertexBuffer pointCloudVertexBuffer;
    public Mesh pointCloudMesh;
    public Shader pointCloudShader;
    // Keep track of the last point cloud rendered to avoid updating the VBO if point cloud
    // was not changed.  Do this using the timestamp since we can't compare PointCloud objects.
    public long lastPointCloudTimestamp = 0;

    // Virtual object (ARCore pawn)
    public Mesh virtualObjectMesh;
    public Shader virtualObjectShader;
    public Texture virtualObjectAlbedoTexture;
    public Texture virtualObjectAlbedoInstantPlacementTexture;

    public final List<WrappedAnchor> wrappedAnchors = new ArrayList<>();

    // Environmental HDR
    public Texture dfgTexture;
    public SpecularCubemapFilter cubemapFilter;

    // Temporary matrix allocated here to reduce number of allocations for each frame.
    public final float[] modelMatrix = new float[16];
    public final float[] viewMatrix = new float[16];
    public final float[] projectionMatrix = new float[16];
    public final float[] modelViewMatrix = new float[16]; // view x model
    public final float[] modelViewProjectionMatrix = new float[16]; // projection x view x model
    public final float[] sphericalHarmonicsCoefficients = new float[9 * 3];
    public final float[] viewInverseMatrix = new float[16];
    public final float[] worldLightDirection = {0.0f, 0.0f, 0.0f, 0.0f};
    public final float[] viewLightDirection = new float[4]; // view x world light direction

    /*-* End Vars Definitions  -----------------------------------------------------------------*/
    @Override
    public  View onCreateView (LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
         View view = inflater.inflate(R.layout.fragment_arassistant,container,false);
         surfaceView = view.findViewById(R.id.surfaceview);

         displayRotationHelper = new DisplayRotationHelper(getContext());
         surfaceView.setEGLContextClientVersion(2);
         surfaceView.setPreserveEGLContextOnPause(true);

         // Set up touch listener
         tapHelper = new TapHelper(getContext());
         surfaceView.setOnTouchListener(tapHelper);

         // Set up renderer
        AssetManager assetManager = requireContext().getAssets();
        render = new SampleRender(surfaceView, (ARAssistantFragment) this, assetManager);

        installRequested = false;
        depthSettings.onCreate(getContext());
        surfaceView.onResume();

         return view;
    }

    @Override
    public void onDestroy() {
        if (session != null) {
            // Explicitly close ARCore Session to release native resources.
            // Review the API reference for important considerations before calling close() in apps with
            // more complicated lifecycle requirements:
            // https://developers.google.com/ar/reference/java/arcore/reference/com/google/ar/core/Session#close()
            session.close();
            session = null;
        }
        super.onDestroy();
    }

    /** Configures the session with feature settings. */
    private void configureSession() {
        Config config = session.getConfig();
        config.setLightEstimationMode(Config.LightEstimationMode.ENVIRONMENTAL_HDR);
        if (session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
            config.setDepthMode(Config.DepthMode.AUTOMATIC);
        } else {
            config.setDepthMode(Config.DepthMode.DISABLED);
        }
        if (instantPlacementSettings.isInstantPlacementEnabled()) {
            config.setInstantPlacementMode(Config.InstantPlacementMode.LOCAL_Y_UP);
        } else {
            config.setInstantPlacementMode(Config.InstantPlacementMode.DISABLED);
        }
        session.configure(config);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (session == null) {
            Exception exception = null;
            String message = null;
            try {
                // Always check the latest availability.
                ArCoreApk.Availability availability = ArCoreApk.getInstance().checkAvailability(getContext());

                // In all other cases, try to install ARCore and handle installation failures.
                if (availability != ArCoreApk.Availability.SUPPORTED_INSTALLED) {
                    switch (ArCoreApk.getInstance().requestInstall(getActivity(), !installRequested)) {
                        case INSTALL_REQUESTED:
                            installRequested = true;
                            return;
                        case INSTALLED:
                            break;
                    }
                }

                // ARCore requires camera permissions to operate. If we did not yet obtain runtime
                // permission on Android M and above, now is a good time to ask the user for it.
                if (!CameraPermissionHelper.hasCameraPermission(getActivity())) {
                    CameraPermissionHelper.requestCameraPermission(getActivity());
                    return;
                }

                // Create the session.
                session = new Session(/* context= */ getContext());
            } catch (UnavailableArcoreNotInstalledException
                     | UnavailableUserDeclinedInstallationException e) {
                message = "Please install ARCore";
                exception = e;
            } catch (UnavailableApkTooOldException e) {
                message = "Please update ARCore";
                exception = e;
            } catch (UnavailableSdkTooOldException e) {
                message = "Please update this app";
                exception = e;
            } catch (UnavailableDeviceNotCompatibleException e) {
                message = "This device does not support AR";
                exception = e;
            } catch (Exception e) {
                message = "Failed to create AR session";
                exception = e;
            }

            if (message != null) {
                messageSnackbarHelper.showError(getActivity(), message);
                Log.e("onResume","Exception creating session", exception);
                return;
            }
        }
        // Note that order matters - see the note in onPause(), the reverse applies here.
        configureSession();

        // To record a live camera session for later playback, call
        // `session.startRecording(recordingConfig)` at anytime. To playback a previously recorded AR
        // session instead of using the live camera feed, call
        // `session.setPlaybackDatasetUri(Uri)` before calling `session.resume()`. To
        // learn more about recording and playback, see:
        // https://developers.google.com/ar/develop/java/recording-and-playback      session.resume();

        surfaceView.onResume();
        displayRotationHelper.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (session != null) {
            // Note that the order matters - GLSurfaceView is paused first so that it does not try
            // to query the session. If Session is paused before GLSurfaceView, GLSurfaceView may
            // still call session.update() and get a SessionPausedException.
            displayRotationHelper.onPause();
            surfaceView.onPause();
            session.pause();
        }
    }



    // Handle only one tap per frame, as taps are usually low frequency compared to frame rate.
    public void handleTap(Frame frame, Camera camera) {
        MotionEvent tap = tapHelper.poll();
        if (tap != null && camera.getTrackingState() == TrackingState.TRACKING) {
            List<HitResult> hitResultList;
            if (instantPlacementSettings.isInstantPlacementEnabled()) {
                hitResultList =
                        frame.hitTestInstantPlacement(tap.getX(), tap.getY(), APPROXIMATE_DISTANCE_METERS);
            } else {
                hitResultList = frame.hitTest(tap);
            }
            for (HitResult hit : hitResultList) {
                // If any plane, Oriented Point, or Instant Placement Point was hit, create an anchor.
                Trackable trackable = hit.getTrackable();
                // If a plane was hit, check that it was hit inside the plane polygon.
                // DepthPoints are only returned if Config.DepthMode is set to AUTOMATIC.
                if ((trackable instanceof Plane
                        && ((Plane) trackable).isPoseInPolygon(hit.getHitPose())
                        && (PlaneRenderer.calculateDistanceToPlane(hit.getHitPose(), camera.getPose()) > 0))
                        || (trackable instanceof Point
                        && ((Point) trackable).getOrientationMode()
                        == Point.OrientationMode.ESTIMATED_SURFACE_NORMAL)
                        || (trackable instanceof InstantPlacementPoint)
                        || (trackable instanceof DepthPoint)) {
                    // Cap the number of objects created. This avoids overloading both the
                    // rendering system and ARCore.
                    if (wrappedAnchors.size() >= 20) {
                        wrappedAnchors.get(0).getAnchor().detach();
                        wrappedAnchors.remove(0);
                    }

                    // Adding an Anchor tells ARCore that it should track this position in
                    // space. This anchor is created on the Plane to place the 3D model
                    // in the correct position relative both to the world and to the plane.
                    wrappedAnchors.add(new WrappedAnchor(hit.createAnchor(), trackable));
                    // For devices that support the Depth API, shows a dialog to suggest enabling
                    // depth-based occlusion. This dialog needs to be spawned on the UI thread.

                    // Hits are sorted by depth. Consider only closest hit on a plane, Oriented Point, or
                    // Instant Placement Point.
                    break;
                }
            }
        }
    }
    @Override
    public void onSurfaceCreated(SampleRender render) {
        // Prepare the rendering objects. This involves reading shaders and 3D model files, so may throw
        // an IOException.
        try {
            planeRenderer = new PlaneRenderer(render);
            backgroundRenderer = new BackgroundRenderer(render);
            virtualSceneFramebuffer = new Framebuffer(render, /* width= */ 1, /* height= */ 1);

            cubemapFilter =
                    new SpecularCubemapFilter(
                            render, CUBEMAP_RESOLUTION, CUBEMAP_NUMBER_OF_IMPORTANCE_SAMPLES);
            // Load DFG lookup table for environmental lighting
            dfgTexture =
                    new Texture(
                            render,
                            Texture.Target.TEXTURE_2D,
                            Texture.WrapMode.CLAMP_TO_EDGE,
                            /* useMipmaps= */ false);
            // The dfg.raw file is a raw half-float texture with two channels.
            final int dfgResolution = 64;
            final int dfgChannels = 2;
            final int halfFloatSize = 2;
            Log.d("OnCreatedSurface","getContext().getAssets()"+ getContext().getAssets().open("models/machine.raw"));
            ByteBuffer buffer =
                    ByteBuffer.allocateDirect(dfgResolution * dfgResolution * dfgChannels * halfFloatSize);
            try (InputStream is = getActivity().getAssets().open("models/machine.raw")) {
                is.read(buffer.array());
            }
            // SampleRender abstraction leaks here.
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, dfgTexture.getTextureId());
            GLError.maybeThrowGLException("Failed to bind DFG texture", "glBindTexture");
            GLES30.glTexImage2D(
                    GLES30.GL_TEXTURE_2D,
                    /* level= */ 0,
                    GLES30.GL_RG16F,
                    /* width= */ dfgResolution,
                    /* height= */ dfgResolution,
                    /* border= */ 0,
                    GLES30.GL_RG,
                    GLES30.GL_HALF_FLOAT,
                    buffer);
            GLError.maybeThrowGLException("Failed to populate DFG texture", "glTexImage2D");

            // Point cloud
            pointCloudShader =
                    Shader.createFromAssets(
                                    render,
                                    "shaders/point_cloud.vert",
                                    "shaders/point_cloud.frag",
                                    /* defines= */ null)
                            .setVec4(
                                    "u_Color", new float[] {31.0f / 255.0f, 188.0f / 255.0f, 210.0f / 255.0f, 1.0f})
                            .setFloat("u_PointSize", 5.0f);
            // four entries per vertex: X, Y, Z, confidence
            pointCloudVertexBuffer =
                    new VertexBuffer(render, /* numberOfEntriesPerVertex= */ 4, /* entries= */ null);
            final VertexBuffer[] pointCloudVertexBuffers = {pointCloudVertexBuffer};
            pointCloudMesh =
                    new Mesh(
                            render, Mesh.PrimitiveMode.POINTS, /* indexBuffer= */ null, pointCloudVertexBuffers);

            // Virtual object to render (ARCore pawn)
            virtualObjectAlbedoTexture =
                    Texture.createFromAsset(
                            render,
                            "models/texture_diffuse.png",
                            Texture.WrapMode.CLAMP_TO_EDGE,
                            Texture.ColorFormat.SRGB);
            virtualObjectAlbedoInstantPlacementTexture =
                    Texture.createFromAsset(
                            render,
                            "models/texture_normal.png",
                            Texture.WrapMode.CLAMP_TO_EDGE,
                            Texture.ColorFormat.SRGB);
            Texture virtualObjectPbrTexture =
                    Texture.createFromAsset(
                            render,
                            "models/texture_metallic.png",
                            Texture.WrapMode.CLAMP_TO_EDGE,
                            Texture.ColorFormat.LINEAR);

            virtualObjectMesh = Mesh.createFromAsset(render, "models/machine_0.obj");
            virtualObjectShader =
                    Shader.createFromAssets(
                                    render,
                                    "shaders/environmental_hdr.vert",
                                    "shaders/environmental_hdr.frag",
                                    /* defines= */ new HashMap<String, String>() {
                                        {
                                            put(
                                                    "NUMBER_OF_MIPMAP_LEVELS",
                                                    Integer.toString(cubemapFilter.getNumberOfMipmapLevels()));
                                        }
                                    })
                            .setTexture("u_AlbedoTexture", virtualObjectAlbedoTexture)
                            .setTexture("u_RoughnessMetallicAmbientOcclusionTexture", virtualObjectPbrTexture)
                            .setTexture("u_Cubemap", cubemapFilter.getFilteredCubemapTexture())
                            .setTexture("u_DfgTexture", dfgTexture);
        } catch (IOException e) {
            Log.e("OnSurfaceCreate", "Failed to read a required asset file", e);
            messageSnackbarHelper.showError(getActivity(), "Failed to read a required asset file: " + e);
        }
    }
    @Override
    public void onSurfaceChanged(SampleRender render, int width, int height) {
        displayRotationHelper.onSurfaceChanged(width, height);
        virtualSceneFramebuffer.resize(width, height);
    }

    /** Checks if we detected at least one plane. */
    public boolean hasTrackingPlane() {
        for (Plane plane : session.getAllTrackables(Plane.class)) {
            if (plane.getTrackingState() == TrackingState.TRACKING) {
                return true;
            }
        }
        return false;
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



