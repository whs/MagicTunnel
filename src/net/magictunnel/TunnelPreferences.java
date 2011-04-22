package net.magictunnel;

import java.security.KeyStore.LoadStoreParameter;

import net.magictunnel.settings.Interfaces;
import net.magictunnel.settings.Profile;
import net.magictunnel.settings.Settings;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.text.InputType;
import android.text.method.PasswordTransformationMethod;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;


public class TunnelPreferences extends PreferenceActivity {
	private static final int MENU_SAVE = Menu.FIRST;
    private static final int MENU_CANCEL = Menu.FIRST + 1;

    private static final int CONFIRM_DIALOG_ID = 0;

	private String m_name;
	private String m_domain;
	private String m_interface;
	private String m_password;
    
	private EditTextPreference m_prefName;
	private EditTextPreference m_prefDomain;
	private EditTextPreference m_prefPassword;
	private ListPreference m_prefInterface;
	
	private boolean m_new;
	private Settings m_settings;
	private Profile m_profile;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.tunnelsettings);
		
		populatePreferenceScreen();		
		fillInProperties();
	}
	
	private void fillInProperties() {
		MagicTunnel app = (MagicTunnel)getApplication();		
		m_settings = app.getSettings();
		String curProfile = m_settings.getCurrentSettingsProfile();
		if (curProfile.equals("")) {
			m_new = true;
			m_profile = new Profile();
			m_password = m_interface = m_domain = m_name = "";			
			return;
		}
	
		Profile prof = m_settings.getProfile(curProfile);
		if (prof == null) {
			throw new RuntimeException("Could not retrive profile");
		}
		
		m_name = prof.getName();
		m_prefName.getEditText().setText(prof.getName());
		m_prefName.setSummary(prof.getName());
		
		m_domain = prof.getDomainName();
		m_prefDomain.getEditText().setText(prof.getDomainName());
		m_prefDomain.setSummary(prof.getDomainName());
		
		m_interface = prof.getInterface().toString();
		m_prefInterface.setValue(prof.getInterface().toString());
		m_prefInterface.setSummary(getInterfaceSummary(m_prefInterface, prof.getInterface()));
		
		m_password = prof.getPassword();
		m_prefPassword.getEditText().setText(prof.getPassword());
		
		m_profile = prof;
	}
	
	private String validate() {
		if (!m_name.equals(m_profile.getName())) {
			Profile prof = m_settings.getProfile(m_name);
			if (prof != null) {
				return getString(R.string.profile_exists);
			}
		}
		
		if (m_name.equals("")) {
			return getString(R.string.profile_enter_name);
		}
		
		if (m_interface.equals("")) {
			return getString(R.string.profile_select_interface);
		}
		
		if (m_domain.equals("")) {
			return getString(R.string.profile_enter_domain);
		}		
		return null;
	}
	
	private void saveProperties() {
		m_profile.setDomainName(m_domain);
		m_profile.setInterface(Interfaces.valueOf(m_interface));
		m_profile.setPassword(m_password);
		if (m_new) {
			m_profile.setName(m_name);
			m_settings.addProfile(m_profile);
			m_profile.saveProfile(this);
		}else {
			m_settings.rename(this, m_profile.getName(), m_name);
		}
	}
	
	private boolean profileChanged() {
		return !m_domain.equals(m_profile.getDomainName()) ||
		!m_interface.equals(m_profile.getInterface().toString()) ||
		!m_password.equals(m_profile.getPassword()) ||
		!m_name.equals(m_profile.getName());
	}
	
	private void populatePreferenceScreen() {
		PreferenceScreen screen = getPreferenceScreen();
				
		PreferenceCategory cat = new PreferenceCategory(this);
		String catName = getString(R.string.dns_tunnel_settings);
		cat.setTitle(catName);
		screen.addPreference(cat);		

		m_prefName = createNamePreference();
		screen.addPreference(m_prefName);
		
		m_prefInterface = createInterfacePreference();
		screen.addPreference(m_prefInterface);
		
		m_prefDomain = createDomainPreference();
		screen.addPreference(m_prefDomain);
		
		m_prefPassword = createPasswordPreference();
		screen.addPreference(m_prefPassword);		
	}
	
	private EditTextPreference createNamePreference() {
		EditTextPreference prefName = new EditTextPreference(this);
		prefName.setTitle(R.string.profile_name);
		prefName.setDialogTitle(R.string.profile_name);
		prefName.getEditText().setInputType(
				prefName.getEditText().getInputType() & ~InputType.TYPE_TEXT_FLAG_MULTI_LINE);
		prefName.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				m_name = ((String) newValue).trim();
				preference.setSummary(m_name);
				return true;
			}
		});
		return prefName;
	}
	
	private String getInterfaceSummary(Preference preference, Interfaces key) {
		CharSequence [] seq = ((ListPreference)preference).getEntryValues();
		CharSequence [] val = ((ListPreference)preference).getEntries();
		int i=0;
		for (CharSequence s:seq) {
			if (s.toString().equals(key.toString())) {
				return val[i].toString();
			}
			++i;
		}
		return "";
	}
	
	private ListPreference createInterfacePreference() {
		ListPreference prefInterface = new ListPreference(this);
		//prefInterface.setKey(prefixedName + Profile.PROFILE_INTERFACE);
		prefInterface.setTitle(R.string.network_interface);
		prefInterface.setEntries(R.array.interface_list);
		prefInterface.setEntryValues(R.array.interface_list_values);
		prefInterface.setDialogTitle(R.string.network_interface);
		prefInterface.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {			
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				m_interface = (String)newValue;
				preference.setSummary(getInterfaceSummary(preference, Interfaces.valueOf(m_interface)));
				return true;
			}
		});

		return prefInterface;
	}
	
	private EditTextPreference createDomainPreference() {
		EditTextPreference prefDomain = new EditTextPreference(this);
//		prefDomain.setKey(prefixedName + Profile.PROFILE_DOMAIN);
		prefDomain.setTitle(R.string.domain_name);
		prefDomain.setDialogTitle(R.string.domain_name);
		prefDomain.getEditText().setInputType(
				prefDomain.getEditText().getInputType() & ~InputType.TYPE_TEXT_FLAG_MULTI_LINE);
		prefDomain.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {			
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				m_domain = ((String) newValue).trim();
				preference.setSummary(m_domain);
				return true;
			}
		});
		return prefDomain;
	}
	
	private EditTextPreference createPasswordPreference() {
		EditTextPreference prefPassword = new EditTextPreference(this);
		prefPassword.setTitle(R.string.password);
		prefPassword.setDialogTitle(R.string.password);
		prefPassword.getEditText().setInputType(
                InputType.TYPE_TEXT_VARIATION_PASSWORD);
		prefPassword.getEditText().setTransformationMethod(
                new PasswordTransformationMethod());
		prefPassword.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				m_password = ((String) newValue);
				return true;
			}
		});

		return prefPassword;
	}
	
	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(0, MENU_SAVE, 0, R.string.save)
            .setIcon(android.R.drawable.ic_menu_save);
        menu.add(0, MENU_CANCEL, 0, R.string.cancel)
            .setIcon(android.R.drawable.ic_menu_close_clear_cancel);
        return true;
    }

	@Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                if (validateAndSaveResult()) 
                	finish();
                return true;
        }
        return super.onKeyDown(keyCode, event);
    }

	public boolean validateAndSaveResult() {
		String error = validate();
    	if (error != null) {
    		Utils.showErrorMessage(this, error);
    		return false;
    	}else {
    		saveProperties();
    	}
    	return true;
	}
	
	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_SAVE:
            	if (validateAndSaveResult()) 
            		finish();
                return true;

            case MENU_CANCEL:
                if (profileChanged()) {
                	showDialog(CONFIRM_DIALOG_ID);
                } else {
                    finish();
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

	
	@Override
    protected Dialog onCreateDialog(int id) {

        if (id == CONFIRM_DIALOG_ID) {
            return new AlertDialog.Builder(this)
                    .setTitle(android.R.string.dialog_alert_title)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setMessage(m_new
                            ? R.string.confirm_add_profile_cancellation
                            : R.string.confirm_edit_profile_cancellation)
                    .setPositiveButton(R.string.yes,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int w) {
                                    finish();
                                }
                            })
                    .setNegativeButton(R.string.no, null)
                    .create();
        }

        return super.onCreateDialog(id);
    }
	

}