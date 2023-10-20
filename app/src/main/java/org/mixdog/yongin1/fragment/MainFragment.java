package org.mixdog.yongin1.fragment;

import android.os.Bundle;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import org.mixdog.yongin1.R;

public class MainFragment extends Fragment {

    private String param1;
    private String param2;
    private NavController navController;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            param1 = getArguments().getString(param1);
            param2 = getArguments().getString(param2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_main, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        navController = Navigation.findNavController(view);

        // 버튼을 ID로 찾아옵니다.
        Button btnStart = view.findViewById(R.id.btn_start);

        // 버튼에 클릭 리스너를 설정합니다.
        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("mixpuppy", "청소시작 버튼 클릭됨");
                navController.navigate(R.id.action_mainFragment_to_mapFragment);
            }
        });

    }

    public static MainFragment newInstance(String param1, String param2) {
        MainFragment fragment = new MainFragment();
        Bundle args = new Bundle();
        args.putString("param1", param1);
        args.putString("param2", param2);
        fragment.setArguments(args);
        return fragment;
    }
}