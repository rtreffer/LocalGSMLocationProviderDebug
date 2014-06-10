package de.measite.gsmlocationdebug;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;
import android.os.DeadObjectException;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import org.gfd.gsmlocation.BackendDebug;
import org.gfd.gsmlocation.model.CellInfo;
import org.osmdroid.util.BoundingBoxE6;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.ItemizedIconOverlay;
import org.osmdroid.views.overlay.ItemizedOverlay;
import org.osmdroid.views.overlay.OverlayItem;
import org.osmdroid.views.overlay.OverlayManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class MainActivity extends Activity {

    public final static String TAG = "de.measite.gsmlocationdebug.UI";

    private MapView mapView = null;
    private TextView towersText = null;
    private TextView areaText = null;
    private TextView countryText = null;

    private Runnable bind = new Runnable() {
        @Override
        public void run() {
            Intent intent = new Intent()
                .setComponent(new ComponentName(
                        "org.gfd.gsmlocation",
                        "org.gfd.gsmlocation.BackendDebugService"));
            bindService(intent, new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName name, IBinder service) {
                    Log.d(TAG, "BackendDebugService bound");
                    backendDebug = BackendDebug.Stub.asInterface(service);
                    if (uiUpdateThread.isAlive()) {
                        uiUpdateThread.interrupt();
                        try {
                            uiUpdateThread.join();
                        } catch (InterruptedException e) {
                        }
                    }
                    try {
                        uiUpdateThread.start();
                    } catch (IllegalThreadStateException e) {
                    }
                }

                @Override
                public void onServiceDisconnected(ComponentName name) {
                    uiUpdateThread.interrupt();
                }
            }, Context.BIND_AUTO_CREATE);
        }

    };
    private BackendDebug backendDebug;
    private Handler handler = new Handler();
    private Thread uiUpdateThread = new Thread() {
        @Override
        public void run() {
            try {
                while (true) {
                    try {
                        Thread.sleep(300);
                        final CellInfo[] cellInfos = backendDebug.getUsedCells();
                        final CellInfo[] unusedInfos = backendDebug.getUnusedCells();
                        final HashSet<Integer> lacs = new HashSet<Integer>();
                        final HashSet<Integer> mccs = new HashSet<Integer>();
                        for (CellInfo i : cellInfos) {
                            if (i.LAC > 0) {
                                lacs.add(i.LAC);
                            }
                            if (i.MCC > 0) {
                                mccs.add(i.MCC);
                            }
                        }
                        for (CellInfo i : unusedInfos) {
                            if (i.LAC > 0) {
                                lacs.add(i.LAC);
                            }
                            if (i.MCC > 0) {
                                mccs.add(i.MCC);
                            }
                        }
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                if (towersText != null) {
                                    if (unusedInfos != null) {
                                        towersText.setText("Towers: " + cellInfos.length + "\n" +
                                                "Unused: " + ((unusedInfos.length + 1) / 2));
                                        towersText.setLines(2);
                                        towersText.invalidate();
                                    } else {
                                        towersText.setText("Unused: " + ((unusedInfos.length + 1) / 2));
                                        towersText.setLines(1);
                                        towersText.invalidate();
                                    }
                                } else if (unusedInfos != null) {
                                    towersText.setText("Unused: " + ((unusedInfos.length + 1) / 2));
                                    towersText.setLines(1);
                                    towersText.invalidate();
                                }

                                if (countryText != null) {
                                    countryText.setText("Countries " + mccs.size());
                                }

                                if (areaText != null) {
                                    areaText.setText("Areas " + lacs.size());
                                }

                                if (cellInfos.length == 0) {
                                    if (mapView != null) {
                                        if (mapView.getOverlayManager().size() > 0) {
                                            mapView.getOverlayManager().remove(0);
                                            mapView.invalidate();
                                        }
                                    }
                                    return;
                                }

                                double minLat = cellInfos[0].lat;
                                double maxLat = minLat;
                                double minLng = cellInfos[0].lng;
                                double maxLng = minLng;
                                for (CellInfo cellInfo : cellInfos) {
                                    minLat = Math.min(minLat, cellInfo.lat);
                                    maxLat = Math.max(maxLat, cellInfo.lat);
                                    minLng = Math.min(minLng, cellInfo.lng);
                                    maxLng = Math.max(maxLng, cellInfo.lng);
                                }

                                double dLat = Math.max((maxLat - minLat) * 0.1, 0.1 / Math.pow(1.75, towersText.length() - 1));
                                double dLng = Math.max((maxLng - minLng) * 0.1, 0.1 / Math.pow(1.75, towersText.length() - 1));

                                BoundingBoxE6 bbe6 = new BoundingBoxE6(
                                        minLat - dLat,
                                        maxLng + dLng,
                                        maxLat + dLat,
                                        minLng - dLng
                                );

                                if (mapView != null) {
                                    OverlayManager overlayManager = mapView.getOverlayManager();
                                    List<OverlayItem> items = new ArrayList<OverlayItem>(cellInfos.length + 1);
                                    for (CellInfo info : cellInfos) {
                                        items.add(new OverlayItem(info.toString(), info.toString(), new GeoPoint(info.lat, info.lng)));
                                    }
                                    ItemizedOverlay<OverlayItem> itemizedOverlay =
                                            new ItemizedIconOverlay<OverlayItem>(getBaseContext(), items, null);
                                    if (overlayManager.size() == 0) {
                                        overlayManager.add(itemizedOverlay);
                                    } else {
                                        overlayManager.set(0, itemizedOverlay);
                                    }

                                    mapView.zoomToBoundingBox(bbe6);
                                    mapView.invalidate();
                                }
                            }
                        });
                    } catch (Exception e) {
                        if (e instanceof InterruptedException) {
                            throw (InterruptedException)e;
                        }
                        if (e instanceof DeadObjectException) {
                            handler.post(bind);
                            break;
                        }
                        e.printStackTrace();
                    }
                }
            } catch (InterruptedException e) {
                // should be "by request"
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mapView = (MapView)findViewById(R.id.mapview);
        mapView.setBuiltInZoomControls(false);
        mapView.setMinZoomLevel(2);
        mapView.zoomToBoundingBox(mapView.getBoundingBox().increaseByScale(-0.5f));
        mapView.setMultiTouchControls(true);
        mapView.invalidate();

        towersText = (TextView) findViewById(R.id.towerText);
        towersText.setBackgroundColor(Color.argb(160, 255, 255, 255));

        areaText = (TextView) findViewById(R.id.areaText);
        areaText.setBackgroundColor(Color.argb(160, 255, 255, 255));

        countryText = (TextView) findViewById(R.id.countryText);
        countryText.setBackgroundColor(Color.argb(160, 255, 255, 255));

        handler.post(new Runnable() {
           @Override
            public void run() {
                mapView.invalidate();
           }
        });
    }

    @Override
    public void onStop() {
        if (uiUpdateThread.isAlive()) uiUpdateThread.interrupt();
        super.onStop();
    }

    @Override
    public void onStart() {
        super.onStart();
        handler.post(bind);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
