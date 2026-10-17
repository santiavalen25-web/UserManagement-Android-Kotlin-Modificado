package com.example.parcialkotlinapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.util.Calendar

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        val tvSaludo = findViewById<TextView>(R.id.tvSaludo)
        val btnLogoutIcon = findViewById<ImageButton>(R.id.btnLogoutIcon)
        val btnParImpar = findViewById<Button>(R.id.btnParImpar)

        // Nombre recibido desde LoginActivity
        val nombreUsuario = intent.getStringExtra("nombreUsuario") ?: "Usuario"

        // Hora actual del dispositivo
        val horaActual = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

        // Determinar saludo
        val saludo = when (horaActual) {
            in 5..11 -> "Buenos días"
            in 12..17 -> "Buenas tardes"
            else -> "Buenas noches"
        }

        // Mostrar saludo personalizado
        tvSaludo.text = "$saludo, $nombreUsuario"

        // Manejo de los bordes de la pantalla
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // BOTÓN EJERCICIO PAR O IMPAR
        btnParImpar.setOnClickListener {
            val intent = Intent(this, ParImparActivity::class.java)
            startActivity(intent)
        }

        // 4.6 CIERRE DE SESIÓN CON ÍCONO SUPERIOR Y ALERTDIALOG DE CONFIRMACIÓN
        btnLogoutIcon.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Cerrar sesión")
                .setMessage("¿Está seguro de que desea cerrar sesión?")
                .setNegativeButton("CANCELAR") { dialog, _ ->
                    dialog.dismiss()
                }
                .setPositiveButton("SALIR") { dialog, _ ->
                    dialog.dismiss()
                    val intent = Intent(this, LoginActivity::class.java)
                    // Limpiar la pila de actividades para que el botón Atrás no regrese a la sesión anterior
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }
                .show()
        }
    }
}