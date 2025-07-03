package com.example.recipeapp

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.google.firebase.Firebase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database

class FirebaseApiManager {

    inline fun <reified T> loadDataFromFirebase(
        path: String,
        crossinline onSuccess: (T?) -> Unit,
        crossinline onError: (DatabaseError) -> Unit
    ) {
        FirebaseDatabase.getInstance()
            .getReference(path)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val data = snapshot.getValue(T::class.java)
                    onSuccess(data)
                }
                override fun onCancelled(error: DatabaseError) {
                    onError(error)
                }
            })
    }

    fun <T> saveDataToFirebase(
        path: String,
        data: T,
        onSuccess: () -> Unit,
        onError: (DatabaseError) -> Unit
    ) {
        FirebaseDatabase.getInstance()
            .getReference(path)
            .setValue(data)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener {
                if (it is DatabaseError) {
                    onError(it)
                } else {
                    Log.e("FirebaseApi", "Unknown error", it)
                }
            }
    }

    inline fun <reified T> loadListFromFirebase(
        dbPath: String,
        crossinline onLoaded: (List<T>) -> Unit,
        crossinline onError: (DatabaseError) -> Unit
    ) {
        val ref = FirebaseDatabase.getInstance()
            .getReference(dbPath)

        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val itemList = mutableListOf<T>()
                for (ds in snapshot.children) {
                    val item = ds.getValue(T::class.java)
                    if (item != null) itemList.add(item)
                }
                onLoaded(itemList)
            }

            override fun onCancelled(error: DatabaseError) {
                onError(error)
            }
        })
    }
    fun deleteByFieldValue(
        path: String,
        field: String,
        value: String,
        onSuccess: (Int) -> Unit = {},
        onError: (Exception) -> Unit = {}
    ) {
        val dbRef = FirebaseDatabase.getInstance().getReference(path)
        dbRef.orderByChild(field).equalTo(value)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var count = 0
                    for (child in snapshot.children) {
                        child.ref.removeValue()
                        count++
                    }
                    onSuccess(count)
                }

                override fun onCancelled(error: DatabaseError) {
                    onError(error.toException())
                }
            })
    }

    fun insertByKey(
        path: String,
        key: String,
        data: Any,
        onSuccess: () -> Unit = {},
        onError: (Exception) -> Unit = {}
    ) {
        val dbRef = FirebaseDatabase.getInstance().getReference(path).child(key)
        dbRef.setValue(data)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it) }
    }




}