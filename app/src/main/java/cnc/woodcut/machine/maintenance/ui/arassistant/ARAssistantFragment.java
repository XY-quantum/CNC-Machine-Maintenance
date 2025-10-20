package cnc.woodcut.machine.maintenance.ui.arassistant;

import static androidx.core.content.ContentProviderCompat.requireContext;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.renderscript.ScriptGroup;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.ar.core.Anchor;
import com.google.ar.core.AugmentedImage;
import com.google.ar.core.AugmentedImageDatabase;
import com.google.ar.core.Config;
import com.google.ar.core.Frame;
import com.google.ar.core.Session;
import com.google.ar.core.TrackingState;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.RotationController;
import com.google.ar.sceneform.ux.TransformableNode;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

import cnc.woodcut.machine.maintenance.R;

public class ARAssistantFragment extends Fragment {

    private static final int CAMERA_PERMISSION_CODE = 100;
    private ArFragment arFragment;
    private ModelRenderable modelRenderable;
    private Session session;
    private Frame frame;
    AugmentedImageDatabase imageDatabase = null;

    private Boolean sessionConfigured = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_arassistant_img2ar, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
        } else {
            setupAR();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_CODE){
            setupAR();
        } else {
            Toast.makeText(getContext(), "Camera permission is required for AR", Toast.LENGTH_LONG).show();
        }
    }

    private void setupAR() {
        arFragment = (ArFragment) getChildFragmentManager().findFragmentById(R.id.fragment_img2ar);

        if (arFragment == null) {
            Log.e("ARFragment", "Failed to find ArFragment");
            return;
        }

        // Disable the hand scanning animation
        arFragment.getPlaneDiscoveryController().hide();
        arFragment.getPlaneDiscoveryController().setInstructionView(null);

        // Configure session
        arFragment.getArSceneView().getScene().addOnUpdateListener(frameTime -> {
            if (sessionConfigured) return;

            session = arFragment.getArSceneView().getSession();
            if (session != null) {
                try (InputStream inputStream = requireContext().getAssets().open("databaseMachineX.imgdb")) {
                    imageDatabase = AugmentedImageDatabase.deserialize(session, inputStream);

                    Config config = new Config(session);
                    config.setAugmentedImageDatabase(imageDatabase);
                    config.setUpdateMode(Config.UpdateMode.LATEST_CAMERA_IMAGE);
                    session.configure(config);

                    sessionConfigured = true;
                    Log.d("ARSetup", "Session configured with AugmentedImageDatabase.");
                } catch (IOException e) {
                    Log.e("ARSetup", "Failed to load .imgdb", e);
                }
            }
        });

        // Load bitmap from assets
        Bitmap bitmap;
        try (InputStream bitmapString = requireActivity().getAssets().open("machine.png")) {
            bitmap = BitmapFactory.decodeStream(bitmapString);
        } catch (IOException e) {
            // The bitmap could not be found in assets; handle this error appropriately.
            throw new AssertionError("The bitmap could not be found in assets.", e);
        }

        // Track images
        arFragment.getArSceneView().getScene().addOnUpdateListener(frameTime -> {
            Frame frame = arFragment.getArSceneView().getArFrame();
            if (frame == null || modelRenderable == null) return;

            Collection<AugmentedImage> updatedAugmentedImages =
                    frame.getUpdatedTrackables(AugmentedImage.class);
            for (AugmentedImage img : updatedAugmentedImages) {
                if (img.getTrackingState() == TrackingState.TRACKING && img.getName().equals("machine")) {
                    Anchor anchor         = img.createAnchor(img.getCenterPose());
                    AnchorNode anchorNode = new AnchorNode(anchor);
                    anchorNode.setParent(arFragment.getArSceneView().getScene());

                    TransformableNode modelNode = new TransformableNode(arFragment.getTransformationSystem());
                    modelNode.setRenderable(modelRenderable);
                    modelNode.setParent(anchorNode);
                    modelNode.setLocalScale(new Vector3(0.1f, 0.1f, 0.1f));
                    modelNode.select();
                }
            }
            loadModel();
        });

    }//end_setupAR

    private void loadModel() {
        Log.d("ARSetup","loadModel funcxtion is called");
        ModelRenderable.builder()
                .setSource(requireContext(), Uri.parse("models/machine_model.glb"))
                .build()
                .thenAccept(renderable -> modelRenderable = renderable)
                .exceptionally(throwable -> {
                    Toast.makeText(getContext(), "Model failed to load", Toast.LENGTH_SHORT).show();
                    return null;
                });
    }
}//__end_ARFragment
