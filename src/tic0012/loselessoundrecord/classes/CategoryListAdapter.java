package tic0012.loselessoundrecord.classes;

import java.util.ArrayList;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.format.DateFormat;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;

/**
 * Expandable Adapter, contains all categories with records
 * 
 * @author tic0012, Michal Tichý
 */
public class CategoryListAdapter extends BaseExpandableListAdapter {
	
	/**
	 * All categories for list
	 */
	private ArrayList<Category> items;
	
	private Context context;

	public CategoryListAdapter(ArrayList<Category> items, Context context) {
		this.items = items;
		this.context = context;
	}

	/**
	 * Get Record from category
	 */
	public Record getChild(int groupPosition, int childPosition) {
		return this.items.get(groupPosition).records.get(childPosition);
	}

	/**
	 * Get Record ID from category
	 */
	public long getChildId(int groupPosition, int childPosition) {
		return this.items.get(groupPosition).records.get(childPosition).getId();		
	}

	public View getChildView(
			int groupPosition, 
			int childPosition,
			boolean isLastChild, 
			View convertView, 
			ViewGroup parent
			) {		
		
		Record record = this.items.get(groupPosition).records.get(childPosition);
		
		TextView textView = this.getGenericView();
		textView.setTextColor(Color.DKGRAY);
		textView.setText(DateFormat.format("d. M. yyyy k:mm", record.dateRecorderd));		
		textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
		
		return textView;
	}

	/**
	 * Get Records count in Category
	 */
	public int getChildrenCount(int groupPosition) {
		return this.items.get(groupPosition).records.size();			
	}

	/**
	 * Get Category
	 */
	public Category getGroup(int groupPosition) {
		return this.items.get(groupPosition);
	}

	/**
	 * Get Category count
	 */
	public int getGroupCount() {
		return this.items.size();		
	}

	/**
	 * Get category ID
	 */
	public long getGroupId(int groupPosition) {
		return this.items.get(groupPosition).getId();
	}

	public View getGroupView(
			int groupPosition, 
			boolean isExpanded,
			View convertView, 
			ViewGroup parent
			) {
		TextView textView = this.getGenericView();				
		textView.setText(this.items.get(groupPosition).name + " (" + this.items.get(groupPosition).records.size() + ")");
		
		textView.setTextColor(Color.BLACK);
		textView.setTypeface(null, Typeface.BOLD);	
		textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
		
		return textView;

	}

	public boolean hasStableIds() {
		return true;
	}

	public boolean isChildSelectable(int groupPosition, int childPosition) {
		return true;
	}

	public TextView getGenericView() {
		// Layout parameters for the ExpandableListView
		AbsListView.LayoutParams lp = new AbsListView.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT, 64);
		TextView textView = new TextView(this.context);
		textView.setLayoutParams(lp);

		// Center the text vertically
		textView.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);

		// Set the text starting position
		textView.setPadding(50, 0, 0, 0);

		return textView;
	}

}
