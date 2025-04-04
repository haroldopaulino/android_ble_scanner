package com.noke.ble_scanner

import android.app.AlertDialog
import android.content.Context
import android.widget.Toast

class Utils {
    fun showAlert(context: Context, title: String, message: String, function: () -> Unit) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle(title)
        builder.setMessage(message)

        builder.setPositiveButton(android.R.string.ok) { _, _ ->
            Toast.makeText(context,
                android.R.string.yes, Toast.LENGTH_SHORT).show()
        }
        builder.show()
    }
}