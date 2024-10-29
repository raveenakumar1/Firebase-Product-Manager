package com.example.lab5;




import android.os.Bundle;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;

import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    EditText editTextName;
    EditText editTextPrice;
    Button buttonAddProduct;
    ListView listViewProducts;

    List<Product> products;
    DatabaseReference databaseProducts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        databaseProducts = FirebaseDatabase.getInstance().getReference("products");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        editTextName = (EditText) findViewById(R.id.editTextName);
        editTextPrice = (EditText) findViewById(R.id.editTextPrice);
        listViewProducts = (ListView) findViewById(R.id.listViewProducts);
        buttonAddProduct = (Button) findViewById(R.id.addButton);

        products = new ArrayList<>();

        //adding an onclicklistener to button
        buttonAddProduct.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addProduct();
            }
        });

        listViewProducts.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
                Product product = products.get(i);
                showUpdateDeleteDialog(product.getId(), product.getProductName());
                return true;
            }
        });
    }


    @Override
    protected void onStart() {
        super.onStart();

        // attach a listener to database for changes in "products" data
        databaseProducts.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // clear the existing list to avoid duplicate data
                products.clear();

                // iterate through each child of the snapshot (each product)
                for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {
                    // convert each snapshot to a Product object and add to the list
                    products.add(postSnapshot.getValue(Product.class));
                }

                // set up the list view adapter with the updated product list
                listViewProducts.setAdapter(new ProductList(MainActivity.this, products));
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // handle potential errors when accessing database
            }
        });
    }



    private void showUpdateDeleteDialog(final String productId, String productName) {

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.update_dialog, null);
        dialogBuilder.setView(dialogView);

        final EditText editTextName = (EditText) dialogView.findViewById(R.id.editTextName);
        final EditText editTextPrice  = (EditText) dialogView.findViewById(R.id.editTextPrice);
        final Button buttonUpdate = (Button) dialogView.findViewById(R.id.buttonUpdateProduct);
        final Button buttonDelete = (Button) dialogView.findViewById(R.id.buttonDeleteProduct);

        dialogBuilder.setTitle(productName);
        final AlertDialog b = dialogBuilder.create();
        b.show();

        buttonUpdate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String name = editTextName.getText().toString().trim();
                double price = Double.parseDouble(String.valueOf(editTextPrice.getText().toString()));
                if (!TextUtils.isEmpty(name)) {
                    updateProduct(productId, name, price);
                    b.dismiss();
                }
            }
        });

        buttonDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                deleteProduct(productId);
                b.dismiss();
            }
        });
    }

    private void updateProduct(String id, String name, double price) {
        //get a reference to the specific product using its id
        DatabaseReference productRef = databaseProducts.child(id);

        // create a new product object with updated values and save to database
        productRef.setValue(new Product(id, name, price))
                .addOnSuccessListener(aVoid ->
                        // notify user of successful update
                        Toast.makeText(getApplicationContext(), "Product Updated", Toast.LENGTH_LONG).show()
                )
                .addOnFailureListener(e ->
                           //notify user if the update failed
                        Toast.makeText(getApplicationContext(), "Update Failed", Toast.LENGTH_LONG).show()
                );
    }


    private void deleteProduct(String id) {
        //find product in database by id and attempt to remove it
        databaseProducts.child(id).removeValue()
                .addOnSuccessListener(aVoid ->
                            //notify user of successful deletion
                        Toast.makeText(getApplicationContext(), "Product Deleted", Toast.LENGTH_LONG).show()
                )
                .addOnFailureListener(e ->
                        //notify user if deletion failed
                        Toast.makeText(getApplicationContext(), "Deletion Failed", Toast.LENGTH_LONG).show()
                );
    }


    private void addProduct() {
        // get and trim the product name from editTextName input field
        String name = editTextName.getText().toString().trim();

        // check if name field is empty
        if (TextUtils.isEmpty(name)) {
            // prompt user to enter a name if the field is empty
            Toast.makeText(this, "Please enter a name", Toast.LENGTH_LONG).show();
            return;  // exit method if name is empty
        }

        try {
            //attempt to parse the price as a double from editTextPrice field
            double price = Double.parseDouble(editTextPrice.getText().toString());

            //generate a unique id for the new product entry
            String id = databaseProducts.push().getKey();

            // create new product with the given id, name, and price
            Product product = new Product(id, name, price);

            // save new product to database
            databaseProducts.child(id).setValue(product)
                    .addOnSuccessListener(aVoid -> {
                        // clear input fields after successful addition
                        editTextName.setText("");
                        editTextPrice.setText("");
                           //inform user of successful product addition
                        Toast.makeText(this, "Product Added", Toast.LENGTH_LONG).show();
                    })
                    .addOnFailureListener(e ->
                            // inform user if adding product failed
                            Toast.makeText(this, "Failed to Add Product", Toast.LENGTH_LONG).show()
                    );
        } catch (NumberFormatException e) {
            //notify user to enter a valid numeric price if an exception occurs
            Toast.makeText(this, "Please enter a valid price", Toast.LENGTH_LONG).show();
        }
    }

}