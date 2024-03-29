/**
 * @author Lee Teck Junn
 * @version 1.0
 */
package my.edu.utar.groupassignment;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnSuccessListener;

import my.edu.utar.groupassignment.adapter.PlaceAutocompleteAdapter;
import my.edu.utar.groupassignment.adapter.PlaceViewAdapter;
import my.edu.utar.groupassignment.structure.Place;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class TravelActivity extends AppCompatActivity {

    // Member variables declaration
    private RecyclerView.Adapter placeViewAdapter;
    private RecyclerView placeRecyclerView;
    private OkHttpClient client = new OkHttpClient();
    private BoundingBox boundingBox;
    private String category = "restaurants";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_travel);

        // Initialize and set up the AutoCompleteTextView for location input
        AutoCompleteTextView autoCompleteTextView = findViewById(R.id.autocomplete);
        autoCompleteTextView.setAdapter(new PlaceAutocompleteAdapter(TravelActivity.this, android.R.layout.simple_list_item_1));

        // Set an item click listener for the AutoCompleteTextView
        autoCompleteTextView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                // When a location is selected, log its address and calculate a bounding box
                Log.d("Address : ", autoCompleteTextView.getText().toString());
                boundingBox = getBoundingBoxFromAddress(autoCompleteTextView.getText().toString());
                if (boundingBox != null) {

                    // Log the bounding box coordinates and update the view
                    Log.d("Bottom Left: ", "Lat: " + boundingBox.bl_latitude + ", Lng: " + boundingBox.bl_longitude);
                    Log.d("Top Right: ", "Lat: " + boundingBox.tr_latitude + ", Lng: " + boundingBox.tr_longitude);

                    updateView();

                } else {
                    Log.d("Bounding Box:", "Bounding Box not found");
                }
            }
        });

        // Set up a RadioGroup for selecting categories
        RadioGroup radioGroup = findViewById(R.id.categoryGroup);
        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                // Handle category selection when a radio button is checked
                RadioButton selectedRadioButton = findViewById(checkedId);
                category = selectedRadioButton.getText().toString().toLowerCase();
                updateView();
            }
        });

        ImageView return_btn = findViewById(R.id.return_btn2);
        return_btn.setOnClickListener(v -> finish());
    }

    // Method to obtain a bounding box from a given address
    private BoundingBox getBoundingBoxFromAddress(String address) {
        Geocoder geocoder = new Geocoder(TravelActivity.this);
        List<Address> addressList;

        try {
            addressList = geocoder.getFromLocationName(address, 1);
            if (addressList != null && !addressList.isEmpty()) {
                Address singleAddress = addressList.get(0);
                BoundingBox boundingBox = new BoundingBox(
                        singleAddress.getLatitude(),
                        singleAddress.getLongitude()
                );
                return boundingBox;
            } else {
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // Static inner class for representing a bounding box
    private static class BoundingBox {
        double bl_latitude;
        double bl_longitude;
        double tr_latitude;
        double tr_longitude;

        // Constructor to calculate a bounding box based on latitude and longitude
        BoundingBox(double lat, double lng) {
            // Calculate bounding box based on lat and lng
            bl_latitude = lat - 0.01;
            bl_longitude = lng - 0.01;
            tr_latitude = lat + 0.01;
            tr_longitude = lng + 0.01;
        }
    }

    // Method to update the view based on the selected category and bounding box
    private void updateView(){

        // Define parameters based on the selected category
        String parameter = "";
        if (category.equals("hotels"))
            parameter = "&limit=6&currency=USD&subcategory=hotel%2Cbb%2Cspecialty&adults=1";
        else if (category.equals("restaurants"))
            parameter = "&restaurant_tagcategory_standalone=10591&restaurant_tagcategory=10591&limit=6&currency=USD&open_now=false&lunit=km&lang=en_US";
        else if (category.equals("attractions"))
            parameter = "&currency=USD&lunit=km&lang=en_US";

        // Construct the URL for the HTTP request
        String url = "https://travel-advisor.p.rapidapi.com/" + category + "/list-in-boundary?" +
                "bl_latitude=" + boundingBox.bl_latitude +
                "&tr_latitude=" + boundingBox.tr_latitude +
                "&bl_longitude=" + boundingBox.bl_longitude +
                "&tr_longitude=" + boundingBox.tr_longitude + parameter;

        // Create an HTTP request using OkHttp
        Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader("X-RapidAPI-Key", "c413846101msha9d7727f22c02c8p1c4267jsn4dc810f3b020")
                .addHeader("X-RapidAPI-Host", "travel-advisor.p.rapidapi.com")
                .build();

        // Asynchronously execute the HTTP request
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()){

                    // Process the response data and update the UI
                    ArrayList<Place> placeArrayList = new ArrayList<>();

                    try {
                        JSONObject jsonObject = new JSONObject(response.body().string());

                        JSONArray placeArray = jsonObject.getJSONArray("data");

                        for (int i = 0; i < placeArray.length(); i++){
                            JSONObject place = placeArray.getJSONObject(i);

                            if (!place.has("ad_position")){
                                // Extract place information from the JSON response
                                String name = "-";
                                if (place.has("name"))
                                    name = place.getString("name");

                                String rating = "-";
                                if (place.has("rating"))
                                    rating = place.getString("rating");

                                String photo = "";
                                if (place.has("photo"))
                                    photo = place.getJSONObject("photo").getJSONObject("images")
                                            .getJSONObject("original").getString("url");

                                String address = "";
                                if (place.has("address"))
                                    address = place.getString("location_string");

                                String price_level = "";
                                if (place.has("price_level"))
                                    price_level = place.getString("price_level");

                                String description = "";
                                if (place.has("description"))
                                    description = place.getString("description");

                                String web_url = "";
                                if (place.has("web_url"))
                                    web_url = place.getString("web_url");

                                // Create a Place object and add it to the list
                                placeArrayList.add(new Place(name, rating, photo, address, price_level, description, web_url));
                            }
                        }

                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }

                    // Update the UI on the main (UI) thread
                    TravelActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            // Set up the RecyclerView and its adapter to display place information
                            placeRecyclerView = findViewById(R.id.place_view);
                            GridLayoutManager gridLayoutManager = new GridLayoutManager(TravelActivity.this, 2);
                            placeRecyclerView.setLayoutManager(gridLayoutManager);
                            placeViewAdapter = new PlaceViewAdapter(placeArrayList);
                            placeRecyclerView.setAdapter(placeViewAdapter);
                        }
                    });
                }
            }
        });
    }

}
