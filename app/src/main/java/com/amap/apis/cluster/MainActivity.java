package com.amap.apis.cluster;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.Toast;

import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.MapView;
import com.amap.api.maps.model.CameraPosition;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.LatLngBounds;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.animation.AlphaAnimation;
import com.amap.apis.cluster.demo.RegionItem;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity implements AMap.OnMapLoadedListener {

    private MapView mMapView;
    private AMap mAMap;

    private int clusterRadius = 100;
    private ClusterOverlay mClusterOverlay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mMapView = (MapView) findViewById(R.id.map);
        mMapView.onCreate(savedInstanceState);
        init();

        findViewById(R.id.tv_add).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });
        findViewById(R.id.tv_clear).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mClusterOverlay.clearMarker();
            }
        });
        findViewById(R.id.tv_get_all).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (mClusterOverlay != null) {
                    Toast.makeText(MainActivity.this, "count=" + normalMarker.size(), Toast.LENGTH_LONG).show();
                }

            }
        });


    }

    public ClusterClickListener mClusterClickListener;

    private void init() {
        mClusterClickListener = new ClusterClickListener() {
            @Override
            public void onClick(Marker marker, List<ClusterItem> clusterItems) {
                LatLngBounds.Builder builder = new LatLngBounds.Builder();
                for (ClusterItem clusterItem : clusterItems) {
                    builder.include(clusterItem.getPosition());
                }
                LatLngBounds latLngBounds = builder.build();
                mAMap.animateCamera(CameraUpdateFactory.newLatLngBounds(latLngBounds, 0));

            }
        };
        if (mAMap == null) {
            // 初始化地图
            mAMap = mMapView.getMap();
            mAMap.setOnMapLoadedListener(this);
            //点击可以动态添加点
            mAMap.setOnMapClickListener(new AMap.OnMapClickListener() {
                @Override
                public void onMapClick(LatLng latLng) {
                    double lat = Math.random() + 39.474923;
                    double lon = Math.random() + 116.027116;

                    LatLng latLng1 = new LatLng(lat, lon, false);
                    RegionItem regionItem = new RegionItem(latLng1,
                            "test");
                    mClusterOverlay.addClusterItem(regionItem);

                }
            });

            mAMap.setOnMarkerClickListener(new AMap.OnMarkerClickListener() {
                @Override
                public boolean onMarkerClick(Marker marker) {
                    if (markerStatus != MARKER_Cluster) {
                        Toast.makeText(MainActivity.this, "clickNormalMaker", Toast.LENGTH_SHORT).show();
                        return true;
                    } else {
                        if (mClusterClickListener == null) {
                            return true;
                        }
                        Cluster cluster = (Cluster) marker.getObject();
                        if (cluster != null) {

                            if (cluster.getClusterItems().size() == 1) {
                                Toast.makeText(MainActivity.this, "signle", Toast.LENGTH_SHORT).show();
                                return true;
                            }

                            mClusterClickListener.onClick(marker, cluster.getClusterItems());
                            return true;
                        }
                        return false;
                    }
                }
            });

            mAMap.setOnCameraChangeListener(new AMap.OnCameraChangeListener() {
                @Override
                public void onCameraChange(CameraPosition cameraPosition) {

                }

                @Override
                public void onCameraChangeFinish(CameraPosition cameraPosition) {

                    if (cameraPosition.zoom < 11) {

                        markerStatus = MARKER_Cluster;

                        if (normalMarker != null && normalMarker.size() > 0) {
                            for (int i = 0; i < normalMarker.size(); i++) {
                                Marker marker = normalMarker.get(i);
                                marker.remove();
                                marker = null;
                            }
                        }

                        normalMarker.clear();

                        mClusterOverlay.mPXInMeters = mAMap.getScalePerPixel();
                        mClusterOverlay.mClusterDistance = mClusterOverlay.mPXInMeters * mClusterOverlay.mClusterSize;
                        mClusterOverlay.assignClusters();
                    } else {
                        if (markerStatus == MARKER_Cluster) {
                            markerStatus = MARKER_NORMA;
                            mClusterOverlay.clearMarker();

                            List<ClusterItem> items = initDatas();

                            for (ClusterItem clusterItem : items) {
                                MarkerOptions markerOptions = new MarkerOptions();
                                markerOptions.anchor(0.5f, 0.5f)
                                        .icon(mClusterOverlay.getBitmapDes(1)).position(clusterItem.getPosition());
                                Marker marker = mAMap.addMarker(markerOptions);
                                marker.setAnimation(mADDAnimation);

                                marker.startAnimation();
                                normalMarker.add(marker);
                            }

                        }
                    }

                }
            });


        }
    }

    List<Marker> normalMarker = new ArrayList<>();

    public int markerStatus = 1;

    public final int MARKER_NORMA = 1;
    public final int MARKER_Cluster = 2;

    private AlphaAnimation mADDAnimation = new AlphaAnimation(0, 1);


    protected void onResume() {
        super.onResume();
        mMapView.onResume();
    }

    protected void onPause() {
        super.onPause();
        mMapView.onPause();
    }

    protected void onDestroy() {
        super.onDestroy();
        //销毁资源
        mClusterOverlay.onDestroy();
        mMapView.onDestroy();
    }

    @Override
    public void onMapLoaded() {
        //添加测试数据
        new Thread() {
            public void run() {

                List<ClusterItem> items = initDatas();
                mClusterOverlay = new ClusterOverlay(mAMap, items, dp2px(getApplicationContext(), clusterRadius), getApplicationContext());

//                mClusterOverlay.setClusterRenderer(MainActivity.this);

//                mClusterOverlay.setOnClusterClickListener(MainActivity.this);


            }

        }.start();


    }

    @NonNull
    private List<ClusterItem> initDatas() {
        List<ClusterItem> items = new ArrayList<ClusterItem>();

        //随机10000个点
        for (int i = 0; i < 300; i++) {

            double lat = Math.random() + 39.474923;
            double lon = Math.random() + 116.027116;

            LatLng latLng = new LatLng(lat, lon, false);
            RegionItem regionItem = new RegionItem(latLng,
                    "test" + i);
            items.add(regionItem);

        }
        return items;
    }


    /**
     * 根据手机的分辨率从 dp 的单位 转成为 px(像素)
     */
    public int dp2px(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }


}
