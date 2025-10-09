package cnc.woodcut.machine.maintenance.ui.arassistant;

import static com.google.android.material.internal.ContextUtils.getActivity;
import static java.security.AccessController.getContext;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLES30;
import android.opengl.GLSurfaceView;
import android.util.Log;
import android.view.Surface;


import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.Nullable;

import com.google.ar.core.Anchor;
import com.google.ar.core.Camera;
import com.google.ar.core.Frame;
import com.google.ar.core.Plane;
import com.google.ar.core.Pose;
import com.google.ar.core.Session;
import com.google.ar.core.Trackable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import cnc.woodcut.machine.maintenance.ui.arassistant.common.helpers.DepthSettings;
import cnc.woodcut.machine.maintenance.ui.arassistant.common.helpers.DisplayRotationHelper;
import cnc.woodcut.machine.maintenance.ui.arassistant.common.helpers.InstantPlacementSettings;
import cnc.woodcut.machine.maintenance.ui.arassistant.common.helpers.SnackbarHelper;
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

public abstract class ARAssistantRenderer implements SampleRender.Renderer {
    private static final String TAG = "ARAssistantRenderer";
    private final Context context;
    private Session session;

    /*-* Vars Definitions  ---------------------------------------------------------------------*/
    public boolean installRequested;
    public GLSurfaceView glSurfaceView;

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

    //public final List<WrappedAnchor> wrappedAnchors = new ArrayList<>();

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

    public ARAssistantRenderer(Context context, Session session) {
        this.context = context;
        this.session = session;
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

            ByteBuffer buffer =
                    ByteBuffer.allocateDirect(dfgResolution * dfgResolution * dfgChannels * halfFloatSize);
            try (InputStream is = getActivity(context).getAssets().open("models/machine.raw")) {
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
            /*pointCloudShader =
                    Shader.createFromAssets(
                                    render,
                                    "shaders/point_cloud.vert",
                                    "shaders/point_cloud.frag",
                                    /* defines= */// null)
                            //.setVec4(
                              //      "u_Color", new float[] {31.0f / 255.0f, 188.0f / 255.0f, 210.0f / 255.0f, 1.0f})
                            //.setFloat("u_PointSize", 5.0f);
            // four entries per vertex: X, Y, Z, confidence
            //pointCloudVertexBuffer =
              //      new VertexBuffer(render, /* numberOfEntriesPerVertex= */ 4, /* entries= */ null);
            //final VertexBuffer[] pointCloudVertexBuffers = {pointCloudVertexBuffer};
            //pointCloudMesh =
              //      new Mesh(
                //            render, Mesh.PrimitiveMode.POINTS, /* indexBuffer= */ null, pointCloudVertexBuffers); */

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

    @Override
    public void onSurfaceChanged(SampleRender render, int width, int height) {
        displayRotationHelper.onSurfaceChanged(width, height);
        virtualSceneFramebuffer.resize(width, height);
    }
}//__End_ARAssistantRenderer
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