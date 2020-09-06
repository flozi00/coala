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
import android.widget.EditText;
import android.widget.Spinner;

import androidx.appcompat.app.AppCompatActivity;
import androidx.loader.content.CursorLoader;

public class settings extends AppCompatActivity {

    public EditText phonenumber;
    public Button save;
    public Button mp3Button;
    public Button logs;
    public SharedPreferences pref;
    public Spinner hlpfeature;
    public String myURI = null;

    public int helpfeature;
    private  final int SELECT_MUSIC = 1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings);
        save = findViewById(R.id.save);
        phonenumber = findViewById(R.id.phonenumber);
        mp3Button = findViewById(R.id.setdefault);
        logs = findViewById(R.id.logs);
        hlpfeature = (Spinner) findViewById(R.id.helpfeature);



        ArrayAdapter<String> adapter6 = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, getResources()
                .getStringArray(R.array.hlpf));//setting the country_array to spinner
        adapter6.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        hlpfeature.setAdapter(adapter6);
//if you want to set any action you can do in this listener
        hlpfeature.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1, int position, long id) {
                helpfeature = position;
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });



        pref = getApplicationContext().getSharedPreferences("settings", 0); // 0 - for private mode
        phonenumber.setText(pref.getString("phonenumber",""));
        hlpfeature.setSelection(pref.getInt("hlp", 0));


        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pref = getApplicationContext().getSharedPreferences("settings", 0); // 0 - for private mode
                SharedPreferences.Editor editor = pref.edit();
                editor.putString("phonenumber",phonenumber.getText().toString());
                editor.putInt("hlp",helpfeature);
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
                    String pathFromUri = getRealPathFromURI(this, selectedMusicUri);
                    myURI = pathFromUri;
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
