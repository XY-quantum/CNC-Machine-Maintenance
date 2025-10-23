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
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;

import com.google.ar.core.Anchor;
import com.google.ar.core.AugmentedImage;
import com.google.ar.core.AugmentedImageDatabase;
import com.google.ar.core.Config;
import com.google.ar.core.Frame;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.core.Point;
import com.google.ar.core.Session;
import com.google.ar.core.Trackable;
import com.google.ar.core.TrackingState;
import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.ViewRenderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.RotationController;
import com.google.ar.sceneform.ux.TransformableNode;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import cnc.woodcut.machine.maintenance.R;

public class ARAssistantFragment extends ArFragment{
    private boolean modelPlaced = false;

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        getPlaneDiscoveryController().hide();
        getPlaneDiscoveryController().setInstructionView(null);
        getArSceneView().getPlaneRenderer().setEnabled(false);

        ImageButton settingsButton = view.findViewById(R.id.settings_button);
        if (settingsButton == null) {
            Log.e("YourFragment", "settingsButton is null! Check your layout XML.");
            return; // Avoid crash
        }

        settingsButton.bringToFront();
        //ViewCompat.setZ(settingsButton, 10f);

        // Use for testing the rendering 3D model
        /*getArSceneView().getScene().setOnTouchListener((hitTestResult, motionEvent) -> {
            if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                Frame frame = getArSceneView().getArFrame();
                if (frame != null) {
                    List<HitResult> hitResults = frame.hitTest(motionEvent);
                    for (HitResult hit : hitResults) {
                        Trackable trackable = hit.getTrackable();
                        if (trackable instanceof Plane || trackable instanceof Point) {
                            Anchor anchor = hit.createAnchor();
                            placeModelAtAnchor(anchor);
                            break;
                        }
                    }
                }
            }
            return true;
        });*/
    }
    private void placeModelAtAnchor(Anchor anchor) {
        AnchorNode anchorNode = new AnchorNode(anchor);
        anchorNode.setParent(getArSceneView().getScene());

        ModelRenderable.builder()
                        //RenderableSource.builder()
                                .setSource(getContext(), Uri.parse("models/machine.sfb")) //for testing rendering 3D model
                                //.setRecenterMode(RenderableSource.RecenterMode.ROOT)
                                //.build())
                .setRegistryId("machine.sfb")
                .build()
                .thenAccept(renderable -> {
                    TransformableNode modelNode = new TransformableNode(getTransformationSystem());
                    modelNode.setParent(anchorNode);
                    modelNode.setRenderable(renderable);
                    modelNode.select();
                })
                .exceptionally(throwable -> {
                    Log.e("ARSetup", "Model loading failed", throwable);
                    return null;
                });
    }
    @Override
    public void onResume() {
        super.onResume();
        // No need to call getArSceneView().resume() here
    }

    @Override
    public void onPause() {
        super.onPause();
        // No need to call getArSceneView().pause() here
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //@Override
        //    public void onResume() {
        //        super.onResume();
        //        // No need to call getArSceneView().resume() here
        //    }
        //
        //    @Override
        //    public void onPause() {
        //        super.onPause();
        //        // No need to call getArSceneView().pause() here
        //    }
        //
        //    @Override
        //    public void onDestroy() {
        //        super.onDestroy();
        //        // No need to call getArSceneView().destroy() here
        //    } No need to call getArSceneView().destroy() here
    }

    @Override
    protected Config getSessionConfiguration(Session session) {
        Config config = super.getSessionConfiguration(session);

        try {
            InputStream is = getContext().getAssets().open("earthmachines.imgdb");
            AugmentedImageDatabase database = AugmentedImageDatabase.deserialize(session, is);
            config.setAugmentedImageDatabase(database);
        } catch (IOException e) {
            e.printStackTrace();
        }

        config.setUpdateMode(Config.UpdateMode.LATEST_CAMERA_IMAGE);
        config.setDepthMode(Config.DepthMode.AUTOMATIC);
        getArSceneView().setupSession(session);

        return config;
    }

    @Override
    public void onUpdate(FrameTime frameTime) {
        if (modelPlaced) return;  // Only place once

        Frame frame = getArSceneView().getArFrame();
        if (frame == null) return;

        Collection<AugmentedImage> images = frame.getUpdatedTrackables(AugmentedImage.class);
        for (AugmentedImage image : images) {
            if (image.getTrackingState() == TrackingState.TRACKING
                    && image.getTrackingMethod() == AugmentedImage.TrackingMethod.FULL_TRACKING) {
                placeModelOnImage(image);
                modelPlaced = true;
                break;
            }
        }
    }

    private void placeModelOnImage(AugmentedImage image) {
        Anchor anchor = image.createAnchor(image.getCenterPose());
        AnchorNode anchorNode = new AnchorNode(anchor);
        anchorNode.setParent(getArSceneView().getScene());
        Log.d("ARSetup","placeModelOnImage called");
        ModelRenderable.builder()
                .setSource(requireContext(), Uri.parse("models/cnc_machine" +
                        ".sfb")) //cnc_machine size is good
                .build()
                .thenAccept(renderable -> {
                    TransformableNode modelNode = new TransformableNode(getTransformationSystem());
                    modelNode.setParent(anchorNode);
                    modelNode.setRenderable(renderable);

                    // Get image extents
                    float imageWidth = image.getExtentX();
                    float imageHeight = image.getExtentZ();
                    float scaleFactor = Math.min(imageWidth, imageHeight);

                    // Scale the model uniformly based on the image size
                    modelNode.setLocalScale(new Vector3(scaleFactor, scaleFactor, scaleFactor));

                    // Offset the model to the right side of the image (side-by-side)
                    // X axis offset equals image width, Y is slightly up, Z is aligned
                    Vector3 offsetPosition = new Vector3(imageWidth, scaleFactor * 0.1f, 0.0f);
                    modelNode.setLocalPosition(offsetPosition);

                    // Optional rotation
                    //modelNode.setLocalRotation(Quaternion.axisAngle(new Vector3(1, 0, 0), -90));
                    modelNode.getTranslationController();
                    modelNode.select();  // activate for gestures
                    // Create a new Node to attach the UI renderable
                    Node uiNode = new Node();

                    // Attach uiNode to the same anchorNode (the image anchor)
                    uiNode.setParent(anchorNode);

                    // Position uiNode next to the model, offset on X axis (e.g., double image width)
                    uiNode.setLocalPosition(new Vector3(imageWidth * 2, scaleFactor * 0.3f, 0));
                    ViewRenderable.builder()
                            .setView(requireContext(), R.layout.fragment_arassistant_img2ar)
                            .build()
                            .thenAccept(viewRenderable -> {
                                uiNode.setRenderable(viewRenderable);
                                TextView textView = viewRenderable.getView().findViewById(R.id.ar_text_view);
                                textView.setText("Virtual Control Panel : ");
                            });

                    // 2. Create the ViewRenderable (TextView)
                    /*ViewRenderable.builder()
                            .setView(requireContext(), R.layout.fragment_arassistant_img2ar)
                            .build()
                            .thenAccept(viewRenderable -> {
                                Node textNode = new Node();
                                textNode.setParent(anchorNode);
                                textNode.setRenderable(viewRenderable);

                                // Position the TextView above the model
                                textNode.setLocalPosition(new Vector3(0.0f, scaleFactor * 0.3f, 0.0f));

                                // Optionally, update the TextView text dynamically:
                                TextView textView = viewRenderable.getView().findViewById(R.id.ar_text_view);
                                textView.setText("Detected: " + image.getName());
                            });*/
                })
                .exceptionally(throwable -> {
                    Log.e("ARSetup", "Model loading failed", throwable);
                    return null;
                });
    }

}//__end_ARFragment
