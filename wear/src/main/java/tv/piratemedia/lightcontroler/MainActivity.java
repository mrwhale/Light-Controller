package tv.piratemedia.lightcontroler;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.support.wearable.view.DismissOverlayView;
import android.view.GestureDetector;
import android.view.MotionEvent;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.util.List;

public class MainActivity extends FragmentActivity {

    private ZonesPagerAdapter FragAdapter;
    private ViewPager ZonePager;
    private DismissOverlayView mDismissOverlayView;
    private GestureDetector mGestureDetector;
    public GoogleApiClient mGoogleApiClient;
    public Boolean isRound = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ShapeWear.initShapeWear(this);

        ShapeWear.setOnShapeChangeListener(new ShapeWear.OnShapeChangeListener() {
            @Override
            public void shapeDetected(ShapeWear.ScreenShape screenShape) {
                //Do your stuff here for example:
                switch (screenShape){
                    case MOTO_ROUND:
                    case ROUND:
                        isRound = true;
                        break;
                    case RECTANGLE:
                        isRound = false;
                        break;
                }
            }
        });

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .build();
        mGoogleApiClient.connect();

        boolean updateZones = !getIntent().getBooleanExtra("updated", false);

        if (mGoogleApiClient == null)
            return;
        if(updateZones) {
            final PendingResult<NodeApi.GetConnectedNodesResult> nodes = Wearable.NodeApi.getConnectedNodes(mGoogleApiClient);
            nodes.setResultCallback(new ResultCallback<NodeApi.GetConnectedNodesResult>() {
                @Override
                public void onResult(NodeApi.GetConnectedNodesResult result) {
                    final List<Node> nodes = result.getNodes();
                    if (nodes != null) {
                        for (int i = 0; i < nodes.size(); i++) {
                            final Node node = nodes.get(i);
                            Wearable.MessageApi.sendMessage(mGoogleApiClient, node.getId(), "/zones", null);
                        }
                    }
                }
            });
        }

        mDismissOverlayView = (DismissOverlayView) findViewById(R.id.dismiss);
        mDismissOverlayView.setIntroText(R.string.intro_text);
        mDismissOverlayView.showIntroIfNecessary();

        mGestureDetector = new GestureDetector(this, new LongPressListener());

        ShapeWear.setOnSizeChangeListener(new ShapeWear.OnSizeChangeListener() {
            @Override
            public void sizeDetected(int widthPx, int heightPx) {
                if(isRound) {
                    findViewById(R.id.rim).setBackground(getResources().getDrawable(R.drawable.color_border));
                } else {
                    findViewById(R.id.rim).setBackground(getResources().getDrawable(R.drawable.color_border_square));
                }

                FragAdapter =
                        new ZonesPagerAdapter(
                                getSupportFragmentManager());
                ZonePager = (ViewPager) findViewById(R.id.pager);
                ZonePager.setAdapter(FragAdapter);
                ZonePager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
                    @Override
                    public void onPageScrolled(int i, float v, int i2) {

                    }

                    @Override
                    public void onPageSelected(int i) {
                        if(FragAdapter.isColor(i)) {
                            if(isRound) {
                                findViewById(R.id.rim).setBackground(getResources().getDrawable(R.drawable.color_border));
                            } else {
                                findViewById(R.id.rim).setBackground(getResources().getDrawable(R.drawable.color_border_square));
                            }
                        } else {
                            if(isRound) {
                                findViewById(R.id.rim).setBackground(getResources().getDrawable(R.drawable.white_border));
                            } else {
                                findViewById(R.id.rim).setBackground(getResources().getDrawable(R.drawable.white_border_square));
                            }
                        }
                    }

                    @Override
                    public void onPageScrollStateChanged(int i) {

                    }
                });
            }
        });
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        return mGestureDetector.onTouchEvent(event)
                || super.dispatchTouchEvent(event);
    }

    private class LongPressListener extends
            GestureDetector.SimpleOnGestureListener {
        @Override
        public void onLongPress(MotionEvent event) {
            mDismissOverlayView.show();

        }

    }
}

