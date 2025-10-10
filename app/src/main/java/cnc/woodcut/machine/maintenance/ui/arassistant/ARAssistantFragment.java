package cnc.woodcut.machine.maintenance.ui.arassistant;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

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

    private ArFragment arFragment;
    private ModelRenderable modelRenderable;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_arassistant_img2ar, container, false);
        arFragment = (ArFragment) getChildFragmentManager().findFragmentById(R.id.fragment_img2ar);

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

        return rootView;
    }

    private void loadModel() {
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
