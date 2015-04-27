package com.blaginov.mapparatus.views;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.view.View;

import com.blaginov.mapparatus.R;
import com.blaginov.mapparatus.exception.OsmIllegalOperationException;
import com.blaginov.mapparatus.osm.BoundingBox;
import com.blaginov.mapparatus.osm.Node;
import com.blaginov.mapparatus.osm.Server;
import com.blaginov.mapparatus.osm.Storage;
import com.blaginov.mapparatus.osm.StorageDelegator;
import com.blaginov.mapparatus.osm.Way;
import com.blaginov.mapparatus.util.GeoMath;

import java.io.IOException;
import java.util.List;

/**
 * Created by stefanblag on 20.04.15.
 */
public class OsmVectorEditorView extends View {

    private enum EditingState { CAN_DRAW, ADDING_NODE, MOVING_NODE, DRAWING_WAY, CAN_REMOVE }

    private BoundingBox viewBox;
    private StorageDelegator storageDelegator;
    private Server server;
    private EditingState currentState = EditingState.CAN_DRAW;

    private int panX;
    private int panY;

    private float zoomFactor = 1;
    private int zoomCenterX;
    private int zoomCenterY;

    private boolean dragging = false;
    private boolean tapping = false;
    private boolean longTapUpFlag = false;
    private boolean nodePlacingLimit = false;
    private boolean nodeRemovalLimit = false;
    private boolean tapUpFlag = false;
    private float drawX;
    private float drawY;
    private final double SELECTING_TOUCH_RADIUS_SQRD = Math.pow(15, 2);
    private boolean cursorOnNode = false;

    private long nodeCursorIsCurrentlyOnId;
    private long lastAssignedId = -1;

    private boolean addElement = false;
    private boolean experiment = false;
    private Way currentWayDrawn = null;

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

        storageDelegator = new StorageDelegator();

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

            Paint wayPaint = new Paint();
            wayPaint.setARGB(255, 50, 50, 255);
            wayPaint.setStrokeWidth(10);


            Paint highlightedNodePaint = new Paint();
            highlightedNodePaint.setARGB(100, 100, 100, 255);

            Paint createdNodePaint = new Paint();
            createdNodePaint.setARGB(100, 100, 255, 100);

            if (storageDelegator != null && !storageDelegator.getCurrentStorage().isEmpty() && !experiment) {
                for (Way way : storageDelegator.getCurrentStorage().getWays()) {
                    List<Node> currentWayNodes = way.getNodes();
                    for (int i = 1; i < currentWayNodes.size(); i++) {
                        canvas.drawLine(
                                zoom((GeoMath.lonE7ToX(getWidth(), viewBox, currentWayNodes.get(i - 1).getLon()) - panX), zoomFactor, zoomCenterX),
                                zoom((GeoMath.latE7ToY(getHeight(), getWidth(), viewBox, currentWayNodes.get(i - 1).getLat()) - panY), zoomFactor, zoomCenterY),
                                zoom((GeoMath.lonE7ToX(getWidth(), viewBox, currentWayNodes.get(i).getLon()) - panX), zoomFactor, zoomCenterX),
                                zoom((GeoMath.latE7ToY(getHeight(), getWidth(), viewBox, currentWayNodes.get(i).getLat()) - panY), zoomFactor, zoomCenterY),
                                wayPaint);
                    }
                }

                if (currentState != EditingState.CAN_REMOVE) {
                    // Used to limit one tap to one added node
                    if (!tapping) {
                        nodePlacingLimit = false;
                        //currentState = EditingState.CAN_DRAW;
                    }

                    if (currentState == EditingState.MOVING_NODE && !tapping) {
                        currentState = EditingState.CAN_DRAW;
                    }

                    if (currentState == EditingState.DRAWING_WAY && !dragging) {
                        currentState = EditingState.CAN_DRAW;
                        currentWayDrawn = null;
                    }
                } else {
                    if (!tapping) {
                        nodeRemovalLimit = false;
                    }
                }

                for (Node node : storageDelegator.getCurrentStorage().getNodes()) {
                    float currentNodeX = zoom((GeoMath.lonE7ToX(getWidth(), viewBox, node.getLon()) - panX), zoomFactor, zoomCenterX);
                    float currentNodeY = zoom((GeoMath.latE7ToY(getHeight(), getWidth(), viewBox, node.getLat()) - panY), zoomFactor, zoomCenterY);

                    if (dragging && Math.pow(drawX - currentNodeX, 2) + Math.pow(drawY + DRAW_OFFSET_Y - currentNodeY, 2) < SELECTING_TOUCH_RADIUS_SQRD) {
                        cursorOnNode = true;
                        nodeCursorIsCurrentlyOnId = node.getOsmId();
                        canvas.drawCircle(currentNodeX, currentNodeY, 30, nodePaint);
                    } else {
                        canvas.drawCircle(currentNodeX, currentNodeY, 10, nodePaint);
                    }
                }

                switch (currentState) {
                    case DRAWING_WAY:
                        if (tapUpFlag) {
                            nodePlacingLimit = true;
                            Node nodeToInsert = new Node(lastAssignedId, 1, Node.STATE_CREATED,
                                    GeoMath.yToLatE7(getHeight(), getWidth(), viewBox, unzoom(drawY + DRAW_OFFSET_Y, zoomFactor, zoomCenterY) + panY),
                                    GeoMath.xToLonE7(getWidth(), viewBox, unzoom(drawX, zoomFactor, zoomCenterX) + panX));
                                lastAssignedId--;
                                storageDelegator.insertElementSafe(nodeToInsert);
                            try {
                                storageDelegator.addNodeToWay(nodeToInsert, currentWayDrawn);
                            } catch (OsmIllegalOperationException e) {
                                e.printStackTrace();
                            }
                            storageDelegator.getUndo().createCheckpoint("Placed first node of way");
                            lastAssignedId--;
                            canvas.drawCircle(drawX, drawY + DRAW_OFFSET_Y, 30, createdNodePaint);
                        }
                        break;
                    case ADDING_NODE:
                        if (tapUpFlag) {
                            currentState = EditingState.CAN_DRAW;
                            nodePlacingLimit = true;
                            storageDelegator.getUndo().createCheckpoint("Placed node");
                            storageDelegator.insertElementSafe(new Node(lastAssignedId, 1, Node.STATE_CREATED,
                                    GeoMath.yToLatE7(getHeight(), getWidth(), viewBox, unzoom(drawY + DRAW_OFFSET_Y, zoomFactor, zoomCenterY) + panY),
                                    GeoMath.xToLonE7(getWidth(), viewBox, unzoom(drawX, zoomFactor, zoomCenterX) + panX)));
                            lastAssignedId--;
                            canvas.drawCircle(drawX, drawY + DRAW_OFFSET_Y, 30, createdNodePaint);
                        } else if (longTapUpFlag) {
                            currentState = EditingState.DRAWING_WAY;
                            nodePlacingLimit = true;
                            storageDelegator.getUndo().createCheckpoint("Placed first node of way");
                            Node nodeToInsert = new Node(lastAssignedId, 1, Node.STATE_CREATED,
                                    GeoMath.yToLatE7(getHeight(), getWidth(), viewBox, unzoom(drawY + DRAW_OFFSET_Y, zoomFactor, zoomCenterY) + panY),
                                    GeoMath.xToLonE7(getWidth(), viewBox, unzoom(drawX, zoomFactor, zoomCenterX) + panX));
                            lastAssignedId--;
                            storageDelegator.insertElementSafe(nodeToInsert);
                            currentWayDrawn = storageDelegator.createAndInsertWay(nodeToInsert);
                        }
                        break;
                    case MOVING_NODE:
                        storageDelegator.updateLatLon(storageDelegator.getCurrentStorage().getNode(nodeCursorIsCurrentlyOnId),
                                GeoMath.yToLatE7(getHeight(), getWidth(), viewBox, unzoom(drawY + DRAW_OFFSET_Y, zoomFactor, zoomCenterY) + panY),
                                GeoMath.xToLonE7(getWidth(), viewBox, unzoom(drawX, zoomFactor, zoomCenterX) + panX));
                        canvas.drawCircle(drawX, drawY + DRAW_OFFSET_Y, 30, highlightedNodePaint);
                        break;
                    case CAN_REMOVE:
                        if (tapping && !nodeRemovalLimit && cursorOnNode) {
                            nodeRemovalLimit = true;
                            storageDelegator.removeNode(storageDelegator.getCurrentStorage().getNode(nodeCursorIsCurrentlyOnId));
                        }
                        break;
                    case CAN_DRAW:
                        if (tapping && !nodePlacingLimit && !cursorOnNode) {
                            currentState = EditingState.ADDING_NODE;
                        } else if (tapping && cursorOnNode) {
                            storageDelegator.getUndo().createCheckpoint("Moved node");
                            currentState = EditingState.MOVING_NODE;
                        }
                        break;
                }
            }

            /*
            if (experiment && storageDelegator.getUndo().canUndo()) {
                storageDelegator.getUndo().undo();
                experiment = false;
            }*/

            cursorOnNode = false;
            tapUpFlag = false;
            longTapUpFlag = false;

            if (dragging) {
                canvas.drawBitmap(magnicursor, drawX + magnicursorOffsetX, drawY + MAGNICURSOR_OFFSET_Y, nodePaint);
            }
        }
        invalidate();
    }

    public void setViewBox(BoundingBox viewBox) {
        this.viewBox = viewBox;
    }

    public void setStorage(Storage storage) {
        this.storageDelegator.setCurrentStorage(storage);
    }

    public StorageDelegator getStorageDelegator() {
        return this.storageDelegator;
    }

    public void setPanCoords(int panX, int panY) {
        this.panX = panX;
        this.panY = panY;
    }

    public void setServer(Server server) {
        this.server = server;
    }

    public void uploadChangesToServer() throws IOException {
        OsmDataUploader osmDataUploader = new OsmDataUploader(this.server, this.storageDelegator);
        osmDataUploader.execute();
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

    public void setTapUp() { this.tapUpFlag = true; }

    public void setLongTapUp() {
        this.longTapUpFlag = true;
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

    public void experiment() {
        //this.experiment = this.experiment ? false : true;
        currentState = currentState != EditingState.CAN_REMOVE ? EditingState.CAN_REMOVE : EditingState.CAN_DRAW;
    }

    class OsmDataUploader extends AsyncTask<Void, Void, Void> {
        private Server server;
        private StorageDelegator storageDelegator;

        public OsmDataUploader(Server server, StorageDelegator storageDelegator) {
            this.server = server;
            this.storageDelegator = storageDelegator;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

        }

        @Override
        protected Void doInBackground(Void... params) {
            try {
                storageDelegator.uploadToServer(server, "mechi mech", "El Mecho", true);
            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        }
    }
}
