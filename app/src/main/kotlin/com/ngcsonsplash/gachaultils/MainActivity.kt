package com.ngcsonsplash.gachaultils

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var instanceList: ListView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        instanceList = findViewById(R.id.instanceList)

        // Placeholder for instance data
        val instances = arrayOf("Instance 1", "Instance 2", "Instance 3")
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, instances)
        instanceList.adapter = adapter

        instanceList.setOnItemClickListener { _, _, position, _ ->
            val selectedInstance = instances[position]
            Toast.makeText(this, "Selected: $selectedInstance", Toast.LENGTH_SHORT).show()
            // TODO: Implement logic to handle instance selection
        }

        findViewById<android.widget.Button>(R.id.createInstanceBtn).setOnClickListener {
            Toast.makeText(this, "Create Instance Clicked", Toast.LENGTH_SHORT).show()
            // TODO: Implement logic to create a new instance
        }

        findViewById<android.widget.Button>(R.id.selectApkBtn).setOnClickListener {
            Toast.makeText(this, "Select APK Clicked", Toast.LENGTH_SHORT).show()
            // TODO: Implement logic to select an APK file
        }

        findViewById<android.widget.Button>(R.id.downloadApkBtn).setOnClickListener {
            Toast.makeText(this, "Download APK Clicked", Toast.LENGTH_SHORT).show()
            // TODO: Implement logic to download an APK
        }
    }
}