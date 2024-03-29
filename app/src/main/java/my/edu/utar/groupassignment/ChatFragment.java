//code by thong wei xin
package my.edu.utar.groupassignment;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

import my.edu.utar.groupassignment.adapter.UserAdapter;


public class ChatFragment extends Fragment {
    private DatabaseReference dbReference;
    private SearchView searchChat;
    private ArrayList<HelperClass> dataList;
    private UserAdapter userAdapter;
    private RecyclerView recyclerView;
    private LinearLayout chatBot;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_chat, container, false);

        searchChat = rootView.findViewById(R.id.searchChat);
        searchChat.clearFocus();
        //search user
        searchChat.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterList(newText);
                return true;
            }
        });


        recyclerView = rootView.findViewById(R.id.userRecycler);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        dataList = new ArrayList<>();

        userAdapter = new UserAdapter(requireContext(), dataList);
        recyclerView.setAdapter(userAdapter);


        dbReference = FirebaseDatabase.getInstance().getReference("Users");
        //load user list
        dbReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                userAdapter.clear();
                //loop through all data found
                for(DataSnapshot dataSnap:snapshot.getChildren()){
                    String UID = dataSnap.getKey();
                    if(!UID.equals(FirebaseAuth.getInstance().getUid())){
                        HelperClass userModel = dataSnap.getValue(HelperClass.class);
                        userAdapter.add(userModel);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        chatBot = rootView.findViewById(R.id.chatBot);
        chatBot.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getContext(), ChatBot.class);
                startActivity(intent);
            }
        });

        return rootView;
    }
    //filter  by dat enter
    private void filterList(String text) {
        ArrayList<HelperClass> filteredList = new ArrayList<>();
        for(HelperClass item : dataList){
            if(item.getName().toLowerCase().contains(text.toLowerCase())){
                filteredList.add(item);
            }
        }

        //no user found
        if(filteredList.isEmpty()){
            Toast.makeText(getContext(), "No user found", Toast.LENGTH_SHORT).show();
        }
        else{
            userAdapter.setFilteredList(filteredList);
        }
    }

    public UserAdapter getUserAdapter(){
        return userAdapter;
    }
}