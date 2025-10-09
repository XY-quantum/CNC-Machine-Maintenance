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

import android.media.Image;
import android.net.Uri;
import android.opengl.GLES30;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
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
import com.google.ar.core.Config;
import com.google.ar.core.Frame;
import com.google.ar.core.InstantPlacementPoint;
import com.google.ar.core.Plane;
import com.google.ar.core.PointCloud;
import com.google.ar.core.Session;
import com.google.ar.core.Trackable;
import com.google.ar.core.TrackingFailureReason;
import com.google.ar.core.TrackingState;
import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.core.exceptions.NotYetAvailableException;
import com.google.ar.core.exceptions.UnavailableApkTooOldException;
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException;
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException;
import com.google.ar.core.exceptions.UnavailableException;
import com.google.ar.core.exceptions.UnavailableSdkTooOldException;
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import cnc.woodcut.machine.maintenance.R;
import cnc.woodcut.machine.maintenance.ui.arassistant.common.helpers.CameraPermissionHelper;
import cnc.woodcut.machine.maintenance.ui.arassistant.common.helpers.DisplayRotationHelper;
import cnc.woodcut.machine.maintenance.ui.arassistant.common.helpers.InstantPlacementSettings;
import cnc.woodcut.machine.maintenance.ui.arassistant.common.helpers.TapHelper;
import cnc.woodcut.machine.maintenance.ui.arassistant.common.helpers.TrackingStateHelper;
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


public class ARAssistantFragment extends Fragment {

    private static final String TAG= ARAssistantFragment.class.getSimpleName();
    private final InstantPlacementSettings instantPlacementSettings = new InstantPlacementSettings();
    private boolean installRequested;

    private GLSurfaceView glSurfaceView;
    private ARAssistantRenderer arRenderer;
    private Session session;

    private DisplayRotationHelper displayRotationHelper;

    private final List<WrappedAnchor> wrappedAnchors = new ArrayList<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_arassistant, container, false);
        glSurfaceView = rootView.findViewById(R.id.glsurfaceview);
        displayRotationHelper = new DisplayRotationHelper(requireContext());

        // Check camera permission
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, ARAssistantRenderer.CAMERA_PERMISSION_CODE);
        } else {
            setupSessionAndRenderer();
        }

        return rootView;
    }

    private void setupSessionAndRenderer() {
        try {
            session = new Session(requireContext());
            arRenderer = new ARAssistantRenderer(requireContext(), session) {
                @Override
                public void onSurfaceChanged(SampleRender render, int width, int height) {
                    if (displayRotationHelper != null) {
                        displayRotationHelper.onSurfaceChanged(width, height);
                    }
                    virtualSceneFramebuffer.resize(width, height);
                }

                @Override
                public void onDrawFrame(SampleRender render) {
                    if (session == null) {
                        return;
                    }

                    // Texture names should only be set once on a GL thread unless they change. This is done during
                    // onDrawFrame rather than onSurfaceCreated since the session is not guaranteed to have been
                    // initialized during the execution of onSurfaceCreated.
                    if (!hasSetTextureNames) {
                        session.setCameraTextureNames(
                                new int[] {backgroundRenderer.getCameraColorTexture().getTextureId()});
                        hasSetTextureNames = true;
                    }

                    // -- Update per-frame state

                    // Notify ARCore session that the view size changed so that the perspective matrix and
                    // the video background can be properly adjusted.
                    if (displayRotationHelper != null) {
                        displayRotationHelper.updateSessionIfNeeded(session);
                    }
                    // Obtain the current frame from the AR Session. When the configuration is set to
                    // UpdateMode.BLOCKING (it is by default), this will throttle the rendering to the
                    // camera framerate.
                    Frame frame;
                    try {
                        frame = session.update();
                    } catch (CameraNotAvailableException e) {
                        Log.e(TAG, "Camera not available during onDrawFrame", e);
                        //messageSnackbarHelper.showError(this, "Camera not available. Try restarting the app.");
                        return;
                    }
                    Camera camera = frame.getCamera();

                    // Update BackgroundRenderer state to match the depth settings.
                    try {
                        backgroundRenderer.setUseDepthVisualization(
                                render, depthSettings.depthColorVisualizationEnabled());
                        backgroundRenderer.setUseOcclusion(render, depthSettings.useDepthForOcclusion());
                    } catch (IOException e) {
                        Log.e(TAG, "Failed to read a required asset file", e);
                        //messageSnackbarHelper.showError(this, "Failed to read a required asset file: " + e);
                        return;
                    }
                    // BackgroundRenderer.updateDisplayGeometry must be called every frame to update the coordinates
                    // used to draw the background camera image.
                    backgroundRenderer.updateDisplayGeometry(frame);

                    if (camera.getTrackingState() == TrackingState.TRACKING
                            && (depthSettings.useDepthForOcclusion()
                            || depthSettings.depthColorVisualizationEnabled())) {
                        try (Image depthImage = frame.acquireDepthImage16Bits()) {
                            backgroundRenderer.updateCameraDepthTexture(depthImage);
                        } catch (NotYetAvailableException e) {
                            // This normally means that depth data is not available yet. This is normal so we will not
                            // spam the logcat with this.
                        }
                    }
                    // Keep the screen unlocked while tracking, but allow it to lock when tracking stops.
                    //trackingStateHelper.updateKeepScreenOnFlag(camera.getTrackingState());

                    // Show a message based on whether tracking has failed, if planes are detected, and if the user
                    // has placed any objects.
                    String message = null;
                    if (camera.getTrackingState() == TrackingState.PAUSED) {
                        if (camera.getTrackingFailureReason() == TrackingFailureReason.NONE) {
                            message = SEARCHING_PLANE_MESSAGE;
                        } else {
                            message = TrackingStateHelper.getTrackingFailureReasonString(camera);
                        }
                    } else if (hasTrackingPlane()) {
                        if (wrappedAnchors.isEmpty()) {
                            message = WAITING_FOR_TAP_MESSAGE;
                        }
                    } else {
                        message = SEARCHING_PLANE_MESSAGE;
                    }
                    if (message == null) {
                        messageSnackbarHelper.hide(this);
                    } /*else {
                        messageSnackbarHelper.showMessage(this, message);
                    }*/

                    // -- Draw background

                    if (frame.getTimestamp() != 0) {
                        // Suppress rendering if the camera did not produce the first frame yet. This is to avoid
                        // drawing possible leftover data from previous sessions if the texture is reused.
                        backgroundRenderer.drawBackground(render);
                    }

                    // If not tracking, don't draw 3D objects.
                    if (camera.getTrackingState() == TrackingState.PAUSED) {
                        return;
                    }

                    // -- Draw non-occluded virtual objects (planes, point cloud)

                    // Get projection matrix.
                    camera.getProjectionMatrix(projectionMatrix, 0, Z_NEAR, Z_FAR);

                    // Get camera matrix and draw.
                    camera.getViewMatrix(viewMatrix, 0);

                    // Visualize tracked points.
                    // Use try-with-resources to automatically release the point cloud.
                    try (PointCloud pointCloud = frame.acquirePointCloud()) {
                        if (pointCloud.getTimestamp() > lastPointCloudTimestamp) {
                            pointCloudVertexBuffer.set(pointCloud.getPoints());
                            lastPointCloudTimestamp = pointCloud.getTimestamp();
                        }
                        Matrix.multiplyMM(modelViewProjectionMatrix, 0, projectionMatrix, 0, viewMatrix, 0);
                        pointCloudShader.setMat4("u_ModelViewProjection", modelViewProjectionMatrix);
                        render.draw(pointCloudMesh, pointCloudShader);
                    }

                    // Visualize planes.
                    planeRenderer.drawPlanes(
                            render,
                            session.getAllTrackables(Plane.class),
                            camera.getDisplayOrientedPose(),
                            projectionMatrix);

                    // -- Draw occluded virtual objects

                    // Update lighting parameters in the shader
                    //updateLightEstimation(frame.getLightEstimate(), viewMatrix);

                    // Visualize anchors created by touch.
                    render.clear(virtualSceneFramebuffer, 0f, 0f, 0f, 0f);
                    for (WrappedAnchor wrappedAnchor : wrappedAnchors) {
                        Anchor anchor = wrappedAnchor.getAnchor();
                        Trackable trackable = wrappedAnchor.getTrackable();
                        if (anchor.getTrackingState() != TrackingState.TRACKING) {
                            continue;
                        }

                        // Get the current pose of an Anchor in world space. The Anchor pose is updated
                        // during calls to session.update() as ARCore refines its estimate of the world.
                        anchor.getPose().toMatrix(modelMatrix, 0);

                        // Calculate model/view/projection matrices
                        Matrix.multiplyMM(modelViewMatrix, 0, viewMatrix, 0, modelMatrix, 0);
                        Matrix.multiplyMM(modelViewProjectionMatrix, 0, projectionMatrix, 0, modelViewMatrix, 0);

                        // Update shader properties and draw
                        virtualObjectShader.setMat4("u_ModelView", modelViewMatrix);
                        virtualObjectShader.setMat4("u_ModelViewProjection", modelViewProjectionMatrix);

                        if (trackable instanceof InstantPlacementPoint
                                && ((InstantPlacementPoint) trackable).getTrackingMethod()
                                == InstantPlacementPoint.TrackingMethod.SCREENSPACE_WITH_APPROXIMATE_DISTANCE) {
                            virtualObjectShader.setTexture(
                                    "u_AlbedoTexture", virtualObjectAlbedoInstantPlacementTexture);
                        } else {
                            virtualObjectShader.setTexture("u_AlbedoTexture", virtualObjectAlbedoTexture);
                        }

                        render.draw(virtualObjectMesh, virtualObjectShader, virtualSceneFramebuffer);
                    }

                    // Compose the virtual scene with the background.
                    backgroundRenderer.drawVirtualScene(render, virtualSceneFramebuffer, Z_NEAR, Z_FAR);
                }

                @Override
                public void onWindowFocusChanged(boolean hasFocus) {

                }

                @Override
                public void onSurfaceCreated(GL10 gl, EGLConfig config) {

                }

                @Override
                public void onSurfaceChanged(GL10 gl, int width, int height) {

                }

                @Override
                public void onDrawFrame(GL10 gl) {

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
                        //messageSnackbarHelper.showError(getActivity(), "Failed to read a required asset file: " + e);
                    }
                }

            };

            SampleRender render = new SampleRender(glSurfaceView, arRenderer, requireContext().getAssets());

        } catch (UnavailableException e) {
            Log.e("ARAssistantFragment", "ARCore not available", e);
        }
    }

    /** Checks if we detected at least one plane. */
    private boolean hasTrackingPlane() {
        for (Plane plane : session.getAllTrackables(Plane.class)) {
            if (plane.getTrackingState() == TrackingState.TRACKING) {
                return true;
            }
        }
        return false;
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
                //messageSnackbarHelper.showError(getActivity(), message);
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
        try {
            session.resume();
        } catch (CameraNotAvailableException e) {
            Log.e("onResume", "Camera not available. Try.",e);
            session = null;
            return;
        }
        try {
            if (displayRotationHelper != null) {
                displayRotationHelper.onResume();
            }
        }catch (Exception e){
            Log.e("OnResume","displayRotationHelper.onResume() failed",e);
        }
        glSurfaceView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (session != null) {
            // Note that the order matters - GLSurfaceView is paused first so that it does not try
            // to query the session. If Session is paused before GLSurfaceView, GLSurfaceView may
            // still call session.update() and get a SessionPausedException.
            displayRotationHelper.onPause();
            glSurfaceView.onPause();
            session.pause();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == ARAssistantRenderer.CAMERA_PERMISSION_CODE &&
                grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            setupSessionAndRenderer();
        } else {
            Toast.makeText(requireContext(), "Camera permission is required for AR", Toast.LENGTH_LONG).show();
        }
    }
}