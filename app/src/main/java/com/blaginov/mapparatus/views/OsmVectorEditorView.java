package com.blaginov.mapparatus.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;

import com.blaginov.mapparatus.exception.OsmException;
import com.blaginov.mapparatus.osm.BoundingBox;
import com.blaginov.mapparatus.util.GeoMath;

import org.osmdroid.util.BoundingBoxE6;
import org.osmdroid.views.MapView;

/**
 * Created by stefanblag on 20.04.15.
 */
public class OsmVectorEditorView extends View {

    MapView mapView;
    BoundingBox viewBox;

    private int panX = 0;
    private int panY = 0;

    private float zoomFactor = 1;
    private int zoomCenterX = 0;
    private int zoomCenterY = 0;

    private int editX = 0;
    private int editY = 0;

    int minlon = (int)(-1.482194*1E7);
    int minlat = (int)(53.379330*1E7);
    int maxlon = (int)(-1.478331*1E7);
    int maxlat = (int)(53.382907*1E7);

    int minlon2 = (int)(-1.484279*1E7);
    int minlat2 = (int)(53.378203*1E7);
    int maxlon2 = (int)(-1.474979*1E7);
    int maxlat2 = (int)(53.384103*1E7);



    double minlon6 = -1.482194*1E6;
    double minlat6 = 53.379330*1E6;
    double maxlon6 = -1.478331*1E6;
    double maxlat6 = 53.382907*1E6;

    int point1Lat = (int)(53.381238*1E7);
    int point1Lon = (int)(-1.480735*1E7);

    int point2Lat = (int)(53.381259*1E7);
    int point2Lon = (int)(-1.479987*1E7);

    int point3Lat = (int)(53.380850*1E7);
    int point3Lon = (int)(-1.479960*1E7);

    int point4Lat = (int)(53.380824*1E7);
    int point4Lon = (int)(-1.480653*1E7);

    public OsmVectorEditorView(Context context) {
        super(context);
    }

    public OsmVectorEditorView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public OsmVectorEditorView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attgit rs, defStyleAttr);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (viewBox != null) {
            zoomCenterX = canvas.getWidth() / 2;
            zoomCenterY = canvas.getHeight() / 2;


            if (mapView != null) {
                //mapView.zoomToBoundingBox(new BoundingBoxE6(maxlat6, maxlon6, minlat6, minlon6));
                //mapView.scrollTo((int) panX, (int) panY);
                //mapViewController.setCenter(new MyPoint());
            }

            Paint nodePaint = new Paint();
            nodePaint.setARGB(255, 50, 50, 255);

            canvas.drawCircle(zoom((int) (GeoMath.lonE7ToX(getWidth(), viewBox, GeoMath.xToLonE7(getWidth(), viewBox, editX)) - panX), zoomFactor, zoomCenterX), zoom((int) (GeoMath.latE7ToY(getHeight(), getWidth(), viewBox, GeoMath.yToLatE7(getHeight(), getWidth(), viewBox, editY)) - panY), zoomFactor, zoomCenterY), 60, nodePaint);

            canvas.drawCircle(zoom((int) (GeoMath.lonE7ToX(getWidth(), viewBox, point1Lon) - panX), zoomFactor, zoomCenterX), zoom((int) (GeoMath.latE7ToY(getHeight(), getWidth(), viewBox, point1Lat) - panY), zoomFactor, zoomCenterY), 10, nodePaint);
            canvas.drawCircle(zoom((int) (GeoMath.lonE7ToX(getWidth(), viewBox, point2Lon) - panX), zoomFactor, zoomCenterX), zoom((int) (GeoMath.latE7ToY(getHeight(), getWidth(), viewBox, point2Lat) - panY), zoomFactor, zoomCenterY), 20, nodePaint);
            canvas.drawCircle(zoom((int) (GeoMath.lonE7ToX(getWidth(), viewBox, point3Lon) - panX), zoomFactor, zoomCenterX), zoom((int) (GeoMath.latE7ToY(getHeight(), getWidth(), viewBox, point3Lat) - panY), zoomFactor, zoomCenterY), 30, nodePaint);
            canvas.drawCircle(zoom((int) (GeoMath.lonE7ToX(getWidth(), viewBox, point4Lon) - panX), zoomFactor, zoomCenterX), zoom((int) (GeoMath.latE7ToY(getHeight(), getWidth(), viewBox, point4Lat) - panY), zoomFactor, zoomCenterY), 40, nodePaint);


        }
        invalidate();
    }

    public void setViewBox(BoundingBox viewBox) {
        this.viewBox = viewBox;
    }

    public void setPanCoords(int panX, int panY) {
        this.panX = panX;
        this.panY = panY;
    }

    public void setZoomFactor(float zoomFactor) {
        this.zoomFactor = zoomFactor;
    }

    public void setEditPointCoords(int editX, int editY) {
        this.editX = editX;
        this.editY = editY;
    }

    private float zoom(int point, float factor, int pointOfZoom) {
        return (int) ((point - pointOfZoom) * factor) + pointOfZoom;
    }
}
