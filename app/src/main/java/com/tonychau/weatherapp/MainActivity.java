package com.tonychau.weatherapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private RelativeLayout homeRL;
    private ProgressBar loadingPB;
    private TextView cityNameTV, temperatureTV, conditionTV;
    private RecyclerView weatherRV;
    private TextInputEditText cityEdt;
    private ImageView backIV, iconIV, searchIV;
    private LocationManager locationManager;
    private int PERMISSION_CODE = 1;

    private ArrayList<WeatherRVModal> weatherRVModalArrayList;
    private WeatherRVAdapter weatherRVAdapter;

    private String cityName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // FUll screen
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);

        homeRL = findViewById(R.id.idRLHome);
        loadingPB = findViewById(R.id.idPBLoading);
        cityNameTV = findViewById(R.id.idTVCityName);
        temperatureTV = findViewById(R.id.idTVTempature);
        conditionTV = findViewById(R.id.idTVCondition);
        weatherRV = findViewById(R.id.idRvWeather);
        cityEdt = findViewById(R.id.idEdtCity);
        backIV = findViewById(R.id.idIVBack);
        iconIV = findViewById(R.id.idIVIcon);
        searchIV = findViewById(R.id.idIVSearch);

        weatherRVModalArrayList = new ArrayList<>();
        weatherRVAdapter = new WeatherRVAdapter(this, weatherRVModalArrayList);
        weatherRV.setAdapter(weatherRVAdapter);

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_CODE);
        }

        Location location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

        cityName = getCityName(location.getLongitude(), location.getLatitude());
        getWeatherInfo(cityName);

        searchIV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String city = cityEdt.getText().toString();
                if(city.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Please enter city name", Toast.LENGTH_SHORT).show();
                } else {
                    cityNameTV.setText(cityName);
                    getWeatherInfo(city);
                }
            }
        });

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == PERMISSION_CODE) {
            if(grantResults.length>0 && grantResults[0]==PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission granted..", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Please provide the permissions.", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private String getCityName(double longitude, double latitude) {
        String cityName = "Not Found";
        Geocoder gcd = new Geocoder(getBaseContext(), Locale.getDefault());
        try{
            List<Address> addresses = gcd.getFromLocation(latitude, longitude, 10);
            for(Address adr: addresses) {
                if(adr != null) {
                    String city = adr.getLocality();
                    if(city != null && !city.equals("")) {
                        cityName = city;
                    } else {
                        Log.d("TAG", "CITY NOT FOUND!");
                        Toast.makeText(this, "User City Not Found...", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return cityName;
    }

    private void getWeatherInfo(String cityName) {
        String url = "http://api.weatherapi.com/v1/forecast.json?key=338f0c61c6314542953230452210311&q=" + cityName +"&days=1&aqi=no&alerts=no";
        cityNameTV.setText(cityName);

        RequestQueue requestQueue = Volley.newRequestQueue(MainActivity.this);

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                loadingPB.setVisibility(View.GONE);
                homeRL.setVisibility(View.VISIBLE);
                weatherRVModalArrayList.clear();
                try{
                    String temperature = response.getJSONObject("current").getString("temp_c");
                    temperatureTV.setText(temperature + " C");
                    int isDay = response.getJSONObject("current").getInt("is_day");
                    String condition = response.getJSONObject("current").getString("text");
                    String conditionIcon = response.getJSONObject("current").getString("icon");
                    Picasso.get().load("http:".concat(conditionIcon)).into(iconIV);
                    conditionTV.setText(condition);

//                    if(isDay == 1){
//                        Picasso.get().load("data:image/jpeg;base64,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").into(backIV);
//                    } else {
//                        Picasso.get().load("data:image/jpeg;base64,/9j/4AAQSkZJRgABAQAAAQABAAD/2wCEAAoHCBYWFRgWFhYZGBgaGBwaHBwcGhweGhwaGBkaGRwaHBocIS4lHB4rIRgYJjgmKy8xNTU1GiQ7QDs0Py40NTEBDAwMEA8QHhISHz8rJCs0NDQ0NDQxNjQ0NDE0NDQ0NDQ0NDQ0NDQ0NDQ0NDQ0NDQ0NDQ0NDQ0NDQ0NDQ0NDQ0NP/AABEIAK4BIgMBIgACEQEDEQH/xAAcAAACAwEBAQEAAAAAAAAAAAAEBQIDBgEABwj/xAA6EAABAwIFAgMHAwMEAgMBAAABAAIRAyEEBRIxQVFhInGBBhMykaGx0ULB8BRS4RVikvFygiNTohb/xAAZAQADAQEBAAAAAAAAAAAAAAABAgMEAAX/xAAoEQACAgICAQQCAwEBAQAAAAAAAQIRAyESMVEEEyJBcYEyUrGRYRT/2gAMAwEAAhEDEQA/AMlifHTBMamWO20/e6WQn+OwYDSQJIEz1HHqkwgG4Xsx2eVK0ykBSDF0qQtsnoWzzqREEiJUQxNaGKBbp0A+fVEYbLj8boAuQB+Ut12FK+hdhMKx5A1aesp2PZ7WQGvDR03v89lRSw1Oo+GkseZgxLZ4mNle7+soNcdEAWLrERwRfZLJv6Y8UktqwmlkjGN/+Rg1cGbHyPC7WyuGt0AEjc9B2S/A4+s8kv1PaOy0lOu4NswjjhI7XZWNNaQoZRbrYGmCPUG23mtdg2BoEdB2Wbw1Al4LWHVqn0W0wOFkAu35Hkp5GUgio1AHD9J2+V0ZS1F37IV+XOdUMSG2v37fRNmUmsvNxv6KEmiiRS8n4dgrcO+8DhBuxQeTBXsLVaXRNv3QrQbHDXyuOautfay9qskGIvKo0G6IAXXWXHCas8AnXwouYD8IRuKYx1yhdAdYEgp0xaAq2HcD1JNgOEsr03mQ6QAfnC0vui3a6SZnXcbafRPF2LJCXF4Rj2BhOnUZBCxGbjQ8sAiLE9ehW2x/hZN7C4Kw+PdrdqnstONGfKxeUzy/Cl4LnOIgiG8nuCVXhctc8i4EmBe55+S2OGybwCQRHe5jqjKSQsI2ZfE4QnT8TQZLu44U8vgAi4aTIJ3X0Cll4c2SyYsJFoWS9oWim42ABO/PokjLloeUeOyirSa9rjImI0jqlLW6X9zaIlMcvpFwlpguvfgd+6MNNjHB0+Z5JTdC1ewFmTgnU8k9gF7EZdTEwIjvsisRnDSSARHHn5ISpR1jU9+ln3Q39na+hFVbLoYCfK6tNNrBLwZ4H+FZica0DSwANHPJjkpZUeSZM+qajkrC/wCtH9v2/C8g/du6H5Ly6g0j6E/HUW6msZDCdzB+6S50WODSwRFvMd/5yhhTe7j08kM4FPGFMg8jaKoXtKlCuouA3AIVKJ2Qpuc2DePoUfhseRubdP3VLnwLNIB3nYqmnQLnADk2JQq+wptPRrMtrtYz3h3IgW47ALWZfhves1VLgizSEjyCC1klrnNAbZu3ktlRe0WBFt+yxZXvRvxrRnPaLDtp0gWNADSNrSk3s9nTn1Cxwb2HMTcd0X7a4jWW02ua1o8bjNyRYD6n6LMZVg3OrtFI3F9XEcyOvrdVhFOHyJSk1NUfTsE1hdZonlOaTRFxCRZNQawOOrU6b3PHWfVONcjsFkn2aovR1+loMGLFZbFe0GhzmkRxfc9/JPMbi2aXzIAbwJJ8uq+f5pg3veXgaQTEWIsLABvJifVUxQT/AJE8kmujU4DENe06dzz+VdldM63FwO8A9esLM5e6oxoJ/VaBYgz0lbDLniAHOGqB/AmnGro6LvscsbbdccIVNItne/ZEPIAWcsVsdaUNVxdyOivc2yBq0/lN0UkBg2Lxf9viPRUUcTUkOOnyi/p1XcbUaGn4WmOeVnMVmJptkuDn/pA58+ytGFrRNyrs0eKzB3fz/CSVMzqsJLhLepE/JDMzqoaYeWMI/wDK4vEQeUvxNfEPZJADXHysOgPn9E8YV2TlO+i7M8b7xhkt2m5vPCy9DC6iA4geIDbqfrZbBmS0qzG6HRfxCbxynOCySm1rRoFiI6z1nlN7kYqkB43J2xHl/s09jw+ZGmB5lbHDZd4ZcLx9Uyo4QACbwvVaTiIGyzSyOReMFHoV4l+lsTsvlHtLmGuqR/YSO1l9NzDCv/Td30hfLs4wNT3rtQ8RPoBwr4UuyWW6KDmTogWMcIV2NedzI2VdakWmDuqitNIzng6+8Ltau50SdtlErhC6hkQhW06Z33UFMPdwhQXZP3Dui6qJXl1ApmwqUH4YkaR5/uAVN+F1EOcAZEkW55X0jHZOyqIeJ6Hkd0rq+y9MbNv/AHTB9Y3UFni++xnga66PnWNw4bbTB/nzV+WYVhaXVBttvfzWyxPs857SAAY2nckDqsrmORV6UlzSG9bwrxyRkqsjKEou6tBD8ZhtBaWSCb38XhiADwPykrcSQfALDbqOyrfSI3CvDwGw23BvM/PZUUUiTm33oeey2YtYHufuXWk2jmPVOsTmwIlrgXXtMx+VisHlz6gcWCdIk+vREYrL3UmtcT4ySNIvbeZU5Y4OXeyscslHrQRmuF1t94CXPcfFw0gdL7yg8txjqbvBNxG1049nqYc3xtLmXOnqZG56CFq6OFpkDwNLOsXBE7/ZLKaj8WrHjj5VJOhKzMfcsA1S9wJgTJ9Ot0LUz7Eue+nTDfCJJP6YFxJt2XvaHMaQLwxpDh4QYkAGJg+iy1V7mkwSNQBMHeb7owxqStr/AKDJkadJ/wDA5+dVNGjU4u1El+o8niDBT72YzCGgPI5N48Xr12WdzPDsa2m5hEFsf7iQdz3ujsooRSc6JdwCDp6EzHn8k8oxcRYSal+jSY+kyoJY4NebNI4OwFt/+0RkuXv1DUC4ixcZsRtb1KWezxBc8vPiaYAA+G3HSxK2eUU6oIlo0QSTzPCyZG4qjXBKWxg2kANrqL7q+u9CVqkA9lmWy7I4ipA3hY3OM6e1xAgAfyStBVfqaTJCy+LycND3Pe1ziZbIv5XMStGKMU9kcjdaBDiXvGt8u/8AUgAHcXuQisBhnYghlOnqiJe4+GJh3qBNlTh85pseCRYNvaZPl1t9VrKGZa2N93DA4WsOeqpNuPSJxqX2FM9ncMwg6BqAAG8eHttKhnGDYWGRHSBtGyJZhn2LnXjgrrw8mHNt1WXk7uzRSroyOGw1dlRugjRF562Wry+mY8e/8hWVMIwCwUcIyOvqjKXJAjGhmHwFW+vZQe9vVCV3qaQ7YHmlchpi3dfP8Zm7S5weJc2QbQZB6x5Lc4hj3HoOqy2d+zYeHOYYeTPZacTitMhk5NaMLii1xJG5JJnuhiEfjcuqMMOafOEEWrYq+jLtdlZC5CsLVIfNGjrK/dFdBgEKb6hNuFXCFBu+yuF5WQuLqDZ+jnsUNKKY8EKt7L2Xkm6jzGBSqMDhpIBB36LgarGU1wRFjvZyk9vwC1xHHFlin+yr21oNJ76c209P9x4hfV2sU9A2Vo55RJywxlTZkMswFNhhrAwn9JaJt9wq87y5z2EMYJneJtz5LXVKTZBgSNj/AJUWsB3sl9x3yC8aqj4vi8FXwzwHwzVdu+kibgH5WUHZtWDQ3Zo6D9/VfXcbklOqfHDhEaTt5x1Wbzr2DY4OdSdoMEhu7Sf2WyHqISrkjJP0843xZ85e99RwB8R/n4VVUDYGf2Uy1zXEXDmkg9QRYpk3FUnsLKjQHj4XtAm2wPbzWv8AC0Y1vt7Ei0vssGC73kAGdJPh9VnXMhEYRji6GiYvHWPJCceUaDjlxkmfXMudT1RpA9ITp1QDZfMMJ7XaQ1rmbRcfZP8AEe2dNtiD5cwvOngnfR6Uc8Gux9jce1vMLLYn2tAcWFhj+4c9hZA5hn9N5ad2n6diEiqYljnjRLRMkfidlXHg/siWTP8A1Zof9cL2kMAaedRJcB9vRJMbjiXkzrbtfYTzZD08eC7TZok/wlNsNjaJYGFoDZINvET1VeHH6JqfL7M9VxrjY3jbrHELYeyVcvDGyCReJvA/yk1bC0X/AAAyR5DfdUNLqUOaDLLahaOJtvb7oySlGkLG4y5N2j6c/FFjZcIPZZTF+3AGoBhEEhZrHe0VV7QyTb9RNyk1Zr7FwN7hJD0yX8ik/Ut/xNKfbmqTdojoPoime3PVpA7XPCxEKTGAmCdPnMfRUeGHgks0/Jsq/tkD12tG09xuqXe2jgBAkzeeizNbAPb0cOrSChnUyLELlhh4C80/tn03KfaqjVhpOh54PXoE4dUZvIXy72bwzH1fGY0iRJi4WhzrNqLGQxwe/sbeqzzwrlUTRDK+NsZ5xiqbWuJgwNrLAZnUY50tAB7bKvE4x7/iNkNC0Qx8SE8nIrIUYVpauEKlCplUL0KzSuQhR1kNK6pQvLqOs/QOCrbSmYiEqbTg2RTKhC8hnophVl1phD6yVNrygNZeai8Hyh3kKLKkLqOsLa3krrm2VTKqn7+EDisAhdrNcAve8C8+pKJwkdk2Ge5z30GOcTckXsIWR9rfZRlNrq9Ew2RLeBNrHz4X0tlMAbboTFYJjgQRY9dlbHmlGV2RyYYyjVHwotU6dRzPhJE/XsvoPtP7OM92XsDWvbeQI1dj9FlKOR1HM1y0dp25uF6cc0ZRs82eGcJUtimo/VxB7fhOqeFZiGN0vIrhsBpNjp/wlFSkW7hRaSDIJBGxCdxtaZNTp7RPE4N7DpeIPmI+apFM9EQS95u4uPcozCZeTGpvl0QbpbClyfxWhSWEbovDYZ7rNBIveDH/AGtLh8pDgA5gjaZtHoU8wzGABgAa0fyylLMl0jRD07u2xPlXs457AXP0k3neBbryq8ywnupZJI0zqPUm8zbjZPC8VQWM1MaDBIJBttBGwWVzWhoe5hc99/CwmQCefNSg25bf6LTSjHSM7XZDjsfLZaLL6g0S5mpwFjYzaIsg25PWe3V7p0T0PPYXhWYPKq5dDG1NwJILWjgkg7q8nFrszQUlK6IvwetweAWu/thtvkl+aNOrxACIFh87/t3WyoezDgyXVCX7iAYB8uVzFZU0MLXhpPBuJ+qlHLG/JeWGTXg+eteRsYRDce9otA62BJ85XMTh9Lo3HHdVtouJgAk+S0UmZk6ZF9Vzjc/z0UqWHe8gNaT6JrlmWOD/ABsPWCLFavDVGiwYApTycekWhj5bZlsDkNSoNTxDRMAdugQ+KyZ8kjSB0WwxubNYCHEDyN/olrqgfckRx3U4zl2+h5QitLsyVXAvbxPldDlq09eu0E6SAR1Fj6pJinOe6bHyV4tvsjKl0wGF6FboKiWpqFsrheU4XkKDZ99arGMQIJ/SVw4hwsvI4np2MjAUPepccaeVz+qlDgzuQzNSQg3VCCqmYhWCsOiNUdZ0YuF12OCCxAalj3OmydRTFcqHzMV3RLMUs0yo4IlmIJXOJykaRmIRTHgi6zuGrmd0yZUI7qco0OpF2LwrTMiZWexzaLGhri03538rbrS0zrFylOOyBmsPBNiDE2kc9imhJJ7YJJtaMnisla9upjAWO3MxH/r++6nS9kmkA2sbidwtS/DON2wG8jr5r2NcGt8I26K/vy6TIvBHtow2NyVtN3h1dxYj6IrKqRM+8NhsTFvVG5gHGHtkuHANj+UD/WuECo0772gK3KUokuMYy1r/AAa0sQHEsYNuSI3vaQpvwnKtwJYY0kGyOfQJErPKVMulaEGKxhpMLWt0l27t44n5JdlWCc9+vS58uiXCBM79hCZ4/CaZe6YANpsT6pW/NTAaCYJ/SS0N7bXV4puOiMmlLZt8I0atNoA/kI92FG8pHgMU1rB+p0TtLoOxMbIupRe4Auc5s/pb+eqySW9mpPQw902OqQ+0WB1sIaJdwL/VOsE4aYg+qIeWhLGTjKwtKSo+JY3BPZ4niLkDpINwFChmL2CGED06r67jsuoVIL2NdG0gGJSLMvZrDkHSxoPa32W+HqYyVNGGXpXF3FmBbm9UEnVJ78eSrq5lVcZ1kfRMcfh6VI6QA49Zn6BJnBaYxi9pGaUpRdNh+BoU51vqBxOwgm/JMppgG0nuIaQ0efiMfYLMwmOVubq8RiYG3HmhOGm7GjkppUa8ZDSe28yf9xhB1MmpUmOAdc8njyKtrZixjdIfJHUj5JHmlGrUAeZ035WaMZt7ejROUEtK2AZm1kNDJkfFPJ6paQjKtRxAB42QxatcVSMblbK9IXlP3Y6LyNB5G3yjN3scASCNoP5K0jMWx/6h5rJ4Wp4pcAZO/N+iaCuxp0lhA6zH0WPLjTekacORqO2PvdkCdwogBVUswYRpmOgNl7WDss3Frs1Wn0FMpA8lXOoACQSh2FXg90jGQK5DOp3kJg9reQqajhwEyA0UsYrxSnhUBytY9E5UEU6IG26m6s6V7DuH/ajXx7ADYGOhH7pabYdINw9cjdFB4dZJcNUD/EwmJRlOq0fqv3SSjsZSDvdaQlmJabop2JnlUPYH7ldHXZz2IX1Ax2kkfhXf0zHC4B6eqLxmEDbuAIG8xCyVfMXSQLDYGbgT1C1Qi5/xM85KH8h9gabQdOxHZPqNxuvnz8e+Z1kuBEHiOnfhXjNqrjqaQ2N77+hKeXp5PdiR9RFaoeZk01SWyRBiINz18oVWGyBmpuq5F52F+ICJy3MGVGgPI186ZKeUWi0KMpyh8ei0Yxl8uy3DYVrGwArns4XTWgWCW18wM2WdWy9pBNYaeUvrYruoh5cZcTHRDYlw4VIxEcijMs2FJhIOp/RJsTnXvKb9MzEHjjhUZrlNZ7pEuHcgDyCz9ek5pLHWg7Tzst+LDBrT2YM+ecZdaKqodHiPzuVQWq7QuaFsowcinQugQrS1c0oUHkRZvJnf1T0Zq2Gs/SRB6/XdJS0grkJZQT7Hjlceg/GCiNmlzt9xp+iHDxphjbnz+nKpLT0V1ItF9Tw7qI/KXjSO5tvwR/o3dHfL/K4j/wCsb/8AZU/4tXkty8D8YeQ+g6DtPYpg/EhzYIQYYrA1JKKbsaMmlQbhSwXIE95+aNZitRho1eQMpQ1qaZNihTcSRMiAel/58lGcdX2aMc9pdDlmEfAJETx081ezCG82twq/6sPEjZWYbEAEyVjfI2Kj1HL3kS4gdB+VaMua47k+kBWUcXLoGx4U6r3DZK5SsakBZrlelhcz4heCdxGwjlA4nK6raYeSNUSWcgRJvO/Zadj2uaCRePqEszzEQwuIB8BtO/8AhNCUrSFnFU2ZJmIcTqG4HT+Qga9UuK4XkJ3kvs06u0vcdDT8JsZgkOlvReg3DGuUjB85vjEVZfUrFwbRLy4/pb/n7rS4vDV6dMOqU21SLnSZLRHNp9QnGQZAMPqdIe91tUEeG3hiTyJTOs+8DdYsvqFKfxWv9NmLDKMPk9/4fMn4+Xamamf+0j5FEU8xqfpq36Fu60GO9khUe57X6ASS4G9zckdOUgzXJ2UnxrOmLEiZcNxZaYTxSpLv8GeUcsdvr8gmIr1ah0vfAPBsFZQyAuEl7R23P3CAcSLbjyurw1mi4g9Qb/8AEqzi0vi6/RFSUn8lf7L8Z7Plo8D2u+/3KBoZW4Ol7TpHS5PyUXAj4XmPUfRWUMQ9pnWfujU0u7/Qt43Lqv3of4BrabToY6CZ+E/uptzB5dZoHm68eUJR/qzzsTbqbH0hSw+IM6ywg9QRH/ElQeJ7cjTHNHSizQsx+tslxHEQqXvnZrj6Ql9LNmM2BdNzJhFUfaJkwW6fNSeKS6RZZYvthRZpEuEIDE4wDYX6mDCLqZlTeJLg7sAT9ISHHYkCzGOcT/c2B6I44NvaBknFK0wTM8VVmPei/A4+QslmLwrWtB1lzjuP4VypqcSSPkPwqwwyvRhDilR5OTNybtfiyki2y5pRDwTv9oUQxUM7ZRpXNKIdSMSRH86KstXBuirSui21lPSvaVx3I65wIuXE+dlTCtLUdl2U1K0+7AsQDJje8+VkknGCtsePKTpK2LNK8tD/APytb/b/APr8Lyn7+PyU9jL/AFZuB7NUTYavmq8blWGpth0h3QOOo+hMJpSrReRO4E3v0CAzDCMquBMh3JESfNeRHJO9tntSxxS0kJGYBjnQx5g7aony3umtHJ2NbBaXHqXEfQWXcLlzGbku5HCOdXJ6J5ZZPSYscUVuhS7CGnebLjajeSjsS4OLWOPr3/dC4rAaQTMrk77C1XRQzFQZCJOYONgEpdIVjHwncUKpMYVMY/aVDE1tbA0kO7fVBuxAROXVW62l2y6q2dyvRZgcmYHMc9pLTuDED91p20wAAwANAgAecrlJzb2VFeqRsoTm5vZWMYxWi6niXGRIAHJ4UBigbbmdx3SyrUmYt2RGXUT8bj4ZsI36ocUlY3JvQe1gcIkj9vmkOesZoe1xtMtMSdYBiPr8ytA+qx3a+6zWfYVhbrDi50gAahDR5Rv6qmHclYmb+DoypBUSxF+7XCxepZ5TiDMozyB5lXNwTbzUaB6/ZdLFwsXNt9MKpdoofT0Gxa6el1BrVeaa77tGxaKHBGUsKKuhrAGv+E9DA+KP5su0MC98ljC4DeB/JPZGZO8scTIbpHIP6rEW8vopznSddopCFupdMXV8KaVXS+bf2mJnaJRVXFNLNDWPiLzv3vwFsWMa/QXtBIMtdG0X36KePxADTNxBBt6cLJ/9FtWt/k2exSdPX4PmVdgJ8IIHQmVV7pNsThS0mwixteAdrob3a3xmq0ebPHvYE5k8BQ92j/drwozwm5k3jsXOoqs0loMJk9Wr8DHOHXZv/I2Q2IwDmkh7HNi1wQgs0bq9nP07q6E/u17QeiY+6Ufdp+ZP2i3JMmdWeJadAPiMgDrE/jqvpOXYGjRZoY3SLk3JJJ7m54WBy6idXh1Nd1DonqBstZgalQsMzA4JEx18vVeb6pyk+9eD1fRQjGN1vyN/eHv8v8LqV/6qOv3Xlj4vwbuSBH0iBLgLcrzsGw+OXB3Y89eyEpPnn6q0VCNtu/5VdktBfvTFyNul0PUxB3BEdlB1SbEC6XVQRMIxidKQY7F+ME3hWVsUXt3JEpI6oUXRqeGyo4pCKVllRDOcV59e91A1QU6RNsm0XTTKNGsSYi/ySoFSp1dJQlG1QYyp2bljxxBVWKghJ8vxwiD06qyvjIsFl4NM08k0SayXAEwOqOxGLaxgawzH7pM+qUN7wzdU4cuxeVBhrE8rmIykuIIeJdeCdha8hD0Ko1XFk1rYxkANa3b1k2+yb5RfxEpSWwOnkLju9gExNyT3A+anmuXU2NGgbG5m5t358kQ1riyW3g3mdlLKqgDzrg9BE/fYrvdndt9fQfajVJdmcdQIMR9LoihlT3ODQ0tmbuBAtveP5K2/9Q29hPXlQ9/I34XP1UvpAXpY3tmO/wBHfrDHOaJG8mPKw3U2ZA8uLZbAPxXg+Vk/bhyS6CD0NkPinuYQ0njjuu9+b0jvYiuynC4d1JmkuBGonw9wOvNkDTwBc8NYDd+qTtANp7iUZicOQRJExIv/ACEflzWlsl3ijslc3FX5G4J68DBuHvsYjckft/hJsbRhxDXO3PPXjyUsbjC0xyPrCUvzHxcjm4SQjLseUkNmUS5gYWS0/t/0hcTkAfGhwaR+k973KtGanR4W9hB5XcHjA/SDIMwTN/rsipTW1oDjCSpi3H+z7mfAS+BLhEEdxe6b5Fl9Ok3W8t1kT/49gesbruLzLRLCCeATygviGoG/7bJpTnKNNiRxwjK0jTYfFAiw/nK5XdvqGpsdojmx3QWEb4BB23PXyUMU5xkatKz1s0fQgzXAU3O0UWeKZtYR08Rj5I+p7OUnNEeAgAGL3je6jRwsSXOAIkg6pO17D+XQ9XHPB5I2krRzk6UX0Z/bgm212cp5RTpPbLnuIF4sJI+cborEYWmWgglpHqSfIoduK1CSHQLB3E91CljwwmSB9UG5S2+xkoxVLor/AKfu76LiK/1Cn1p/8R+VxdcvAfiKqWLaIkD1urX49hEWnzSl+DgWcfW6qGFMTI+St7aezPzktDgVwL6vnBQ1XFid0r0gGDPorf6aRIPzR4Jdi82+i9+IBUW14VTcIeY+qubg28z6H8ptI5WynEPnZDsqOHE/VNhgmcT6qurgWx08kVJAcH2Atxsbgt7wVYzGtduYK6zDDqfoiaNIWtPn+Qi2jkmWYN4JjVumz3yAwb/z5pcxrY2Hy/yrRT5n0UZU2WjaR3EF9O722tshTmDJ2KLqUiYE26Xj7oV+XMPBHkf8J419iyv6Kn4noURhHuIkmyEdgdJ+InzCJoUf9xHlEJpVWhI3extgs2c0aWgm3f5wpNxbHfEAOkWv3QtLDDlzvSyqrUG7AuBnrZR4pstyaRoG4umGATfrMH/KVYiu5p8LrIcYWOdiDtv+FbUpNvba43/KVRSYXJtDLD47S2dQLudlDHYxljqBm+4MFAe8HInzhQqYamQfBG2xhBRV2xuTqkGe/Y6AHBx+3ZWYgNawlp8drauEDQcGiNIgR5+cqytUBJsua2BPVg9V9RwI0yEOGPmCJjgnhM3AaRAXKIIktcWx05RukDiQbiXaY0RHAEfNU4ao4G408guH78opxf8A3G/3+SqrtdMuIJQQzJPeXHefl9FOkQ0yRY2uRZBvZpM9DwSF52K1cfMyurwLfkYuzAsmGnpePn3QlXNnEGQ4+QB+xQjsQdjwV44wi0IqFfQXL/0lhs5AI1N53tt80Wc2BnTB9ftG6X08WJ/VHpKjUxHSf51CLim+hVJpdjGpnBaAQ2Cd7gSO4KU4mqX3azffxD8rr38m6gas3Ej1TJJdIVyb02Ce5f8A2D/kPyvIr+qd1K8n5MnS8n//2Q==").into(backIV);
//                    }

                    JSONObject forecastObj = response.getJSONObject("forecast");
                    JSONObject forecast0 = forecastObj.getJSONArray("forecastday").getJSONObject(0);
                    JSONArray hourArray = forecast0.getJSONArray("hour");

                    for(int i = 0; i < hourArray.length(); i++) {
                        JSONObject hourObj = hourArray.getJSONObject(i);
                        String time = hourObj.getString("time");
                        String temper = hourObj.getString("temp_c");
                        String img = hourObj.getJSONObject("condition").getString("icon");
                        String wind = hourObj.getString("wind_kph");
                        weatherRVModalArrayList.add(new WeatherRVModal(time, temper, img, wind));
                    }
                    weatherRVAdapter.notifyDataSetChanged();

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(MainActivity.this, "Please enter valid city name", Toast.LENGTH_SHORT).show();
            }
        });

        requestQueue.add(jsonObjectRequest);
    }
}