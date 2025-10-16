package cnc.woodcut.machine.maintenance.ui.arassistant;

import static androidx.core.content.ContentProviderCompat.requireContext;

import android.Manifest;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
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
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.TransformableNode;

import cnc.woodcut.machine.maintenance.R;

public class ARAssistantFragment extends Fragment {

    private static final int CAMERA_PERMISSION_CODE = 100;
    private ArFragment arFragment;
    private ModelRenderable modelRenderable;

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

        loadModel();

        arFragment.setOnTapArPlaneListener((hitResult, plane, motionEvent) -> {
            if (modelRenderable == null) return;

            Anchor anchor = hitResult.createAnchor();
            AnchorNode anchorNode = new AnchorNode(anchor);
            anchorNode.setParent(arFragment.getArSceneView().getScene());

            TransformableNode node = new TransformableNode(arFragment.getTransformationSystem());
            node.setParent(anchorNode);
            node.setRenderable(modelRenderable);
            node.select();

            node.setOnTapListener((hitTestResult, motionEvent1) -> {
                Quaternion rotation = Quaternion.axisAngle(new Vector3(0f, 1f, 0f), 45f);
                node.setLocalRotation(Quaternion.multiply(node.getLocalRotation(), rotation));
            });
        });
    }

    private void loadModel() {
        ModelRenderable.builder()
            .setSource(requireContext(), Uri.parse("models/machine.sfb")) //No RCB file at uri: models/machine_model.glb
            .build()
            .thenAccept(renderable -> modelRenderable = renderable)
            .exceptionally(throwable -> {
                Toast.makeText(getContext(), "Model failed to load", Toast.LENGTH_SHORT).show();
                return null;
            });
    }
}//__end_ARFragment
