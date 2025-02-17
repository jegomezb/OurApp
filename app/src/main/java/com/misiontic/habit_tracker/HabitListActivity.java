package com.misiontic.habit_tracker;

import static android.media.CamcorderProfile.get;

import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.getbase.floatingactionbutton.FloatingActionButton;
import com.misiontic.habit_tracker.db.MySQLiteHelper;
import com.misiontic.habit_tracker.listviews.HabitListViewAdapter;
import com.misiontic.habit_tracker.model.Habits;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import com.google.gson.Gson;

//import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class HabitListActivity extends AppCompatActivity implements HabitListViewAdapter.CheckBoxCheckedListener{

    private static ArrayList<Habits> habitList;
    private static ListView listView;
    private static HabitListViewAdapter adapter;
    //private FloatingActionButton fabCreate;
    private FloatingActionButton fabCreate, fabReset;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_habit_list);

        listView = findViewById(R.id.habitsList);
        habitList = new ArrayList<>();

        Toolbar myChildToolbar =
                (Toolbar) findViewById(R.id.yl_toolbar);
        setSupportActionBar(myChildToolbar);

        ActionBar ab = getSupportActionBar();

        ab.setDisplayHomeAsUpEnabled(true);
        ab.setTitle(R.string.title_go_home);
        ab.setHomeAsUpIndicator(R.drawable.ic_back_white);

        fabCreate=findViewById(R.id.fabCreate);
        fabReset=findViewById(R.id.fabReset);

        fabCreate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HabitListActivity.this, NewActivity.class);
                startActivity(intent);
            }
        });

        fabReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MySQLiteHelper connectionBD = new MySQLiteHelper(HabitListActivity.this);
                connectionBD.resetData();
                Intent intent = new Intent(HabitListActivity.this, HabitListActivity.class);
                startActivity(intent);
                Toast.makeText(HabitListActivity.this, HabitListActivity.this.getString(R.string.all_habits_reset), Toast.LENGTH_SHORT).show();
            }
        });

        //Traer todayHabitListErase para eliminar los habitlist que ya se habían enviado a todayHabitActivity


        //Cursor ...a... listview
        Cursor results=getHabitsBd();
        habitList = getHabitList(results);
        listView = adapterHabits(habitList);
        results.close();


        //Al tocar
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Habits selectedHabit=(Habits)listView.getItemAtPosition(position);
                //Mostrar en un DialogFragment
                dialog(selectedHabit);
            }
        });

        //Al chequear
        /*
        for (int i = 0; i < habitList.size(); i++) {
            if(habitList.get(i).isSelected()){
                Toast.makeText(HabitListActivity.this, "Se remueve el elemento "+habitList.get(i).getName(), Toast.LENGTH_SHORT).show();
                habitList.remove(get(i));
            }
        }
        adapter.notifyDataSetChanged();
        */
        adapter.setCheckedlistener(this);

        //si la hora son las 00:00 -> update a base todos los campos de checked serán false. EN SEGUNDO PLANO
        //String time = new SimpleDateFormat("hh:mm").format(new Date());
        //if(time=="00:00"){

        //}
    }

    public Cursor getHabitsBd(){
        MySQLiteHelper connectionBD = new MySQLiteHelper(this);
        String selectQuery = "SELECT * FROM habits WHERE checked=?";
        String[] params = new String[]{String.valueOf("FALSE")};
        Cursor results = connectionBD.getData(selectQuery, params);
        //Cursor results = connectionBD.getData(selectQuery, null);
        return results;
    }

    public ArrayList<Habits> getHabitList(Cursor results) {
        try {
            results.moveToFirst();
            do {
                int id = results.getInt(0);
                int nameId = results.getColumnIndex("name");
                String nameHabit = results.getString(nameId);
                String descriptionHabit = results.getString(2);
                String categoryHabit = results.getString(3);

                Habits newHabit = new Habits(nameHabit, descriptionHabit, categoryHabit);
                newHabit.setId(id);

                habitList.add(newHabit);

            } while (results.moveToNext());
        } catch (Exception e) {
            Toast.makeText(this, this.getString(R.string.failure_on_get), Toast.LENGTH_LONG).show();
        }
        return habitList;
    }

    public ListView adapterHabits(ArrayList<Habits> habitList) {
        adapter = new HabitListViewAdapter(this, habitList);
        listView.setAdapter(adapter);
        return listView;
    }

    public void dialog(Habits selectedHabit) {
        AlertDialog.Builder dialog1 = new AlertDialog.Builder(HabitListActivity.this);
        dialog1.setTitle(this.getString(R.string.edit_text_habit_description)+" "+selectedHabit.getName()+": ");
        dialog1.setMessage(selectedHabit.getDescription());
        dialog1.setPositiveButton(this.getString(R.string.back), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        dialog1.setNegativeButton(this.getString(R.string.btn_delete), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String tabla = "habits";
                String whereClause = "_id = ?";
                String[] params = new String[]{String.valueOf(selectedHabit.getId())};
                MySQLiteHelper connectionBD = new MySQLiteHelper(HabitListActivity.this);
                int rows = connectionBD.deleteData(tabla, whereClause, params);


                if (rows > 0) {
                    Toast.makeText(HabitListActivity.this,R.string.habit_deleted, Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(HabitListActivity.this, HomeActivity.class);
                    startActivity(intent);

                } else {
                    Toast.makeText(HabitListActivity.this, R.string.habit_didnt_delete, Toast.LENGTH_SHORT).show();
                }
                dialog.cancel();
            }
        });

        dialog1.create();
        dialog1.show();
        //se puede eliminar desde aquí?
    }


    @Override
    public void getCheckBoxCheckedListener(int position) {
        Habits checkedHabit=(Habits)listView.getItemAtPosition(position);
        //Toast.makeText(getApplicationContext(),"Has marcado "+checkedHabit.getName()+"?",Toast.LENGTH_LONG).show();
        habitList.remove(position);
        //adapter.notifyDataSetChanged();
        //Enviar normal
        int checkedHabitId=checkedHabit.getId();
        String checkedHabitName=checkedHabit.getName();
        checkedHabit.setSelected(true);
        /*
        Intent intentToday=new Intent(HabitListActivity.this,TodayHabitsActivity.class);
        intentToday.putExtra("checkedHabitId",checkedHabitId);
        startActivity(intentToday);
        */
        //Enviar con serializable
        /*
        Intent intentToday=new Intent(HabitListActivity.this,TodayHabitsActivity.class);
        intentToday.putStringArrayListExtra("today", ArrayList<Habits>todayHabitList);
        */
        //Enviar con bundle
        /*
        Bundle value= new Bundle();
        value.putParcelableArrayList("temp", todayHabitList);
        */
        //Enviar con bd
        MySQLiteHelper connectionBD = new MySQLiteHelper(this);


        //String now = Calendar.getInstance().getTime().toString();

        /*
        Calendar now = Calendar.getInstance();
        String DATE_FORMAT_NOW = "yyyy-MM-dd";
        SimpleDateFormat today = new SimpleDateFormat(DATE_FORMAT_NOW);
        String.valueOf(today.format(now.getTime()));
         */

        updateHabitChecked(checkedHabit);

        String today = new SimpleDateFormat("dd-MM-yyyy").format(new Date());
        String time = new SimpleDateFormat("HH:mm:ss").format(new Date());

        String insertQuery = "INSERT INTO dates(date,time,name_habit,id_habit )" +
                "VALUES('" + today + "','"+time+ "','" +checkedHabitName+"','"+ checkedHabitId +"')";

        boolean suc = connectionBD.insertData(insertQuery);
        if (suc) {
            Toast.makeText(this, this.getString(R.string.success_on_save) +" "+this.getString(R.string.in_local), Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, this.getString(R.string.failure_on_save) +" "+this.getString(R.string.in_local), Toast.LENGTH_LONG).show();
        }
        //adapter.clear();
        adapter.notifyDataSetChanged();
        listView.setAdapter(adapter);

    }

    public void updateHabitChecked(Habits checkedHabit) {
        String tabla = "habits";
        ContentValues cv = new ContentValues();
        cv.put("name", checkedHabit.getName().toString());
        cv.put("description", checkedHabit.getDescription().toString());
        cv.put("category", checkedHabit.getCategory().toString());
        cv.put("checked", "TRUE");
        String whereClause = "_id = ?";
        String[] params = new String[]{String.valueOf(checkedHabit.getId())};
        MySQLiteHelper connectionBD = new MySQLiteHelper(this);
        int rows = connectionBD.updateData(tabla, cv, whereClause, params);
        if (rows > 0) {
            Toast.makeText(this, this.getString(R.string.habit_checked), Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(HabitListActivity.this, TodayHabitsActivity.class);
            startActivity(intent);

        } else {
            Toast.makeText(this, this.getString(R.string.habit_didnt_checked), Toast.LENGTH_SHORT).show();
        }
    }

}