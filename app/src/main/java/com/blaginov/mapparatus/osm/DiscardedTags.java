package com.blaginov.mapparatus.osm;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.SortedMap;
import java.util.TreeMap;

import android.util.Log;

/**
 * Tags that we want to remove before saving to server. List is in discarded.json from the iD repository
 * @author simon
 *
 */
public class DiscardedTags {

	private HashSet<String> redundantTags = new HashSet<String>();
	
	SortedMap<String, String> newTags = new TreeMap<String, String>();

	/**
	 * Implicit assumption that the list will be short and that it is OK to read in synchronously
	 */
	DiscardedTags() {
        /*
		Resources r = Application.mainActivity.getResources();

		Log.d("DiscardedTags","Parsing configuration file");

		AssetManager assetManager = Application.mainActivity.getAssets();

		try {
			InputStream is = assetManager.open("discarded.json");
			JsonReader reader = new JsonReader(new InputStreamReader(is, "UTF-8"));
			try {

				try {
					reader.beginArray();
					while (reader.hasNext()) {
						redundantTags.add(reader.nextString());
					}
					reader.endArray();
					Log.d("DiscardedTags","Found " + redundantTags.size() + " tags.");
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} 
			}
			finally {
				reader.close();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
        String[] tags = {"KSJ2:ADS",
                "KSJ2:ARE",
                "KSJ2:AdminArea",
                "KSJ2:COP_label",
                "KSJ2:DFD",
                "KSJ2:INT",
                "KSJ2:INT_label",
                "KSJ2:LOC",
                "KSJ2:LPN",
                "KSJ2:OPC",
                "KSJ2:PubFacAdmin",
                "KSJ2:RAC",
                "KSJ2:RAC_label",
                "KSJ2:RIC",
                "KSJ2:RIN",
                "KSJ2:WSC",
                "KSJ2:coordinate",
                "KSJ2:curve_id",
                "KSJ2:curve_type",
                "KSJ2:filename",
                "KSJ2:lake_id",
                "KSJ2:lat",
                "KSJ2:long",
                "KSJ2:river_id",
                "yh:LINE_NAME",
                "yh:LINE_NUM",
                "yh:STRUCTURE",
                "yh:TOTYUMONO",
                "yh:TYPE",
                "yh:WIDTH_RANK"};
        for (String tag : tags) {
            redundantTags.add(tag);
        }
	}
	
	/**
	 * Remove the redundant tags from element.
	 * Notes:
	 *  - element already has the modified flag set if not, something went wrong and we skip 
	 *  - this does not create a checkpoint and assumes that we will never want to undo this
	 * @param element
	 */
	void remove(OsmElement element) {
		if (element.isUnchanged()) {
			Log.e("DicardedTags","Presented with unmodified element");
			return;
		}
		boolean modified = false;
		for (String key:element.getTags().keySet()) {
			Log.d("DicardedTags","Checking " + key);
			if (!redundantTags.contains(key)) {
				newTags.put(key, element.getTags().get(key));
			} else {
				Log.d("DicardedTags"," delete");
				modified = true;
			}
		}
		if (modified) {
			element.setTags(newTags);
		}
	}
}

