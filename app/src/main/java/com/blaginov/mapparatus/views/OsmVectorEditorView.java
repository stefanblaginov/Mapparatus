package com.blaginov.mapparatus.views;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import com.blaginov.mapparatus.R;
import com.blaginov.mapparatus.osm.BoundingBox;
import com.blaginov.mapparatus.osm.Node;
import com.blaginov.mapparatus.osm.Storage;
import com.blaginov.mapparatus.osm.Way;
import com.blaginov.mapparatus.util.GeoMath;

import org.osmdroid.views.MapView;

import java.util.List;

/**
 * Created by stefanblag on 20.04.15.
 */
public class OsmVectorEditorView extends View {

    private MapView mapView;
    private BoundingBox viewBox;
    private Storage storage;
    private List<Node> nodes;

    private int panX;
    private int panY;

    private float zoomFactor = 1;
    private int zoomCenterX;
    private int zoomCenterY;

    private boolean dragging = false;
    private boolean tapping = false;
    private boolean movingNode = false;
    private float drawX;
    private float drawY;
    private final double SELECTING_TOUCH_RADIUS_SQRD = Math.pow(15, 2);

    private long currentMovingNodeId;

    private Bitmap magnicursor;
    private float magnicursorOffsetX;
    private final float MAGNICURSOR_OFFSET_Y = -380;
    private final float DRAW_OFFSET_Y = -260;

    public OsmVectorEditorView(Context context) { super(context); }

    public OsmVectorEditorView(Context context, AttributeSet attrs) { super(context, attrs); }

    public OsmVectorEditorView(Context context, AttributeSet attrs, int defStyleAttr) { super(context, attrs, defStyleAttr); }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        Resources resources = getResources();
        magnicursor = BitmapFactory.decodeResource(resources, R.drawable.magnicursor);
        magnicursorOffsetX = -(magnicursor.getWidth() / 2) - 2;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (viewBox != null) {
            zoomCenterX = canvas.getWidth() / 2;
            zoomCenterY = canvas.getHeight() / 2;

            Paint nodePaint = new Paint();
            nodePaint.setARGB(255, 50, 50, 255);

            Paint highlightedNodePaint = new Paint();
            highlightedNodePaint.setARGB(255, 100, 100, 255);

            if (storage != null && !storage.isEmpty()) {
                for (Way way : storage.getWays()) {
                    List<Node> currentWayNodes = way.getNodes();
                    for (int i = 1; i < currentWayNodes.size(); i++) {
                        canvas.drawLine(
                                zoom((GeoMath.lonE7ToX(getWidth(), viewBox, currentWayNodes.get(i - 1).getLon()) - panX), zoomFactor, zoomCenterX),
                                zoom((GeoMath.latE7ToY(getHeight(), getWidth(), viewBox, currentWayNodes.get(i - 1).getLat()) - panY), zoomFactor, zoomCenterY),
                                zoom((GeoMath.lonE7ToX(getWidth(), viewBox, currentWayNodes.get(i).getLon()) - panX), zoomFactor, zoomCenterX),
                                zoom((GeoMath.latE7ToY(getHeight(), getWidth(), viewBox, currentWayNodes.get(i).getLat()) - panY), zoomFactor, zoomCenterY),
                                nodePaint);
                    }
                }

                if (!tapping) {
                    movingNode = false;
                    currentMovingNodeId = -1;
                }

                for (Node node : storage.getNodes()) {
                    float currentNodeX = zoom((GeoMath.lonE7ToX(getWidth(), viewBox, node.getLon()) - panX), zoomFactor, zoomCenterX);
                    float currentNodeY = zoom((GeoMath.latE7ToY(getHeight(), getWidth(), viewBox, node.getLat()) - panY), zoomFactor, zoomCenterY);

                    canvas.drawCircle(currentNodeX, currentNodeY, 10, nodePaint);

                    if (tapping) {
                        if (movingNode) {
                            storage.getNode(currentMovingNodeId).setLon(GeoMath.xToLonE7(getWidth(), viewBox, unzoom(drawX, zoomFactor, zoomCenterX) + panX));
                            storage.getNode(currentMovingNodeId).setLat(GeoMath.yToLatE7(getHeight(), getWidth(), viewBox, unzoom(drawY + DRAW_OFFSET_Y, zoomFactor, zoomCenterY) + panY));
                            canvas.drawCircle(drawX, drawY + DRAW_OFFSET_Y, 30, highlightedNodePaint);
                        } else if (Math.pow(drawX - currentNodeX, 2) + Math.pow(drawY + DRAW_OFFSET_Y - currentNodeY, 2) < SELECTING_TOUCH_RADIUS_SQRD) {
                            movingNode = true;
                            currentMovingNodeId = node.getOsmId();
                            node.updateState(Node.STATE_MODIFIED);
                            canvas.drawCircle(drawX, drawY + DRAW_OFFSET_Y, 30, highlightedNodePaint);
                        }
                    } else if (dragging && Math.pow(drawX - currentNodeX, 2) + Math.pow(drawY + DRAW_OFFSET_Y - currentNodeY, 2) < SELECTING_TOUCH_RADIUS_SQRD) {
                        canvas.drawCircle(currentNodeX, currentNodeY, 30, nodePaint);
                    }
                }

            }

            if (dragging || tapping) {
                canvas.drawBitmap(magnicursor, drawX + magnicursorOffsetX, drawY + MAGNICURSOR_OFFSET_Y, nodePaint);
            }
        }
        invalidate();
    }

    public void setViewBox(BoundingBox viewBox) {
        this.viewBox = viewBox;
    }

    public void setStorage(Storage storage) {
        this.storage = storage;
        nodes = storage.getNodes();

    }

    public void setPanCoords(int panX, int panY) {
        this.panX = panX;
        this.panY = panY;
    }

    public void setZoomFactor(float zoomFactor) {
        this.zoomFactor = zoomFactor;
    }

    public void setDragStatus(boolean dragging) {
        this.dragging = dragging;
    }

    public void setTapStatus(boolean tapping) {
        this.tapping = tapping;
    }

    public void setDrawingCoords(float drawX, float drawY) {
        this.drawX = drawX;
        this.drawY = drawY;
    }

    private float zoom(float point, float factor, int pointOfZoom) {
        return ((point - pointOfZoom) * factor) + pointOfZoom;
    }

    private float unzoom(float point, float factor, int pointOfZoom) {
        return ((point - pointOfZoom) / factor) + pointOfZoom;
    }
}
