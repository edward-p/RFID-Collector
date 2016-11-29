package com.geek.edward.rfidcollector;

import android.app.Activity;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.os.Environment;
import android.os.Process;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.util.SparseArrayCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.SparseArray;
import android.view.ContextMenu;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.stericson.RootShell.RootShell;  //这里引入了一个第三方包RootShell.jar，这个包里面有写好的关于root的一些便捷方法
                                           //可以在这里找到最新发布的版本 https://github.com/Stericson/RootShell/releases

public class MainActivity extends AppCompatActivity {
    int item_id;

    SparseArray<String> IDmap= new SparseArray<String>();


    private static boolean isRooted() {return RootShell.isAccessGiven();}

    private static void showcurrent(TextView textView,SharedPreferences spcu){
        boolean isexisted=new File(Environment.getRootDirectory().getPath()+"/etc/libnfc-nxp.conf.bak").exists();
        if(isexisted) {
            if(spcu.contains("current"))
                textView.setText(spcu.getString("current",""));
            else
                textView.setText("N/A");
        }
        else {
            SharedPreferences.Editor editor=spcu.edit();
            editor.putString("current","N/A");
            editor.apply();
            textView.setText("N/A");
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this,Scanning.class);
                startActivityForResult(intent,1);
            }
        });
        boolean ir=isRooted();
        Toast suc=Toast.makeText(getApplicationContext(), "Root access is given!", Toast.LENGTH_SHORT);
        Toast fai=Toast.makeText(getApplicationContext(),"Can't get root access!",Toast.LENGTH_SHORT);
        if(ir&&isfirstrun()) {
            suc.show();
            SharedPreferences first = getSharedPreferences("first", MODE_PRIVATE);
            SharedPreferences.Editor editor=first.edit();
            editor.putString("first","no");
            editor.apply();
        }
        else if(!ir&&isfirstrun()) {
            fai.show();
            finish();
        }
        showcurrent((TextView) findViewById(R.id.textView3),getSharedPreferences("current",MODE_PRIVATE));
        showUIDlst(this);
        ListView lst= (ListView) findViewById(R.id.listView);
        lst.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
                item_id=i;
                return false;
            }
        });
        lst.setOnCreateContextMenuListener(new View.OnCreateContextMenuListener() {
            @Override
            public void onCreateContextMenu(ContextMenu contextMenu, View view, ContextMenu.ContextMenuInfo contextMenuInfo) {
                contextMenu.add(0,0,0,"Emulate");
                contextMenu.add(0,1,0,"Rename");
                contextMenu.add(0,2,0,"Delete");
            }
        });
}

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent intent = new Intent(MainActivity.this,Settings.class);
            startActivityForResult(intent,2);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        String card= IDmap.get(item_id);
        SharedPreferences spuid = getSharedPreferences("UID", MODE_PRIVATE);
        SharedPreferences.Editor euid = spuid.edit();
        SharedPreferences spname = getSharedPreferences("name", MODE_PRIVATE);
        SharedPreferences.Editor ename = spname.edit();
        SharedPreferences spcu = getSharedPreferences("current", MODE_PRIVATE);
        SharedPreferences.Editor ecu = spcu.edit();
        switch (item.getItemId()){
            case 0:
                //挂载system为rw
                java.lang.Process p=null;
                try {
                    p=Runtime.getRuntime().exec(new String[]{"su","-c","mount -o rw,remount "+Environment.getRootDirectory().getPath()});
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    assert p != null;
                    p.waitFor();
                }catch (InterruptedException e) {
                  e.printStackTrace();
                }
                boolean isexisted=new File(Environment.getRootDirectory().getPath()+"/etc/libnfc-nxp.conf.bak").exists();
                if(!isexisted){
                    try {
                        p=Runtime.getRuntime().exec(new String[]{"su","-c","cp "+Environment.getRootDirectory().getPath()+"/etc/libnfc-nxp.conf "+Environment.getRootDirectory().getPath()+"/etc/libnfc-nxp.conf.bak"});
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                try {
                    p.waitFor();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                try {
                    p=Runtime.getRuntime().exec(new String[]{"su","-c","sed -i \"s/01, 02, 03, 04/"+card+"/g\" "+Environment.getRootDirectory().getPath()+"/etc/libnfc-nxp.conf"});
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    p.waitFor();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                //挂载system为ro
                try {
                    p=Runtime.getRuntime().exec(new String[]{"su","-c","mount -o ro,remount "+Environment.getRootDirectory().getPath()});
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    p.waitFor();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                //重启NFC服务
                try {
                    p=Runtime.getRuntime().exec(new String[]{"su","-c","svc nfc disable"});
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    p.waitFor();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                try {
                    p=Runtime.getRuntime().exec(new String[]{"su","-c","svc nfc enable"});
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    p.waitFor();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                Toast esuc=Toast.makeText(getApplicationContext(), card+"Emulated!", Toast.LENGTH_SHORT);
                esuc.show();
                ecu.putString("current",card);
                ecu.apply();
                showcurrent((TextView) findViewById(R.id.textView3),getSharedPreferences("current",MODE_PRIVATE));
                break;
            case 1: //修改卡片名
                final EditText editText=new EditText(this);
                editText.setText(spname.getString("card:"+card,""));
                new AlertDialog.Builder(this).setTitle("New Name:").setView(
                        editText).setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        SharedPreferences spname = getSharedPreferences("name", MODE_PRIVATE);
                        SharedPreferences.Editor ename = spname.edit();
                        ename.putString("card:"+IDmap.get(item_id),editText.getText().toString());
                        ename.apply();
                        showUIDlst(MainActivity.this);
                        dialogInterface.dismiss();
                    }
                })
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.dismiss();
                            }
                        }).show();
                break;
            case 2:

                euid.remove("card:"+card);
                euid.apply();
                ename.remove("card:"+card);
                ename.apply();
                showUIDlst(this);
                Toast suc=Toast.makeText(getApplicationContext(), card+" Deleted!", Toast.LENGTH_SHORT);
                suc.show();
                break;
        }
        return super.onContextItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        SharedPreferences spname = getSharedPreferences("name", MODE_PRIVATE);
        SharedPreferences.Editor ename = spname.edit();
        if(resultCode==1||resultCode==2) {
            Bundle bundle = data.getExtras();
                            String uid = bundle.getString("UID");
            SharedPreferences spuid = getSharedPreferences("UID", MODE_PRIVATE);
            SharedPreferences.Editor euid = spuid.edit();
                euid.putString("card:" + uid, uid);
                euid.apply();
            ename.putString("card:"+uid,"(null)");
            ename.apply();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        showUIDlst(this);
       showcurrent((TextView) findViewById(R.id.textView3),getSharedPreferences("current",MODE_PRIVATE));
    }
    public void showUIDlst(Context context){
        List<Map<String, String>> listmap=new ArrayList<>();
        SharedPreferences spname = getSharedPreferences("name", MODE_PRIVATE);
        SharedPreferences spuid = getSharedPreferences("UID", MODE_PRIVATE);
        Map<String, ?> allContent = spuid.getAll();
        int i=0;
        for(Map.Entry<String, ?>  entry : allContent.entrySet()){
            Map<String, String> map1 = new HashMap<>();
            map1.put("name",spname.getString("card:"+entry.getValue(),""));
            map1.put("uid",(String) entry.getValue());
            listmap.add(map1);
            IDmap.put(i++,(String)entry.getValue());
        }
        ListView lst= (ListView) findViewById(R.id.listView);
        lst.setAdapter(new SimpleAdapter(context,listmap,android.R.layout.simple_list_item_2,new String[]{"name","uid"},new int[]{android.R.id.text1,android.R.id.text2}));
    }
    private boolean isfirstrun(){
        SharedPreferences first = getSharedPreferences("first", MODE_PRIVATE);
        return first.getString("first","yes").equals("yes");
    }
}
