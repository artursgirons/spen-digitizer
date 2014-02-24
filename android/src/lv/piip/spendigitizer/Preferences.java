package lv.piip.spendigitizer;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
 
public class Preferences extends PreferenceActivity implements OnSharedPreferenceChangeListener {
        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.prefs);
            
    		if(PreferenceManager.getDefaultSharedPreferences(this).getBoolean("autolocate", true))
    		{
    			getPreferenceScreen().findPreference("host").setEnabled(false);
    		}
    		else
    		{
    			getPreferenceScreen().findPreference("host").setEnabled(true);
    		}
            
            PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);
        }
        
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        	if(key.equals("autolocate"))
        	{
        		if(sharedPreferences.getBoolean("autolocate", true))
        		{
        			getPreferenceScreen().findPreference("host").setEnabled(false);
        		}
        		else
        		{
        			getPreferenceScreen().findPreference("host").setEnabled(true);
        		}
        	}
        }
}