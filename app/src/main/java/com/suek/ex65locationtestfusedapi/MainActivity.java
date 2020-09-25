package com.suek.ex65locationtestfusedapi;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

public class MainActivity extends AppCompatActivity {

    // Google Fused Location 라이브러리 : 적절한 위치정보제공자를 선정하여 위치정보자를 제공 (안드로이드 라이브러리 아님)

    // 외부 라이브러리 추가 : play-services 라이브러리 검색, 근데 너무 무거워서..명시적으로 play-services-location 버전만 받기..
    // 디바이스에 Google Play Store 앱이 없으면 실행 안됨

    GoogleApiClient googleApiClient;  //위치정보관리 객체(LocationManager 역할)
    FusedLocationProviderClient providerClient;   //위치정보제공자 객체(알아서 적절한 위치정보제공자를 선택-gps..etc)


    TextView tv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tv= findViewById(R.id.tv);

        //위치정보제공을 받기위한 퍼미션 작업 추가..
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.M){
            //동적퍼미션 받기
            int permissionResult= checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION);
            if(permissionResult != PackageManager.PERMISSION_GRANTED){
                String[] permissions= new String[]{Manifest.permission.ACCESS_FINE_LOCATION};
                requestPermissions(permissions, 10);    //그리고 나서 다이얼로그가 뜸..
            }
        }
    }//onCreate method

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode){
            case 10:
                if(grantResults[0] == PackageManager.PERMISSION_DENIED){
                    Toast.makeText(this, "위치정보 사용을 거부 하셨습니다.\n사용자의 위치탐색 기능이 제한됩니다.", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }




    public void clickBtn(View view) {
        //이제.. 버튼을 눌렀을대 Fused API 를 이용해서 내 위치 정보 얻어오기(실시간 갱신)

        //위치정보관리 객체 생성을 위해 빌더객체 생성(마치 다이얼로그빌더처럼)
        GoogleApiClient.Builder builder= new GoogleApiClient.Builder(this);
        //1. 구글 API 사용 키 설정
        builder.addApi(LocationServices.API);    //LocationServices- 라이브러리 공용키/공개키 -(키를 발급받은 것고 같음)
        //2. 위치정보 연결 성공 리스너
        builder.addConnectionCallbacks(connectionCallbacks);
        //3. 위치정보 연결 실패 리스너
        builder.addOnConnectionFailedListener(connectionFailedListener);


        //위치정보 관리 객체 생성
        googleApiClient= builder.build();

        //준비가 되었으니 위치정보 취득을 위한 연결시도!!
        googleApiClient.connect();    //만약 이 연결이 성공하면 connectionCallbacks 리스너 객체의 onConnect() 메소드가 실행

        //위치정보 제공자 객체 얻어오기
        providerClient= LocationServices.getFusedLocationProviderClient(this);
    }



    //위치정보를 얻기위한 연결시도에 [성공]하였는지 듣는 리스너 객체
    GoogleApiClient.ConnectionCallbacks connectionCallbacks= new GoogleApiClient.ConnectionCallbacks() {
        @Override
        public void onConnected(@Nullable Bundle bundle) {
            //연결에 성공했을 때...(아직 위치정보를 얻을 수 있는것은 아니고, 얻을 수 있는 상태)
            Toast.makeText(MainActivity.this, "위치정보 탐색을 시작할 수 있습니다.", Toast.LENGTH_SHORT).show();

            //위치정보제공자 객체에게 최적의 제공자를 선택하는 기준 설정
            LocationRequest locationRequest= LocationRequest.create();   //new 대신에
            locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);  //높은 정확도 제공자를 우선시....
            locationRequest.setInterval(5000);   //위치정보 탐색 주기 : 5초마다

            //위치정보제공자 객체에게 실시간 위치정보를 요청
            providerClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper() );    //별도 쓰레드라 Looper 가 필요함
        }

        @Override
        public void onConnectionSuspended(int i) {
            //연결을 유예했을 때 suspend...

        }
    };



    //위치정보를 얻기위한 연결시도에 [실패]하였는지 듣는 리스너 객체
    GoogleApiClient.OnConnectionFailedListener connectionFailedListener= new GoogleApiClient.OnConnectionFailedListener() {
        @Override
        public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
            Toast.makeText(MainActivity.this, "위치정보 탐색을 시작 할 수 없습니다.", Toast.LENGTH_SHORT).show();

        }
    };


    //위치정보가 갱신되는 것을 듣는 리스너
    LocationCallback locationCallback= new LocationCallback(){
        //위치정보 결과를 받았을때 호출되는 메소드- onLocationResult (그 안에 정보가 있음)
        @Override
        public void onLocationResult(LocationResult locationResult) {

            Location location= locationResult.getLastLocation();
            double latitude= location.getLatitude();
            double longitude= location.getLongitude();

            tv.setText(latitude+", "+longitude);

            super.onLocationResult(locationResult);
        }
    };

    //액티비티가 화면에 보이지 않으면 위치정보를 더이상 갱신하지 않도록
    //화면이 안보일때 노출
    @Override
    protected void onPause() {
        super.onPause();

        if(providerClient!=null)  providerClient.removeLocationUpdates(locationCallback);
    }
}
