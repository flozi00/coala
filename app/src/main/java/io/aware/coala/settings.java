package io.aware.coala;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;

import androidx.appcompat.app.AppCompatActivity;
import androidx.loader.content.CursorLoader;

public class settings extends AppCompatActivity {

    public EditText phonenumber, customurl;
    public CheckBox alarm, api, sms, call;
    public boolean alarm_bool, api_bool, sms_bool, call_bool;
    public Button save;
    public Button mp3Button;
    public Button logs;
    public SharedPreferences pref;
    public String myURI = null;

    public int helpfeature;
    private  final int SELECT_MUSIC = 1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings);
        save = findViewById(R.id.save);
        phonenumber = findViewById(R.id.phonenumber);
        customurl = findViewById(R.id.customurl);

        alarm = findViewById(R.id.alarm);
        sms = findViewById(R.id.sms);
        api = findViewById(R.id.api);
        call = findViewById(R.id.call);

        mp3Button = findViewById(R.id.setdefault);
        logs = findViewById(R.id.logs);


        pref = getApplicationContext().getSharedPreferences("settings", 0); // 0 - for private mode
        phonenumber.setText(pref.getString("phonenumber",""));
        customurl.setText(pref.getString("customurl",""));
        alarm.setChecked(pref.getBoolean("alarm",false));
        sms.setChecked(pref.getBoolean("sms",false));
        api.setChecked(pref.getBoolean("api",false));
        call.setChecked(pref.getBoolean("call",false));

        alarm_bool = pref.getBoolean("alarm",false);
        sms_bool = pref.getBoolean("sms",false);
        api_bool = pref.getBoolean("api",false);
        call_bool = pref.getBoolean("call",false);


        alarm.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                alarm_bool = b;
            }
        });

        sms.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                sms_bool = b;
            }
        });

        call.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                call_bool = b;
            }
        });

        api.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                api_bool = b;
            }
        });


        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pref = getApplicationContext().getSharedPreferences("settings", 0); // 0 - for private mode
                SharedPreferences.Editor editor = pref.edit();
                editor.putString("phonenumber",phonenumber.getText().toString());
                editor.putString("customurl",customurl.getText().toString());
                editor.putBoolean("alarm",alarm_bool);
                editor.putBoolean("sms",sms_bool);
                editor.putBoolean("call",call_bool);
                editor.putBoolean("api",api_bool);

                if(myURI != null){
                    editor.putString("mp3URI",myURI);
                }
                editor.commit();

                finish();
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(intent);
            }
        });

        mp3Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(i,SELECT_MUSIC);
            }
        });

        logs.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SharedPreferences errors = getApplicationContext().getSharedPreferences("ErrorLog", Context.MODE_MULTI_PROCESS);
                String error = errors.getString("ErrorLogs","Log:\n");
                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_TEXT, error);
                sendIntent.setType("text/plain");

                Intent shareIntent = Intent.createChooser(sendIntent, null);
                startActivity(shareIntent);
            }
        });

    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == SELECT_MUSIC) {
                Uri selectedMusicUri = data.getData();
                if (selectedMusicUri != null) {
                    myURI = getRealPathFromURI(this, selectedMusicUri);
                }
            }
        }
    }

    private String getRealPathFromURI(Context context, Uri contentUri) {
        String[] projection = { MediaStore.Audio.Media.DATA };
        CursorLoader loader = new CursorLoader(context, contentUri, projection, null, null, null);
        Cursor cursor = loader.loadInBackground();
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);
        cursor.moveToFirst();
        return cursor.getString(column_index);
    }

    @Override
    public void onBackPressed() {
        save.performClick();
    }

}
