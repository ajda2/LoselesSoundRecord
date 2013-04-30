package tic0012.loselessoundrecord;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Toast;

/**
 * Base Activity
 * 
 * @author tic0012, Michal Tichý
 */
public abstract class BaseActivity extends Activity {
	/**
	 * Key to retrieve stored WAV file path through extra intent
	 */
	protected final String FILEPATH_FLAG = "filePath";

	/**
	 * Key to retrieve stored Record id through extra intent
	 */
	protected final String RECORD_ID_FLAG = "gunshot_id";

	/**
	 * Key to retrieve stored Category id through extra intent
	 */
	protected final String CATEGORY_ID_FLAG = "category_id";
	
	/**
	 * Basic application preferences name
	 */
	private final String PREFERENCES_NAME = "BasePref";
	
	/**
	 * Sensitivity key name in configuration file
	 */
	protected final String CONFIG_SENSITIVITY_KEY = "gunshot_sensitivity";
	
	/**
	 * Image show key name in configuration file
	 */
	protected final String CONFIG_SHOW_IMAGE_KEY = "show_image";
	
	/**
	 * Delete WAV file key name in configuration file
	 */
	protected final String CONFIG_DELETE_WAV = "delete_wav";
	
	/**
	 * Save records into music folder name in configuration file
	 */
	protected final String SAVE_INTO_MUSIC_FOLDER = "record_into_music_folder";
	
	/**
	 * Auto stop time key in configuration file
	 */
	protected final String CONFIG_AUTO_STOP_TIME_KEY = "auto_stop_time";

	/**
	 * Shared preferences for whole application
	 */
	protected SharedPreferences preferences;
	

	@Override
    protected void onCreate(Bundle state){
       super.onCreate(state);                    
       
       this.preferences = this.getSharedPreferences(this.PREFERENCES_NAME, 0);         
	}		
	
	@Override
	/**
	 * Menu items click handling
	 */
	public boolean onOptionsItemSelected(MenuItem item) {

		// Handle item selection
		switch (item.getItemId()) {
		case android.R.id.home:
			NavUtils.navigateUpFromSameTask(this);

			return true;
		case R.id.menu_show_stored:
			this.setActivity(StoredActivity.class, false);
			return true;

		case R.id.menu_settings:			
			this.settingsDialog();
			
			return true;		
		}
		
		return super.onOptionsItemSelected(item);
	}	
	
	/**
	 * Bring Activity to front and terminate current
	 * @param cls Activity class to set
	 * @param terminate Set TRUE to terminate current Activity
	 */
	protected void setActivity(Class<?> cls, boolean terminate) {
		// start parent activity
		Intent intent = new Intent(this.getApplicationContext(), cls);
		intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);		
		this.startActivity(intent);

		// end this activity
		if(terminate){
			this.finish();
		}		
	}		
	
	/**
	 * Show and handle settings dialog
	 */
	private void settingsDialog(){
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.menu_settings);	
		
		LayoutInflater inflater = LayoutInflater.from(this);
		View view = inflater.inflate(R.layout.dialog_settings_edit, null);
		builder.setView(view);
		
		final SeekBar sensitivityBar = (SeekBar)view.findViewById(R.id.sensitivityBar);	
		final CheckBox showImageCheckBox = (CheckBox)view.findViewById(R.id.show_image_checkbox);
		final CheckBox deleteWavCheckBox = (CheckBox)view.findViewById(R.id.delete_wav_checkbox);
		final CheckBox saveIntoMusicCheckBox = (CheckBox)view.findViewById(R.id.save_into_music_checkbox);
		final EditText stopTimeEditText = (EditText)view.findViewById(R.id.stopTimeEditText);	
		final Resources res = this.getResources();
		
		
		sensitivityBar.setProgress(this.preferences.getInt(this.CONFIG_SENSITIVITY_KEY, res.getInteger(R.integer.gunshot_sensitivity)));
		showImageCheckBox.setChecked(this.preferences.getBoolean(this.CONFIG_SHOW_IMAGE_KEY, res.getBoolean(R.bool.show_image)));
		deleteWavCheckBox.setChecked(this.preferences.getBoolean(this.CONFIG_DELETE_WAV, res.getBoolean(R.bool.delete_wav_after_read)));
		
		int autoStopDefault = this.preferences.getInt(this.CONFIG_AUTO_STOP_TIME_KEY, res.getInteger(R.integer.auto_stop_time));
		if(autoStopDefault > 0){
			stopTimeEditText.setText(Integer.toString(autoStopDefault));
		}		
		
		builder.setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {				
		    public void onClick(DialogInterface dialog, int whichButton) {
		    	int sensitivityValue = sensitivityBar.getProgress();	
		    	boolean showImage = showImageCheckBox.isChecked();
		    	boolean deleteWav = deleteWavCheckBox.isChecked();
		    	boolean saveIntoMusic = saveIntoMusicCheckBox.isChecked();				    			    
		    	int autoStopTime = 0; 
		    	
		    	String textValue = stopTimeEditText.getText().toString();
		    	
		    	if (!textValue.equals("")) {
		    		autoStopTime = Integer.parseInt(textValue);
				}		    
		    	
		    	
		    	SharedPreferences.Editor editor = preferences.edit();
		    	editor.putInt(CONFIG_SENSITIVITY_KEY, sensitivityValue);
		    	editor.putBoolean(CONFIG_SHOW_IMAGE_KEY, showImage);
		    	editor.putBoolean(CONFIG_DELETE_WAV, deleteWav);
		    	editor.putBoolean(SAVE_INTO_MUSIC_FOLDER, saveIntoMusic);
		    	editor.putInt(CONFIG_AUTO_STOP_TIME_KEY, autoStopTime);
		    	
		    	if(editor.commit()){
		    		Toast.makeText(getApplicationContext(),
							R.string.change_saved, Toast.LENGTH_SHORT).show();
		    	}
		    	else{
		    		Toast.makeText(getApplicationContext(),
							R.string.error, Toast.LENGTH_SHORT).show();
		    	}		    			    						   				 
		    }
		});
		
		AlertDialog settingsDialog = builder.create();						
		settingsDialog.show();			
	}	
	
}