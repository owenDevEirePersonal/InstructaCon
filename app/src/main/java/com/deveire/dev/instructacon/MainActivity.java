package com.deveire.dev.instructacon;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.deveire.dev.instructacon.remastered.Manager2Activity;
import com.deveire.dev.instructacon.remastered.Station2Activity;

public class MainActivity extends Activity
{

    private Button driverButton;
    private Button managerButton;

    private Button driver2Button;
    private Button manager2Button;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.

        driverButton = (Button) findViewById(R.id.driverButton);
        managerButton = (Button) findViewById(R.id.managerButton);
        driver2Button = (Button) findViewById(R.id.driver2Button);
        manager2Button = (Button) findViewById(R.id.manager2Button);

        driverButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                startActivity(new Intent(getApplicationContext(), StationActivity.class));
            }
        });

        managerButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                startActivity(new Intent(getApplicationContext(), ManagerActivity.class));
            }
        });

        driver2Button.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                startActivity(new Intent(getApplicationContext(), Station2Activity.class));
            }
        });

        manager2Button.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                startActivity(new Intent(getApplicationContext(), Manager2Activity.class));
            }
        });
    }


}
