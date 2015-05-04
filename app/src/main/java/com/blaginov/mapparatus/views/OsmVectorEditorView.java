package com.blaginov.mapparatus.views;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.blaginov.mapparatus.R;
import com.blaginov.mapparatus.exception.OsmIllegalOperationException;
import com.blaginov.mapparatus.osm.BoundingBox;
import com.blaginov.mapparatus.osm.Node;
import com.blaginov.mapparatus.osm.Server;
import com.blaginov.mapparatus.osm.Storage;
import com.blaginov.mapparatus.osm.StorageDelegator;
import com.blaginov.mapparatus.osm.Way;
import com.blaginov.mapparatus.util.GeoMath;
import com.cocosw.bottomsheet.BottomSheet;

import org.osmdroid.ResourceProxy;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Created by stefanblag on 20.04.15.
 */
public class OsmVectorEditorView extends View {

    private enum EditingState { CAN_DRAW, ADDING_NODE, MOVING_NODE, DRAWING_WAY, CAN_REMOVE }

    private BoundingBox viewBox;
    private StorageDelegator storageDelegator;
    private Server server;
    private EditingState currentState = EditingState.CAN_DRAW;
    private Map<String, String> currentlyEditedKvps = new TreeMap<>();
    private Context mainContext;

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
    private boolean cursorOnWay = false;

    private long nodeCursorIsCurrentlyOnId;
    private long wayCursorIsCurrentlyOnId;
    private long elementCursorIsCurrentlyOnId;
    private long lastAssignedId = -1;

    private boolean addElement = false;
    private boolean isNodeKvpEdited = false;
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

            Paint highlightedWayPaint = new Paint();
            highlightedWayPaint.setARGB(255, 50, 50, 255);
            highlightedWayPaint.setStrokeWidth(30);


            Paint highlightedNodePaint = new Paint();
            highlightedNodePaint.setARGB(100, 100, 100, 255);

            Paint createdNodePaint = new Paint();
            createdNodePaint.setARGB(100, 100, 255, 100);

            if (storageDelegator != null && !storageDelegator.getCurrentStorage().isEmpty()) {
                boolean localCursorOnWay = false;
                for (Way way : storageDelegator.getCurrentStorage().getWays()) {
                    Paint currentWayPaint;
                    if (cursorOnWay && way.getOsmId() == wayCursorIsCurrentlyOnId) {
                        currentWayPaint = highlightedWayPaint;
                    } else {
                        currentWayPaint = wayPaint;
                    }
                    List<Node> currentWayNodes = way.getNodes();

                    for (int i = 1; i < currentWayNodes.size(); i++) {
                        float currentNodeAX = zoom((GeoMath.lonE7ToX(getWidth(), viewBox, currentWayNodes.get(i - 1).getLon()) - panX), zoomFactor, zoomCenterX);
                        float currentNodeAY = zoom((GeoMath.latE7ToY(getHeight(), getWidth(), viewBox, currentWayNodes.get(i - 1).getLat()) - panY), zoomFactor, zoomCenterY);
                        float currentNodeBX = zoom((GeoMath.lonE7ToX(getWidth(), viewBox, currentWayNodes.get(i).getLon()) - panX), zoomFactor, zoomCenterX);
                        float currentNodeBY = zoom((GeoMath.latE7ToY(getHeight(), getWidth(), viewBox, currentWayNodes.get(i).getLat()) - panY), zoomFactor, zoomCenterY);

                        if (dragging && Math.sqrt(Math.pow(drawX - currentNodeAX, 2) + Math.pow(drawY + DRAW_OFFSET_Y - currentNodeAY, 2))
                                + Math.sqrt(Math.pow(drawX - currentNodeBX, 2) + Math.pow(drawY + DRAW_OFFSET_Y - currentNodeBY, 2))
                                <= Math.sqrt(Math.pow(currentNodeAX - currentNodeBX, 2) + Math.pow(currentNodeAY - currentNodeBY, 2)) + 10) {
                            localCursorOnWay = true;
                            wayCursorIsCurrentlyOnId = elementCursorIsCurrentlyOnId = way.getOsmId();
                        }
                        canvas.drawLine(currentNodeAX, currentNodeAY, currentNodeBX, currentNodeBY, currentWayPaint);
                    }
                }
                cursorOnWay = localCursorOnWay;

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

                boolean localCursorOnNode = false;
                for (Node node : storageDelegator.getCurrentStorage().getNodes()) {
                    float currentNodeX = zoom((GeoMath.lonE7ToX(getWidth(), viewBox, node.getLon()) - panX), zoomFactor, zoomCenterX);
                    float currentNodeY = zoom((GeoMath.latE7ToY(getHeight(), getWidth(), viewBox, node.getLat()) - panY), zoomFactor, zoomCenterY);

                    if (dragging && Math.pow(drawX - currentNodeX, 2) + Math.pow(drawY + DRAW_OFFSET_Y - currentNodeY, 2) < SELECTING_TOUCH_RADIUS_SQRD) {
                        localCursorOnNode = true;
                        nodeCursorIsCurrentlyOnId = elementCursorIsCurrentlyOnId = node.getOsmId();
                        canvas.drawCircle(currentNodeX, currentNodeY, 30, nodePaint);
                    } else {
                        canvas.drawCircle(currentNodeX, currentNodeY, 10, nodePaint);
                    }
                }
                cursorOnNode = localCursorOnNode;

                switch (currentState) {
                    case DRAWING_WAY:
                        if (tapUpFlag) {
                            nodePlacingLimit = true;
                            if (cursorOnNode && nodeCursorIsCurrentlyOnId == currentWayDrawn.getFirstNode().getOsmId()) {
                                try {
                                    storageDelegator.addNodeToWay(currentWayDrawn.getFirstNode(), currentWayDrawn);
                                } catch (OsmIllegalOperationException e) {
                                    e.printStackTrace();
                                }
                                storageDelegator.getUndo().createCheckpoint("Polygon completed");
                                canvas.drawCircle(drawX, drawY + DRAW_OFFSET_Y, 30, createdNodePaint);
                            } else if (!cursorOnNode) {
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

    public void toggleDeleteMode() {
        currentState = currentState != EditingState.CAN_REMOVE ? EditingState.CAN_REMOVE : EditingState.CAN_DRAW;
    }

    public void triggerUndo() {
        if (storageDelegator.getUndo().canUndo()) {
            storageDelegator.getUndo().undo();
        }
    }

    public void setContext(Context mainContext) {
        this.mainContext = mainContext;
    }

    public void openKvpMenu() {
        if (cursorOnNode || cursorOnWay) {
            BottomSheet.Builder kvpMenu = new BottomSheet.Builder(mainContext, R.style.BottomSheet_Dialog);
            kvpMenu.title("Tags");
            Set kvpsSet;

            if (cursorOnNode) {
                kvpsSet = storageDelegator.getCurrentStorage().getNode(nodeCursorIsCurrentlyOnId).getTags().entrySet();
                isNodeKvpEdited = true;
            } else {
                kvpsSet = storageDelegator.getCurrentStorage().getWay(wayCursorIsCurrentlyOnId).getTags().entrySet();
                isNodeKvpEdited = false;
            }

            Iterator kvpsSetIterator = kvpsSet.iterator();
            kvpMenu.sheet(0, "Add tag");
            int menuId = 1;
            while (kvpsSetIterator.hasNext()) {
                Map.Entry m = (Map.Entry) kvpsSetIterator.next();
                this.currentlyEditedKvps.put(m.getKey().toString(), m.getValue().toString());
                kvpMenu.sheet(menuId, m.getKey() + " - " + m.getValue());
                menuId++;
            }


            kvpMenu.listener(new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (which == 0) {
                        addKVP();
                    } else {
                        Set kvpsSet = currentlyEditedKvps.entrySet();
                        Iterator kvpsSetIterator = kvpsSet.iterator();
                        int menuId = 1;
                        while (kvpsSetIterator.hasNext() && menuId <= which) {
                            Map.Entry m = (Map.Entry) kvpsSetIterator.next();
                            if (menuId == which) {
                                editKVP(m.getKey().toString());
                            }
                            menuId++;
                        }
                    }
                }
            });

            kvpMenu.show();
        }
    }

    private void editKVP(final String key) {
        AlertDialog.Builder alert = new AlertDialog.Builder(mainContext);

        alert.setTitle("Edit tag");
        LinearLayout layout = new LinearLayout(mainContext);
        layout.setOrientation(LinearLayout.VERTICAL);

        // Set an EditText view to get user input
        final EditText keyInput = new EditText(mainContext);
        final EditText valueInput = new EditText(mainContext);
        keyInput.setText(key);
        valueInput.setText(currentlyEditedKvps.get(key).toString());
        layout.addView(keyInput);
        layout.addView(valueInput);
        alert.setView(layout);

        alert.setPositiveButton("Change", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                currentlyEditedKvps.remove(key);
                currentlyEditedKvps.put(keyInput.getText().toString(), valueInput.getText().toString());
                if (isNodeKvpEdited) {
                    storageDelegator.setTags(storageDelegator.getCurrentStorage().getNode(nodeCursorIsCurrentlyOnId), currentlyEditedKvps);
                } else {
                    storageDelegator.setTags(storageDelegator.getCurrentStorage().getWay(wayCursorIsCurrentlyOnId), currentlyEditedKvps);
                }

                storageDelegator.getUndo().createCheckpoint("Changed tag");
            }
        });

        alert.setNegativeButton("Delete", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                currentlyEditedKvps.remove(key);
                if (isNodeKvpEdited) {
                    storageDelegator.setTags(storageDelegator.getCurrentStorage().getNode(nodeCursorIsCurrentlyOnId), currentlyEditedKvps);
                } else {
                    storageDelegator.setTags(storageDelegator.getCurrentStorage().getWay(wayCursorIsCurrentlyOnId), currentlyEditedKvps);
                }

                storageDelegator.getUndo().createCheckpoint("Deleted tag");
            }
        });

        alert.setNeutralButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
            }
        });

        alert.show();
    }

    private void addKVP() {
        AlertDialog.Builder alert = new AlertDialog.Builder(mainContext);

        alert.setTitle("Add tag");
        LinearLayout layout = new LinearLayout(mainContext);
        layout.setOrientation(LinearLayout.VERTICAL);

        // Set an EditText view to get user input
        final EditText keyInput = new EditText(mainContext);
        final EditText valueInput = new EditText(mainContext);
        layout.addView(keyInput);
        layout.addView(valueInput);
        alert.setView(layout);

        alert.setPositiveButton("Add", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                currentlyEditedKvps.put(keyInput.getText().toString(), valueInput.getText().toString());
                if (isNodeKvpEdited) {
                    storageDelegator.setTags(storageDelegator.getCurrentStorage().getNode(nodeCursorIsCurrentlyOnId), currentlyEditedKvps);
                } else {
                    storageDelegator.setTags(storageDelegator.getCurrentStorage().getWay(wayCursorIsCurrentlyOnId), currentlyEditedKvps);
                }

                storageDelegator.getUndo().createCheckpoint("Added tag");
            }
        });

        alert.setNeutralButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
            }
        });

        alert.show();
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
