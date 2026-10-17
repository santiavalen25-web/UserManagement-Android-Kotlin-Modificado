package com.example.parcialkotlinapp

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.parcialkotlinapp.api.ApiClient
import com.example.parcialkotlinapp.models.CrearUsuarioRequest
import com.example.parcialkotlinapp.models.Usuario
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Calendar
import java.util.Locale

class RegistroActivity : AppCompatActivity() {

    private var fechaSeleccionadaIso: String? = null
    private var edadCalculada: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registro)

        // COMPONENTES
        val btnBackRegistro = findViewById<ImageButton>(R.id.btnBackRegistro)
        val etNombre = findViewById<EditText>(R.id.etNombre)
        val etApellido = findViewById<EditText>(R.id.etApellido)
        val etCorreo = findViewById<EditText>(R.id.etCorreoRegistro)
        val etPasswordRegistro = findViewById<EditText>(R.id.etPasswordRegistro)
        val etFechaNacimiento = findViewById<EditText>(R.id.etFechaNacimiento)
        val etUniversidad = findViewById<EditText>(R.id.etUniversidad)
        val spSemestre = findViewById<Spinner>(R.id.spSemestre)
        val btnRegistrar = findViewById<Button>(R.id.btnRegistrar)
        val tvMensaje = findViewById<TextView>(R.id.tvMensajeRegistro)

        // FLECHA DE NAVEGACIÓN SUPERIOR IZQUIERDA
        btnBackRegistro.setOnClickListener {
            finish()
        }

        // SPINNER DE SEMESTRE (1 A 10)
        val semestres = arrayOf(
            "Seleccione un semestre",
            "1", "2", "3", "4", "5", "6", "7", "8", "9", "10"
        )

        val adaptador = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            semestres
        )
        adaptador.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spSemestre.adapter = adaptador

        // DATEPICKER PARA FECHA DE NACIMIENTO Y CÁLCULO EXACTO DE EDAD
        etFechaNacimiento.setOnClickListener {
            val calendarioActual = Calendar.getInstance()
            val anioActual = calendarioActual.get(Calendar.YEAR)
            val mesActual = calendarioActual.get(Calendar.MONTH)
            val diaActual = calendarioActual.get(Calendar.DAY_OF_MONTH)

            val datePicker = DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    // Formato visual DD/MM/YYYY y formato ISO YYYY-MM-DD para el Backend
                    val fechaVisual = String.format(
                        Locale.getDefault(),
                        "%02d/%02d/%04d",
                        dayOfMonth,
                        month + 1,
                        year
                    )
                    fechaSeleccionadaIso = String.format(
                        Locale.US,
                        "%04d-%02d-%02d",
                        year,
                        month + 1,
                        dayOfMonth
                    )

                    edadCalculada = calcularEdadExacta(year, month, dayOfMonth)
                    etFechaNacimiento.setText("$fechaVisual ($edadCalculada años)")
                    etFechaNacimiento.error = null
                },
                anioActual - 18,
                mesActual,
                diaActual
            )

            // Evitar seleccionar fechas futuras
            datePicker.datePicker.maxDate = System.currentTimeMillis()
            datePicker.show()
        }

        // BOTÓN REGISTRAR
        btnRegistrar.setOnClickListener {
            val nombre = etNombre.text.toString().trim()
            val apellido = etApellido.text.toString().trim()
            val correo = etCorreo.text.toString().trim()
            val password = etPasswordRegistro.text.toString().trim()
            val universidad = etUniversidad.text.toString().trim()

            // VALIDACIONES
            if (nombre.isBlank()) {
                etNombre.error = "Ingrese el nombre"
                return@setOnClickListener
            }

            if (apellido.isBlank()) {
                etApellido.error = "Ingrese el apellido"
                return@setOnClickListener
            }

            if (correo.isBlank()) {
                etCorreo.error = "Ingrese el correo"
                return@setOnClickListener
            }

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
                etCorreo.error = "Correo no válido"
                return@setOnClickListener
            }

            if (password.length < 6) {
                etPasswordRegistro.error = "La contraseña debe tener al menos 6 caracteres"
                return@setOnClickListener
            }

            if (fechaSeleccionadaIso == null || edadCalculada < 0) {
                etFechaNacimiento.error = "Seleccione su fecha de nacimiento"
                Toast.makeText(this, "Seleccione su fecha de nacimiento", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (universidad.isBlank()) {
                etUniversidad.error = "Ingrese la universidad"
                return@setOnClickListener
            }

            if (spSemestre.selectedItemPosition == 0) {
                Toast.makeText(this, "Seleccione un semestre válido (1 a 10)", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val semestreSeleccionado = spSemestre.selectedItem.toString().toInt()

            val nuevoUsuario = CrearUsuarioRequest(
                nombre = nombre,
                apellido = apellido,
                correo = correo,
                password = password,
                fechaNacimiento = fechaSeleccionadaIso!!,
                universidad = universidad,
                semestre = semestreSeleccionado
            )

            tvMensaje.text = "Registrando usuario..."
            btnRegistrar.isEnabled = false

            // LLAMADA AL BACKEND
            ApiClient.usuarioApi
                .crearUsuario(nuevoUsuario)
                .enqueue(object : Callback<Usuario> {

                    override fun onResponse(
                        call: Call<Usuario>,
                        response: Response<Usuario>
                    ) {
                        btnRegistrar.isEnabled = true

                        if (response.isSuccessful) {
                            tvMensaje.text = ""

                            // 4.2 CONFIRMACIÓN DEL REGISTRO CON ALERTDIALOG
                            AlertDialog.Builder(this@RegistroActivity)
                                .setTitle("Registro exitoso")
                                .setMessage("El usuario fue creado exitosamente.")
                                .setCancelable(false)
                                .setPositiveButton("ACEPTAR") { dialog, _ ->
                                    dialog.dismiss()
                                    finish() // Regresa al Login
                                }
                                .show()

                        } else {
                            // 4.3 MANEJO DE ERRORES CONTROLADOS DEL BACKEND (EJ. HTTP 400 CORREO DUPLICADO)
                            val errorBodyStr = response.errorBody()?.string()
                            var mensajeError = "Error en el registro (${response.code()})"

                            if (!errorBodyStr.isNullOrBlank()) {
                                try {
                                    val jsonObj = JSONObject(errorBodyStr)
                                    if (jsonObj.has("mensaje")) {
                                        mensajeError = jsonObj.getString("mensaje")
                                    }
                                } catch (_: Exception) {
                                }
                            }

                            tvMensaje.text = mensajeError
                            Toast.makeText(this@RegistroActivity, mensajeError, Toast.LENGTH_LONG).show()
                        }
                    }

                    override fun onFailure(
                        call: Call<Usuario>,
                        t: Throwable
                    ) {
                        btnRegistrar.isEnabled = true
                        tvMensaje.text = "Error de conexión: ${t.message}"
                    }
                })
        }
    }

    // Cálculo exacto de edad considerando día, mes y año
    private fun calcularEdadExacta(anioNac: Int, mesNac: Int, diaNac: Int): Int {
        val hoy = Calendar.getInstance()
        val anioActual = hoy.get(Calendar.YEAR)
        val mesActual = hoy.get(Calendar.MONTH)
        val diaActual = hoy.get(Calendar.DAY_OF_MONTH)

        var edad = anioActual - anioNac

        if (mesActual < mesNac || (mesActual == mesNac && diaActual < diaNac)) {
            edad--
        }

        return edad
    }
}